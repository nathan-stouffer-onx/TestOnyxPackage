$input a_position, a_texcoord7
$output v_texcoord7, v_texcoord6, v_texcoord5, v_texcoord4

//includes
#include <common.sh>
#include "layers.sc"
#include "terrain.sc"

//samplers
SAMPLER2D(s_heightTexture, 0);
uniform vec4 s_heightTexture_Res;
SAMPLER2D(s_vectorColor, 2);
uniform vec4 s_vectorColor_Res;
SAMPLER2D(s_texture0, 1);
uniform vec4 s_texture0_Res;

//cubeSamplers

//definitions
uniform vec4 u_tileSize;
uniform vec4 u_tileDistortion;
uniform vec4 u_ScaleOffsetHeight;
uniform vec4 u_ScaleOffsetTex0;
uniform vec4 u_OpacityTex0;
uniform vec4 u_FogTransition;
uniform vec4 u_FogColor;
uniform vec4 u_BackgroundColor;
uniform vec4 u_NearFarFocus;
uniform vec4 u_eyePos;
uniform vec4 u_camRight;
uniform vec4 u_camForward;
uniform vec4 u_camUp;
uniform vec4 u_time;
uniform vec4 u_tileMin;
uniform vec4 u_tileMax;

//functions

void main()
{

vec3 position = a_position.xyz;
vec4 texcoords = a_texcoord7.xyzw;
//main start
	vec2 tileCoord = mix(u_tileMin.xy, u_tileMax.xy, position.xy);
	float baseHeight = u_tileMin.z + (position.z * u_tileMax.z);
	vec4 worldPosition = vec4(tileCoord, baseHeight, 0.0);
	vec3 vertexPosition = worldPosition.xyz;
mat4 viewMat = u_view;
	vec2 tilePos = clamp(a_position.xy, 0, 1);
	worldPosition.w = elevationAtVS(tilePos, u_ScaleOffsetHeight, s_heightTexture);
	float z = distort(worldPosition.w, tilePos, u_tileDistortion.xy);
	worldPosition.z += z;
	vertexPosition.z += z * u_tileSize.z;

//lighting
vec4 fogDist = vec4(length(worldPosition.xyz), 0.0, 0.0, 0.0);

//compose
	vec4 projected = mul(u_proj, mul(viewMat, vec4(vertexPosition.xyz, 1.0)));
	vec4 depth = projected;
	 projected.z -= (projected.w * 0.01);
//	 projected.z -= (1.0 / 128.0);
projected.z -= (projected.w * 0.01);
	gl_Position = projected;

v_texcoord7 = worldPosition.xyzw;
v_texcoord6 = depth.xyzw;
v_texcoord5 = texcoords.xyzw;
v_texcoord4 = fogDist.xyzw;

}

