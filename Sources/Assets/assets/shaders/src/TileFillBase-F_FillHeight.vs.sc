$input a_texcoord7
$output v_texcoord4, v_bitangent, v_texcoord7, v_texcoord6, v_depth, v_texcoord5

//includes
#include <common.sh>
#include "layers.sc"
#include "terrain.sc"

//samplers
SAMPLER2D(s_heightTexture, 0);
uniform vec4 s_heightTexture_Res;
SAMPLER2D(s_vectorColors, 2);
uniform vec4 s_vectorColors_Res;
SAMPLER2D(s_vectorPatterns, 3);
uniform vec4 s_vectorPatterns_Res;
SAMPLER2D(s_patterns, 1);
uniform vec4 s_patterns_Res;

//cubeSamplers

//definitions
uniform vec4 u_tileSize;
uniform vec4 u_ScaleOffsetHeight;
uniform vec4 u_tileDistortion;
uniform vec4 u_MeshResolution;
uniform vec4 u_TileFillData;
uniform vec4 u_NearFarFocus;
uniform vec4 u_eyePos;
uniform vec4 u_camRight;
uniform vec4 u_camForward;
uniform vec4 u_camUp;
uniform vec4 u_time;
uniform vec4 u_tileMin;
uniform vec4 u_TileFillOpacityTransition;
uniform vec4 u_PackedParams;
uniform vec4 u_tileMax;
uniform vec4 u_TileFragClip;

//functions

void main()
{

vec4 tilePosition_style = a_texcoord7.xyzw;
//main start
	vec2 tilePos = vec2(tilePosition_style.xy);
	float tileZ = u_tileMin.z;
	float zOffset = u_PackedParams.x;
	vec3 worldPosition = vec3(mix(u_tileMin.xy, u_tileMax.xy, tilePos), u_tileMin.z);
	 vec4 vecPattern = texture2DLod(s_vectorPatterns, tilePosition_style.zw, 0);
	 vecPattern.zw = vecPattern.zw * s_patterns_Res.xy;
	 vec4 vecColor = texture2DLod(s_vectorColors, tilePosition_style.zw, 0);
mat4 viewMat = u_view;
	// compute height at the actual mesh
	float z = meshHeightAtPlanes(tilePos, u_tileDistortion.xy, u_ScaleOffsetHeight, u_MeshResolution.x, s_heightTexture);
	tileZ += z * u_tileSize.z;
	zOffset = distort(zOffset, tilePos, u_tileDistortion.xy) * u_tileSize.z;

//lighting

//compose
	worldPosition.z = tileZ + zOffset;
	float distFade = 1.0 - smoothstep(u_TileFillOpacityTransition.x, u_TileFillOpacityTransition.y, length(worldPosition.xyz));
	float biasKm = max(0.010, max((2.0 / 256.0) * (u_tileMax.x - u_tileMin.x), 0.002 * u_NearFarFocus.z));
	float biasScalar = max(0.5, 1.0 - biasKm / length(worldPosition));
	vec3 biasedPosition = worldPosition * biasScalar;
	vec4 projected = mul(u_proj, mul(viewMat, vec4(biasedPosition.xyz, 1.0)));
	vec4 depth = projected;
	vec4 tilePosition = vec4(tilePos, 0.0, 0.0);
	gl_Position = projected;

v_texcoord4 = vecPattern.xyzw;
v_bitangent = worldPosition.xyz;
v_texcoord7 = tilePosition.xyzw;
v_texcoord6 = depth.xyzw;
v_depth = distFade;
v_texcoord5 = vecColor.xyzw;

}

