$input a_normal, a_position, a_texcoord7
$output v_normal, v_texcoord7, v_texcoord6, v_texcoord5, v_texcoord4, v_texcoord3, v_texcoord2

//includes
#include <common.sh>
#include "layers.sc"
#include "terrain.sc"

//samplers
SAMPLER2D(s_heightTexture, 2);
uniform vec4 s_heightTexture_Res;
SAMPLER2D(s_SlopeAngleShadeTexture, 0);
uniform vec4 s_SlopeAngleShadeTexture_Res;
SAMPLER2D(s_SlopeAspectShadeTexture, 1);
uniform vec4 s_SlopeAspectShadeTexture_Res;
SAMPLER2D(s_texture0, 3);
uniform vec4 s_texture0_Res;
SAMPLER2D(s_texture1, 4);
uniform vec4 s_texture1_Res;

//cubeSamplers

//definitions
uniform vec4 u_tileSize;
uniform vec4 u_tileDistortion;
uniform vec4 u_ScaleOffsetHeight;
uniform vec4 u_lightStrengthPow;
uniform vec4 u_ContourParams0;
uniform vec4 u_ContourColor0;
uniform vec4 u_ContourParams1;
uniform vec4 u_ContourColor1;
uniform vec4 u_ContourFade;
uniform vec4 u_MaxNormalZ;
uniform vec4 u_HillshadeLightDir;
uniform vec4 u_HillshadeAlbedo;
uniform vec4 u_HillshadeParams;
uniform vec4 u_FogTransition;
uniform vec4 u_FogColor;
uniform vec4 u_ScaleOffsetTex0;
uniform vec4 u_OpacityTex0;
uniform vec4 u_ScaleOffsetTex1;
uniform vec4 u_OpacityTex1;
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

vec4 normal = a_normal.xyzw;
vec3 position = a_position.xyz;
vec4 texcoords = a_texcoord7.xyzw;
//main start
	vec2 tileCoord = mix(u_tileMin.xy, u_tileMax.xy, position.xy);
	float baseHeight = u_tileMin.z + (position.z * u_tileMax.z);
	vec4 worldPosition = vec4(tileCoord, baseHeight, 0.0);
	vec3 vertexPosition = worldPosition.xyz;
normal = mul(u_model[0], vec4(normal.xyz, 0.0));
mat4 viewMat = u_view;
	worldPosition.w = elevationAtVS(texcoords.xy, u_ScaleOffsetHeight, s_heightTexture);
	float z = distort(worldPosition.w, texcoords.xy, u_tileDistortion.xy);
	worldPosition.z += z;
	vertexPosition.z += z * u_tileSize.z;
	vec4 tileDistortion = u_tileDistortion;
	vec4 scaleOffsetHeight = u_ScaleOffsetHeight;

//lighting
vec4 fogDist = vec4(length(worldPosition.xyz), 0.0, 0.0, 0.0);

//compose
	vec4 projected = mul(u_proj, mul(viewMat, vec4(vertexPosition.xyz, 1.0)));
	vec4 depth = projected;
	gl_Position = projected;

v_normal = normal.xyz;
v_texcoord7 = worldPosition.xyzw;
v_texcoord6 = depth.xyzw;
v_texcoord5 = texcoords.xyzw;
v_texcoord4 = fogDist.xyzw;
v_texcoord3 = tileDistortion.xyzw;
v_texcoord2 = scaleOffsetHeight.xyzw;

}

