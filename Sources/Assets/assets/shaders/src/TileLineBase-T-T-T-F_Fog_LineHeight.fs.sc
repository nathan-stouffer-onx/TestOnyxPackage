$input v_texcoord4, v_bitangent, v_texcoord7, v_texcoord6, v_color0, v_depth, v_texcoord5, v_texcoord3, v_texcoord2, v_tangent, v_normal, v_texcoord1, v_texcoord0, v_color4, v_color3, v_color2, v_color1
//includes
#include <common.sh>
#include "layers.sc"
#include "derivatives.sc"

//samplers
SAMPLER2D(s_heightTexture, 4);
uniform vec4 s_heightTexture_Res;
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
uniform vec4 u_FogTransition;
uniform vec4 u_FogColor;
uniform vec4 u_time;
uniform vec4 u_tileMin;
uniform vec4 u_tileMax;
uniform vec4 u_TileFragClip;
uniform vec4 u_p1p2;
uniform vec4 u_PrevNext;
uniform vec4 u_screenDimensions;
uniform vec4 u_params;
uniform vec4 u_vectorFade;
uniform vec4 u_TileLineOpacityTransition;
uniform vec4 u_NearFarFocus;

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
	float dist = df;
	vec2 ddist = vec2(dFdx(dist), dFdy(dist));
	float pixelDist = dist / length(ddist);
	return saturate(-pixelDist);
}
// This makes a line in UV units. A 1.0 thick line will span a whole 0..1 in UV space.
float DashLine(vec3 uv, float dashId, vec2 pA, vec2 pB, vec4 screenEndpoints, vec2 totalLen, vec2 thick)
{
	float dashLenCount = floor(texture2D(s_DashSampler, vec2(0.0, dashId)).x * (s_DashSampler_Res.x - 1.0) + 0.5);
	float worldLineLen = totalLen.y - totalLen.x;
	float screenLineLen = length(screenEndpoints.xy - screenEndpoints.zw);
	float worldToScreenLengthScale = screenLineLen / worldLineLen * 0.5;
	float lengthOffset = totalLen.x * worldToScreenLengthScale +  uv.x * thick.y; //find our position along the dashing
	lengthOffset /= thick.y; //put in scale of line widths per dash unit
	float dashScale = 0.25; //magic number to fix scaling - may not be needed, converted to zoom controlled, or...?
	float dashPosition = mod(dashScale * lengthOffset , dashLenCount); //tile by the number of dash units we have per pattern 
	float dashUVx = (dashPosition + 1.0) / s_DashSampler_Res.x;
	float dashOnOff = texture2D(s_DashSampler, vec2(dashUVx, dashId)).x;
	float dashDf = LineDistField(uv, pA, pB, thick, 0.0, 0.0, 0.0) * dashOnOff;
	float dist = -dashDf;
	vec2 ddist = vec2(dFdx(dist), dFdy(dist));
	float pixelDist = dist / length(ddist);
	return saturate(pixelDist);
}
// This makes an outlined line in UV units. A 1.0 thick outline will span 0..1 in UV space.
float DrawOutline(vec3 uv, vec2 pA, vec2 pB, vec2 thick, float rounded, float outlineThick)
{
	float df = LineDistField(uv, pA, pB, vec2(thick), rounded, 0.0, 0.0);
	float outlineDf = ((abs(df + outlineThick*0.5) - outlineThick * 0.5));
	float dist = outlineDf;
	vec2 ddist = vec2(dFdx(dist), dFdy(dist));
	float pixelDist = dist / length(ddist);
	return saturate(0.5 - pixelDist) * abs(sign(outlineThick));
//	return smoothStep(saturate(-((abs(df + outlineThick*0.5) - outlineThick * 0.5))), 0.0, 0.001);
}
vec3 fog(vec3 underneath, vec4 color, vec2 transition, float d)
{
	float strength = smoothstep(transition.x, transition.y, d);
	return mix(underneath, color.rgb, strength * color.a);
}

void main()
{

vec4 lineCenter = v_texcoord4.xyzw;
vec3 worldPosition = v_bitangent.xyz;
vec4 tilePosition = v_texcoord7.xyzw;
vec4 linePosition = v_texcoord6.xyzw;
vec4 color = v_color0.xyzw;
float dashRow = v_depth;
vec4 screenPosition = v_texcoord5.xyzw;
vec4 texcoords = v_texcoord3.xyzw;
vec4 jointNormals = v_texcoord2.xyzw;
vec3 line_dir = v_tangent.xyz;
vec3 line_side = v_normal.xyz;
vec4 line_size = v_texcoord1.xyzw;
vec4 line_endPointsScreen = v_texcoord0.xyzw;
vec4 line_lengthTotal = v_color4.xyzw;
vec4 line_endFlags = v_color3.xyzw;
vec4 distFade = v_color2.xyzw;
vec4 fogDist = v_color1.xyzw;
//main start
float inX = inRange(tilePosition.x, u_TileFragClip.x, u_TileFragClip.z);
float inY = inRange(tilePosition.y, u_TileFragClip.y, u_TileFragClip.w);
if (inX * inY == 0.0) { discard; }
vec2 d = abs(linePosition.xy);
float pos = dot(d, d);
float cMaxDashArrayLength = 31.0; // NOTE: there is an identical constant in Styling::DashArray
vec2 dashPixelWidth = 1.0 / s_DashSampler_Res.xy;
float dashV = dashRow + 0.5 * dashPixelWidth.y;
// clip off the ends of the corners to make the mitered joints
vec2 fromEndA = screenPosition.xy - line_endPointsScreen.xy;
vec2 fromEndB = screenPosition.xy - line_endPointsScreen.zw;
bool clipA = dot(fromEndA, jointNormals.xy) < 0.0; // see which side of the end we're on
bool clipB = dot(fromEndB, jointNormals.zw) < 0.0;
if ((clipA && line_endFlags.x > -9999.0) || (clipB && line_endFlags.y > -9999.0)) { discard; }

//lighting
float ends = texcoords.z;
vec3 lineCoords = vec3(texcoords.yx, texcoords.z);
vec2 start = vec2(0,0);
vec2 end = vec2(ends, 0);
float widthExpansion = max(line_size.x, line_size.y) + line_size.z + line_size.w + 2.0;
widthExpansion /= 2.0; // account for x uv being -1 to both side so need to cut width in half to get proper pixel count
float solid = FillLine(lineCoords, start, end, vec2((line_size.x * 0.5) / widthExpansion, (line_size.x * 0.5) / widthExpansion), 0.0);
float solidOuterOutline = DrawOutline(lineCoords, start, end, vec2( ((max(line_size.x, line_size.y) + line_size.z + line_size.w) * 0.5) / widthExpansion, ((max(line_size.x, line_size.y) + line_size.z + line_size.w) * 0.5) / widthExpansion), 0.0, line_size.w * 0.5 / widthExpansion);
float dashLine = DashLine(lineCoords, dashV, start, end, line_endPointsScreen, line_lengthTotal.xy, vec2((line_size.y * 0.5) / widthExpansion,(line_size.y * 0.5) / widthExpansion));
float alpha = min(1.0, max(dashLine + solid + solidOuterOutline,0));
vec4 fragColor = vec4(solid*1.0, -solid*1.0, 0.0,1.0);
//temp variables for color, need to hook up to cpu access
vec4 u_lineOuterOutlineColor = vec4(0,0,1,1);
vec4 dashColor = color.xyzw; // TODO - pass in dash color separate from solid vec4(1,0,0,1);
vec3 solidDashBlend = mix(color.xyz, dashLine * dashColor.xyz, dashLine);
fragColor = vec4(mix(solidDashBlend, u_lineOuterOutlineColor.xyz, solidOuterOutline), alpha);
//float df = LineDistField(lineCoords, start, end, vec2(1.0,1.0), 0.0, 0.0, 0.0);
//fragColor.xyzw = vec4(df, -df,0,1.0);
//fragColor.xyzw = vec4(1.0, 0.0,1.0,1.0);
fragColor.rgb = fog(fragColor.rgb, u_FogColor, u_FogTransition.xy, fogDist.x);

//compose
gl_FragData[0] = vec4(fragColor.rgb, fragColor.a * color.a * u_vectorFade.r * distFade.x);
gl_FragData[1] = vec4(0, 0, 0, 0);


}

