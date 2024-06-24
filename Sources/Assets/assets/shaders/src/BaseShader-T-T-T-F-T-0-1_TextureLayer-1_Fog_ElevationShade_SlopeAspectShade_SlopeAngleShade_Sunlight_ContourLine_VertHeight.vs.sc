$input a_normal, a_position, a_texcoord7
$output v_normal, v_texcoord7, v_texcoord6, v_texcoord5, v_texcoord4, v_texcoord3, v_texcoord2, v_texcoord1, v_texcoord0, v_color4

//includes
#include <common.sh>
#include "layers.sc"
#include "terrain.sc"

//samplers
SAMPLER2D(s_heightTexture, 3);
uniform vec4 s_heightTexture_Res;
SAMPLER2D(s_sunShadowDepth, 4);
uniform vec4 s_sunShadowDepth_Res;
SAMPLER2D(s_SlopeAngleShadeTexture, 1);
uniform vec4 s_SlopeAngleShadeTexture_Res;
SAMPLER2D(s_SlopeAspectShadeTexture, 2);
uniform vec4 s_SlopeAspectShadeTexture_Res;
SAMPLER2D(s_ElevationShadeTexture, 0);
uniform vec4 s_ElevationShadeTexture_Res;
SAMPLER2D(s_texture0, 5);
uniform vec4 s_texture0_Res;

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
uniform vec4 u_SunTimeData;
uniform vec4 u_SunMinStrength;
uniform vec4 u_SunAmbient;
uniform vec4 u_sunTileMin;
uniform vec4 u_sunTileMax;
uniform vec4 u_sunShadowTileMin;
uniform vec4 u_sunShadowTileMax;
uniform vec4 u_sunShadowFarPlane;
uniform vec4 u_sunShadowBias;
uniform vec4 u_sunShadowStrength;
uniform mat4 u_sunShadowView;
uniform mat4 u_sunShadowProj;
uniform vec4 u_sunShadowVSMParams;
uniform vec4 u_CascadeDebug;
uniform vec4 u_MaxNormalZ;
uniform vec4 u_ElevationExtents;
uniform vec4 u_FogTransition;
uniform vec4 u_FogColor;
uniform vec4 u_ScaleOffsetTex0;
uniform vec4 u_OpacityTex0;
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
	vec4 sunUV = vec4(u_sunTileMin.xy + (u_sunTileMax.xy - u_sunTileMin.xy) * texcoords.xy, 0,0);
	vec4 sunDir = vec4(calcSunDir(u_SunTimeData.x, u_SunTimeData.y), 0);
mat4 viewMat = u_view;
	worldPosition.w = elevationAtVS(texcoords.xy, u_ScaleOffsetHeight, s_heightTexture);
	float z = distort(worldPosition.w, texcoords.xy, u_tileDistortion.xy);
	worldPosition.z += z;
	vertexPosition.z += z * u_tileSize.z;
	vec4 tileDistortion = u_tileDistortion;
	vec4 scaleOffsetHeight = u_ScaleOffsetHeight;

//lighting
vec4 fogDist = vec4(length(worldPosition.xyz), 0.0, 0.0, 0.0);
	vec2 shadowTileCoord = mix(u_sunShadowTileMin.xy, u_sunShadowTileMax.xy, position.xy);
	float shadowBaseHeight = u_sunShadowTileMin.z + (position.z * u_sunShadowTileMax.z);
	vec4 shadowWorldPosition = vec4(shadowTileCoord, shadowBaseHeight, shadowBaseHeight);
	shadowWorldPosition.z += z * u_tileSize.z;
	vec4 sunShadowUV = mul(u_sunShadowProj, mul(u_sunShadowView, vec4(shadowWorldPosition.xyz, 1.0)));
	sunShadowUV.z = length(shadowWorldPosition.xyz) / u_sunShadowFarPlane.x;

//compose
	vec4 projected = mul(u_proj, mul(viewMat, vec4(vertexPosition.xyz, 1.0)));
	vec4 depth = projected;
	gl_Position = projected;

v_normal = normal.xyz;
v_texcoord7 = worldPosition.xyzw;
v_texcoord6 = depth.xyzw;
v_texcoord5 = texcoords.xyzw;
v_texcoord4 = fogDist.xyzw;
v_texcoord3 = sunDir.xyzw;
v_texcoord2 = sunUV.xyzw;
v_texcoord1 = sunShadowUV.xyzw;
v_texcoord0 = tileDistortion.xyzw;
v_color4 = scaleOffsetHeight.xyzw;

}

