$input v_texcoord0, v_color4, v_texcoord7, v_tangent, v_color3, v_texcoord1, v_texcoord2, v_bitangent, v_texcoord3, v_depth, v_texcoord4, v_texcoord5, v_color0, v_texcoord6
//includes
#include <common.sh>
#include "map3dFunctions.sc"
#include "map3dFragFunctions.sc"

//samplers
SAMPLER2D(s_heightTextureVert, 4);
uniform vec4 s_heightTextureVert_Res;
SAMPLER2D(s_VectorColors, 2);
uniform vec4 s_VectorColors_Res;
SAMPLER2D(s_VectorWidths, 3);
uniform vec4 s_VectorWidths_Res;
SAMPLER2D(s_DashCoords, 0);
uniform vec4 s_DashCoords_Res;
SAMPLER2D(s_DashSampler, 1);
uniform vec4 s_DashSampler_Res;

//cubeSamplers

//definitions
uniform vec4 u_tileSize;
uniform vec4 u_ScaleOffsetHeight;
uniform vec4 u_tileDistortion;
uniform vec4 u_MeshResolution;
uniform vec4 u_time;
uniform vec4 u_tileMin;
uniform vec4 u_tileMax;
uniform vec4 u_TileFragClip;
uniform vec4 u_endAngles;
uniform vec4 u_p1p2;
uniform vec4 u_screenDimensions;
uniform vec4 u_params;
uniform vec4 u_drawColor;
uniform vec4 u_vectorFade;
uniform vec4 u_nearFarPlane;

//functions
	float repeat(float x) { return abs(fract(x*0.5+0.5)-0.5)*2.0; }
float LineDistField(vec3 uv, vec2 pA, vec2 pB, vec2 thick, float rounded, float dashOn, float time)
{
	// Don't let it get more round than circular.
	rounded = min(thick.y, rounded);
	// midpoint
	vec2 mid = (pB + pA) * 0.5;
	// vector from point A to B
	vec2 delta = pB - pA;
	// Distance between endpoints
	float lenD = length(delta);
	// unit vector pointing in the line's direction
	vec2 unit = delta / lenD;
	// Check for when line endpoints are the same
	if (lenD < 0.0001) unit = vec2(1.0, 0.0);
	// Perpendicular vector to unit - also length 1.0
	vec2 perp = unit.yx * vec2(-1.0, 1.0);
	// position along line from midpoint
	float dpx = dot(unit, uv.xy - mid);
	// distance away from line at a right angle
	float dpy = dot(perp, uv.xy - mid);
	// Make a distance function that is 0 at the transition from black to white
	float disty = abs(dpy) - thick.y + rounded;
	float distx = abs(dpx) - lenD * 0.5 - thick.x + rounded;
	// Too tired to remember what this does. Something like rounded endpoints for distance function.
	float dist = length(vec2(max(0.0, distx), max(0.0,disty))) - rounded;
	dist = min(dist, max(distx, disty));
	// This is for animated dashed lines. Delete if you don't like dashes.
	float dashScale = 2.0*thick.y;
	// Make a distance function for the dashes
	float dash = (repeat(dpx/dashScale + time)-0.5)*dashScale;
	// Combine this distance function with the line's.
	dist = max(dist, dash-(1.0-dashOn*1.0)*10000.0);
	//distance from endpoint
//need to account for width and endcap size here to scale it so its not stretched
	float distPa = length(pA - uv.xy) - thick.y;
	float distPb = length(pB - uv.xy)- thick.y;
	float endA = max(0.0, -sign(uv.x));
	float endB = max(0.0,sign(uv.x - uv.z));
	float endMask = endA + endB;
	return mix(dist, min(distPa, distPb), endMask);
	//return dist;
}
float smoothStep(float val, float low, float high)
{
	float f = (val - low) / (high - low);
	return min(max(f, 0.0), 1.0);
}
// This makes a line in UV units. A 1.0 thick line will span a whole 0..1 in UV space.
float FillLine(vec3 uv, vec2 pA, vec2 pB, vec2 thick, float rounded)
{
	float df = LineDistField(uv, pA, pB, thick, rounded, 0.0, 0.0);
	return smoothStep(saturate(-df), 0.0, 0.05);
}
// This makes a line in UV units. A 1.0 thick line will span a whole 0..1 in UV space.
float DashLine(vec3 uv, float dashId, vec2 pA, vec2 pB, vec4 screenEndpoints, vec2 totalLen, vec2 thick)
{
	float dashLenCount = floor(texture2D(s_DashSampler, vec2(0.0, dashId)).x * (s_DashSampler_Res.x - 1.0) + 0.5);
	float worldLineLen = totalLen.y - totalLen.x;
	float screenLineLen = length(screenEndpoints.xy - screenEndpoints.zw);
	float worldToScreenLengthScale = screenLineLen / worldLineLen;
	float lengthOffset = totalLen.x * worldToScreenLengthScale +  uv.x * thick.y; //find our position along the dashing
	lengthOffset /= thick.y; //put in scale of line widths per dash unit
	float dashScale = 1.0; //magic number to fix scaling - may not be needed, converted to zoom controlled, or...?
	float dashPosition = mod(dashScale * lengthOffset + u_time.x / 500.0, dashLenCount); //tile by the number of dash units we have per pattern 
	float dashUVx = (dashPosition + 1.0) / s_DashSampler_Res.x;
	float dashOnOff = texture2D(s_DashSampler, vec2(dashUVx, dashId)).x;
	float dashDf = LineDistField(uv, pA, pB, thick, 0.0, 0.0, 0.0) * dashOnOff;
	float dashLine = smoothStep((-dashDf), 0.0, 0.01);
	 return dashLine;
}
// This makes an outlined line in UV units. A 1.0 thick outline will span 0..1 in UV space.
float DrawOutline(vec3 uv, vec2 pA, vec2 pB, vec2 thick, float rounded, float outlineThick)
{
	float df = LineDistField(uv, pA, pB, vec2(thick), rounded, 0.0, 0.0);
	return smoothStep(saturate(-((abs(df + outlineThick*0.5) - outlineThick * 0.5))), 0.0, 0.001);
}

void main()
{

vec4 line_endPointsScreen = v_texcoord0.xyzw;
vec4 line_lengthTotal = v_color4.xyzw;
vec4 tilePosition = v_texcoord7.xyzw;
vec3 line_side = v_tangent.xyz;
vec4 line_endFlags = v_color3.xyzw;
vec4 line_size = v_texcoord1.xyzw;
vec4 line_endAngles = v_texcoord2.xyzw;
vec3 line_dir = v_bitangent.xyz;
vec4 texcoords = v_texcoord3.xyzw;
float dashRow = v_depth;
vec4 lineCenter = v_texcoord4.xyzw;
vec4 screenPosition = v_texcoord5.xyzw;
vec4 color = v_color0.xyzw;
vec4 linePosition = v_texcoord6.xyzw;
//main start
	float inX = inRange(tilePosition.x, u_TileFragClip.x, u_TileFragClip.z);
	float inY = inRange(tilePosition.y, u_TileFragClip.y, u_TileFragClip.w);
	if (inX * inY == 0.0)
	{
		discard;
	}
	 vec2 d = abs(linePosition.xy);
	 float pos = dot(d, d);
	 float cMaxDashArrayLength = 16.0;
	 vec2 dashPixelWidth = 1.0 / s_DashSampler_Res.xy;
	 float dashV = dashRow + 0.5 * dashPixelWidth.y;
//clip off the ends of the corners to make the mitered joints
vec2 toEndA = normalize(screenPosition.xy - line_endPointsScreen.xy);//normalize(texcoords.xy - vec2(0,0)) * u_viewTexel.xy;
vec2 toEndB = normalize(screenPosition.xy - line_endPointsScreen.zw);//normalize(texcoords.xy - vec2(0,texcoords.z)) * u_viewTexel.xy;
//spin the miters 90 deg so they're 90 deg to the miter angle
vec2 angleAUV = line_endAngles.xy;
vec2 angleBUV = line_endAngles.zw;
vec2 endADir = vec2(-angleAUV.y, angleAUV.x);
vec2 endBDir = vec2(-angleBUV.y, angleBUV.x);
//calculate if the directions are facing towards the center of the line like we expect, and flip if not
float flipA = sign(dot(endADir, line_dir.xy));//vec2(0,1)));
float flipB = sign(dot(endBDir, -line_dir.xy));//vec2(0,-1)));
float clipA = -flipA * dot(toEndA, endADir); //see which side of the end we're on
float clipB = flipB * dot(toEndB, endBDir);
if( ((clipA * abs(line_endAngles.x)) > 0.0 && line_endFlags.x > -9999.0) || ((clipB * abs(line_endAngles.z)) < 0.0 && line_endFlags.y > -9999.0) )
	discard;

//lighting

//compose
float ends = texcoords.z;
vec3 lineCoords = vec3(texcoords.yx, texcoords.z);
vec2 start = vec2(0,0);
vec2 end = vec2(ends, 0);
float widthExpansion = max(line_size.x, line_size.y) + line_size.z + line_size.w + 2.0;
widthExpansion /= 2.0; //account for x uv being -1 to both side so need to cut width in half to get proper pixel count
float solid = FillLine(lineCoords, start, end, vec2((line_size.x * 0.5) / widthExpansion, (line_size.x * 0.5) / widthExpansion), 0.0);
float solidOuterOutline = DrawOutline(lineCoords, start, end, vec2( ((max(line_size.x, line_size.y) + line_size.z + line_size.w) * 0.5) / widthExpansion, ((max(line_size.x, line_size.y) + line_size.z + line_size.w) * 0.5) / widthExpansion), 0.0, line_size.w * 0.5 / widthExpansion);
float dashLine = DashLine(lineCoords, dashV, start, end, line_endPointsScreen, line_lengthTotal.xy, vec2((line_size.y * 0.5) / widthExpansion,(line_size.y * 0.5) / widthExpansion));
float alpha = min(1.0, max(dashLine + solid + solidOuterOutline,0));
vec4 fragColor = vec4(solid*1.0, -solid*1.0, 0.0,1.0);
//temp variables for color, need to hook up to cpu access
vec4 u_lineOuterOutlineColor = vec4(0,0,1,1);
vec4 dashColor = vec4(1,0,0,1);
vec3 solidDashBlend = mix(color.xyz, dashLine * dashColor.xyz, dashLine);
fragColor = vec4(mix(solidDashBlend, u_lineOuterOutlineColor.xyz, solidOuterOutline), alpha);
//float df = LineDistField(lineCoords, start, end, vec2(1.0,1.0), 0.0, 0.0, 0.0);
//fragColor.xyzw = vec4(df, -df,0,1.0);
	gl_FragData[0] = vec4(fragColor.rgb, fragColor.a * color.a * u_vectorFade.r);
	gl_FragData[1] = vec4(0, 0, 0, 0);


}

