$input a_normal, a_position, a_texcoord7
$output v_normal, v_texcoord7, v_texcoord6, v_texcoord5, v_texcoord4, v_texcoord3, v_texcoord2, v_texcoord1, v_texcoord0, v_color4

//includes
#include <common.sh>
#include "OnyxFunctions.sc"

//samplers
SAMPLER2D(s_heightTexture, 2);
uniform vec4 s_heightTexture_Res;
SAMPLER2D(s_sunShadowDepth, 3);
uniform vec4 s_sunShadowDepth_Res;
SAMPLER2D(s_SlopeAngleTexture, 1);
uniform vec4 s_SlopeAngleTexture_Res;
SAMPLER2D(s_HeightBandTexture, 0);
uniform vec4 s_HeightBandTexture_Res;
SAMPLER2D(s_texture0, 4);
uniform vec4 s_texture0_Res;

//cubeSamplers

//definitions
uniform vec4 u_tileSize;
uniform vec4 u_tileDistortion;
uniform vec4 u_heightTileSize;
uniform vec4 u_ScaleOffsetHeight;
uniform vec4 u_lightStrengthPow;
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
uniform vec4 u_HeightExtents;
uniform vec4 u_fogVars;
uniform vec4 u_fogColor;
uniform vec4 u_ScaleOffsetTex0;
uniform vec4 u_nearFarPlane;
uniform vec4 u_eyePos;
uniform vec4 u_camRight;
uniform vec4 u_camForward;
uniform vec4 u_camUp;
uniform vec4 u_time;
uniform vec4 u_tileMin;
uniform vec4 u_tileMax;

//functions
// expects uv to be in tile coordinates
float heightAt(vec2 uv, vec4 scaleOffset)
{
	vec2 scaledUV = scaleOffset.zw * uv + scaleOffset.xy;
	return texture2DLod(s_heightTexture, scaledUV, 0).r;
}
// expects uv to be in tile coordinates
float distortedHeightAt(vec2 uv, vec2 distortion, vec4 scaleOffset)
{
	float z = heightAt(uv, scaleOffset);
	float distort = mix(distortion.x, distortion.y, uv.y);
	return z * distort;
}

void main()
{

vec4 normal = a_normal.xyzw;
vec3 position = a_position.xyz;
vec4 texcoords = a_texcoord7.xyzw;
//main start
	vec2 tileCoord = mix(u_tileMin.xy, u_tileMax.xy, position.xy);
	float baseHeight = u_tileMin.z + (position.z * u_tileMax.z);
	vec4 worldPosition = vec4(tileCoord, baseHeight, baseHeight);
	vec3 vertexPosition = worldPosition.xyz;
normal = mul(u_model[0], vec4(normal.xyz, 0.0));
	vec4 sunUV = vec4(u_sunTileMin.xy + (u_sunTileMax.xy - u_sunTileMin.xy) * texcoords.xy, 0,0);
	vec4 sunDir = vec4(calcSunDir(u_SunTimeData.x, u_SunTimeData.y), 0);
mat4 viewMat = u_view;
	float z = distortedHeightAt(texcoords.xy, u_tileDistortion.xy, u_ScaleOffsetHeight);
	worldPosition.zw += vec2(z, heightAt(texcoords.xy, u_ScaleOffsetHeight));
	vertexPosition.z += z * u_tileSize.z;
	vec4 tileDistortion = u_tileDistortion;
	vec4 scaleOffsetHeight = u_ScaleOffsetHeight;

//lighting
vec4 fogDist = vec4(length(worldPosition.xyz) / u_nearFarPlane.y, 0.0,0.0,0.0);

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

