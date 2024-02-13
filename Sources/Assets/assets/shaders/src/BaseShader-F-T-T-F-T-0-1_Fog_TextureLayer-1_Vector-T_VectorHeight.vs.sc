$input a_position, a_texcoord7
$output v_texcoord7, v_texcoord6, v_texcoord5, v_texcoord4

//includes
#include <common.sh>
#include "OnyxFunctions.sc"

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
uniform vec4 u_heightTileSize;
uniform vec4 u_ScaleOffsetTex0;
uniform vec4 u_OpacityTex0;
uniform vec4 u_fogVars;
uniform vec4 u_fogColor;
uniform vec4 u_BackgroundColor;
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
float heightAt(vec2 uv)
{
	vec2 scaledUV = u_ScaleOffsetHeight.zw * uv + u_ScaleOffsetHeight.xy;
	return texture2DLod(s_heightTexture, scaledUV, 0).r;
}
// expects uv to be in tile coordinates
float distortedHeightAt(vec2 uv)
{
	float z = heightAt(uv);
	float distortion = mix(u_tileDistortion.x, u_tileDistortion.y, uv.y);
	return z * distortion;
}
// expects uv to be in tile coordinates
vec3 normalAt(vec2 uv)
{
	vec2 pixelWidth = 1.0 / s_heightTexture_Res.xy;
	vec2 tileDelta = pixelWidth / u_ScaleOffsetHeight.z;
	vec2 westUV = uv - vec2(tileDelta.x, 0);
	vec2 eastUV = uv + vec2(tileDelta.x, 0);
	vec2 northUV = uv - vec2(0, tileDelta.y);
	vec2 southUV = uv + vec2(0, tileDelta.y);
	float z = distortedHeightAt(uv);
	float westZ = distortedHeightAt(westUV) - z;
	float eastZ = distortedHeightAt(eastUV) - z;
	float northZ = distortedHeightAt(northUV) - z;
	float southZ = distortedHeightAt(southUV) - z;
	vec2 worldStep = u_heightTileSize.xy / 256.0;
	vec3 westDelta = vec3(-worldStep.x, 0, westZ);
	vec3 eastDelta = vec3(worldStep.x, 0, eastZ);
	vec3 northDelta = vec3(0, -worldStep.y, northZ);
	vec3 southDelta = vec3(0, worldStep.y, southZ);
	vec3 normal = cross(westDelta, northDelta) + cross(northDelta, eastDelta) + cross(eastDelta, southDelta) + cross(southDelta, westDelta);
	return normalize(normal);
}

void main()
{

vec3 position = a_position.xyz;
vec4 texcoords = a_texcoord7.xyzw;
//main start
	vec2 tileCoord = mix(u_tileMin.xy, u_tileMax.xy, position.xy);
	float baseHeight = u_tileMin.z + (position.z * u_tileMax.z);
	vec4 worldPosition = vec4(tileCoord, baseHeight, baseHeight);
	vec3 vertexPosition = worldPosition.xyz;
mat4 viewMat = u_view;
	vec2 tilePos = clamp(a_position.xy, 0, 1);
	float z = distortedHeightAt(tilePos);
	worldPosition.zw += vec2(z, heightAt(tilePos));
	vertexPosition.z += z * u_tileSize.z;

//lighting
vec4 fogDist = vec4(length(worldPosition.xyz) / u_nearFarPlane.y, 0.0,0.0,0.0);


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

