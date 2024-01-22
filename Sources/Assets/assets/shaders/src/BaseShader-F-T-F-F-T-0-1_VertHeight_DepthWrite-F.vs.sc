$input a_position, a_texcoord7
$output v_texcoord7, v_texcoord6, v_texcoord5, v_texcoord4

//includes
#include <common.sh>
#include "OnyxFunctions.sc"

//samplers
SAMPLER2D(s_heightTexture, 0);
uniform vec4 s_heightTexture_Res;

//cubeSamplers

//definitions
uniform vec4 u_tileSize;
uniform vec4 u_tileDistortion;
uniform vec4 u_heightTileSize;
uniform vec4 u_ScaleOffsetHeight;
uniform vec4 u_lightStrengthPow;
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

vec3 position = a_position.xyz;
vec4 texcoords = a_texcoord7.xyzw;
//main start
	vec2 tileCoord = mix(u_tileMin.xy, u_tileMax.xy, position.xy);
	float baseHeight = u_tileMin.z + (position.z * u_tileMax.z);
	vec4 worldPosition = vec4(tileCoord, baseHeight, baseHeight);
	vec3 vertexPosition = worldPosition.xyz;
mat4 viewMat = u_view;
	float z = distortedHeightAt(texcoords.xy, u_tileDistortion.xy, u_ScaleOffsetHeight);
	worldPosition.zw += vec2(z, heightAt(texcoords.xy, u_ScaleOffsetHeight));
	vertexPosition.z += z * u_tileSize.z;
	vec4 tileDistortion = u_tileDistortion;
	vec4 scaleOffsetHeight = u_ScaleOffsetHeight;

//lighting

//compose
	vec4 projected = mul(u_proj, mul(viewMat, vec4(vertexPosition.xyz, 1.0)));
	vec4 depth = projected;
	depth.xyz = vec3(length(worldPosition.xyz) / u_nearFarPlane.y, 0, 0);
	gl_Position = projected;

v_texcoord7 = depth.xyzw;
v_texcoord6 = texcoords.xyzw;
v_texcoord5 = tileDistortion.xyzw;
v_texcoord4 = scaleOffsetHeight.xyzw;

}

