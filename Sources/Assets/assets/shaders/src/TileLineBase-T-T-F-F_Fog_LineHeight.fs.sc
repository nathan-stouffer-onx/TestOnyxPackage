$input v_texcoord7, v_texcoord6, v_depth, v_texcoord5, v_texcoord4, v_texcoord3, v_texcoord2, v_texcoord1, v_bitangent, v_tangent, v_texcoord0, v_color4, v_color3, v_color2, v_color1, v_color0
//includes
#include <common.sh>
#include "layers.sc"
#include "derivatives.sc"

//samplers
SAMPLER2D(s_heightTexture, 5);
uniform vec4 s_heightTexture_Res;
SAMPLER2D(s_LineColors, 3);
uniform vec4 s_LineColors_Res;
SAMPLER2D(s_CasingColors, 0);
uniform vec4 s_CasingColors_Res;
SAMPLER2D(s_Widths, 4);
uniform vec4 s_Widths_Res;
SAMPLER2D(s_DashCoords, 1);
uniform vec4 s_DashCoords_Res;
SAMPLER2D(s_DashSampler, 2);
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
uniform vec4 u_params;
uniform vec4 u_vectorFade;
uniform vec4 u_TileLineOpacityTransition;
uniform vec4 u_NearFarFocus;

//functions
float LineStrength(vec2 p, vec2 a, vec2 b, float w)
{
	float d_max = 0.5 * (w + 1.0); // furthest distance from the line that should have any strength
	float window = 2.0; // number of pixels used to transition from full strength to no strength
	Segment seg = segment(a, b);
	float d = distanceTo(seg, p); // compute distance from pixel position to line segment
	// compute strength -- use different functions based on line width
	float s = 1.0;
	if (w < 1.0) { s = w - max(0.0, d + 0.5 * (w - 1.0)); }
	else { s = lerpInv(d_max, max(0.0, d_max - window), d); }
	return clamp(s, 0.0, 1.0);
}
float DashStrength(float v, vec2 length, float t, float width)
{
	vec2 pixelWidth = 1.0 / s_DashSampler_Res.xy;
	v += 0.5 * pixelWidth.y;
	float period = floor(texture2D(s_DashSampler, vec2(0.0, v)).x * (s_DashSampler_Res.x - 1.0) + 0.5);
	float l = mix(length.x, length.y, t);
	float rasterized = l * 256.0 / max(1.0, floor(width));
	float position = mod(rasterized, period);
	float u = (position + 1.0) / s_DashSampler_Res.x;
	float strength = texture2D(s_DashSampler, vec2(u, v)).x;
	return strength;
}
vec3 fog(vec3 underneath, vec4 color, vec2 transition, float d)
{
	float strength = smoothstep(transition.x, transition.y, d);
	return mix(underneath, color.rgb, strength * color.a);
}

void main()
{

vec4 tilePosition = v_texcoord7.xyzw;
vec4 linePosition = v_texcoord6.xyzw;
float dashRow = v_depth;
vec4 lineColor = v_texcoord5.xyzw;
vec4 casingColor = v_texcoord4.xyzw;
vec4 screenPosition = v_texcoord3.xyzw;
vec4 texcoords = v_texcoord2.xyzw;
vec4 jointNormals = v_texcoord1.xyzw;
vec3 line_dir = v_bitangent.xyz;
vec3 line_side = v_tangent.xyz;
vec4 line_width = v_texcoord0.xyzw;
vec4 line_endPointsScreen = v_color4.xyzw;
vec4 phaseParams = v_color3.xyzw;
vec4 line_endFlags = v_color2.xyzw;
vec4 distFade = v_color1.xyzw;
vec4 fogDist = v_color0.xyzw;
//main start
// discard fragments that are outside the tile
float inX = inRange(tilePosition.x, u_TileFragClip.x, u_TileFragClip.z);
float inY = inRange(tilePosition.y, u_TileFragClip.y, u_TileFragClip.w);
if (inX * inY == 0.0) { discard; }
// clip off the ends of the corners to make the mitered joints
vec2 fromEndA = screenPosition.xy - line_endPointsScreen.xy;
vec2 fromEndB = screenPosition.xy - line_endPointsScreen.zw;
bool clipA = dot(fromEndA, jointNormals.xy) < 0.0;
bool clipB = dot(fromEndB, jointNormals.zw) < 0.0;
bool isEndA = line_endFlags.x > -999999.0;
bool isEndB = line_endFlags.y > -999999.0;
if ((clipA && isEndA) || (clipB && isEndB)) { discard; }

//lighting
// compute line color
float dashStrength = DashStrength(dashRow, phaseParams.xy, linePosition.y, phaseParams.z);
float strength = LineStrength(screenPosition.xy, line_endPointsScreen.xy, line_endPointsScreen.zw, line_width.x);
lineColor.a *= strength * dashStrength;
// compute line casing
float casingStrength = LineStrength(screenPosition.xy, line_endPointsScreen.xy, line_endPointsScreen.zw, line_width.y);
casingColor.a *= casingStrength;
// compute fragment color
vec4 fragColor = (casingColor.a == 0.0) ? lineColor : vec4(mix(casingColor.rgb, lineColor.rgb, lineColor.a), min(1.0, casingColor.a + lineColor.a));
fragColor.rgb = fog(fragColor.rgb, u_FogColor, u_FogTransition.xy, fogDist.x);

//compose
gl_FragData[0] = vec4(fragColor.rgb, fragColor.a * u_vectorFade.r * distFade.x);
gl_FragData[1] = vec4(0, 0, 0, 0);


}

