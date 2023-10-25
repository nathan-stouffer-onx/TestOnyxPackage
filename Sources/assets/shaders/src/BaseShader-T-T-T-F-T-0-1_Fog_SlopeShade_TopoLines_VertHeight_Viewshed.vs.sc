$input a_texcoord7, a_normal, a_position
$output v_texcoord2, v_texcoord3, v_texcoord5, v_texcoord4, v_normal, v_texcoord6, v_texcoord7

//includes
#include <common.sh>
#include "map3dFunctions.sc"

//samplers
SAMPLER2D(s_heightTextureVert, 3);
uniform vec4 s_heightTextureVert_Res;
SAMPLER2D(s_heightTextureFrag, 2);
uniform vec4 s_heightTextureFrag_Res;
SAMPLER2D(s_SlopeAngleTexture, 0);
uniform vec4 s_SlopeAngleTexture_Res;

//cubeSamplers
SAMPLERCUBE(s_cubeDepth0, 1);
uniform vec4 s_cubeDepth0_Res;

//definitions
uniform vec4 u_viewshedColor0;
uniform vec4 u_viewshedRange;
uniform vec4 u_viewshedPos0;
uniform vec4 u_viewshedFarPlane;
uniform vec4 u_viewshedStrength;
uniform vec4 u_viewshedBias;
uniform vec4 u_tileSize;
uniform vec4 u_tileDistortion;
uniform vec4 u_heightTileSize;
uniform vec4 u_ScaleOffsetHeight;
uniform vec4 u_lightStrengthPow;
uniform vec4 u_TopoParams;
uniform vec4 u_minorTopoColor;
uniform vec4 u_majorTopoColor;
uniform vec4 u_TopoFade;
uniform vec4 u_TopoHeightFade;
uniform vec4 u_fogVars;
uniform vec4 u_fogColor;
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
	return texture2DLod(s_heightTextureVert, scaledUV, 0).r;
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

vec4 texcoords = a_texcoord7.xyzw;
vec4 normal = a_normal.xyzw;
vec3 position = a_position.xyz;
//main start
	vec2 tileCoord = mix(u_tileMin.xy, u_tileMax.xy, position.xy);
	float baseHeight = u_tileMin.z + (position.z * u_tileMax.z);
	vec4 worldPosition = vec4(tileCoord, baseHeight, baseHeight);
	vec3 vertexPosition = worldPosition.xyz;
normal = mul(u_model[0], vec4(normal.xyz, 0.0));
mat4 viewMat = u_view;
	float z = distortedHeightAt(texcoords.xy, u_tileDistortion.xy, u_ScaleOffsetHeight);
	worldPosition.zw += vec2(z, heightAt(texcoords.xy, u_ScaleOffsetHeight));
	vertexPosition.z += z * u_tileSize.z;
	vec4 tileDistortion = u_tileDistortion;
	vec4 scaleOffsetHeight = u_ScaleOffsetHeight;

//lighting
vec4 fogDist = vec4(length(worldPosition.xyz) / u_nearFarPlane.y, 0.0,0.0,0.0);


//compose
	vec4 projected = mul(u_proj, mul(viewMat, vec4(vertexPosition.xyz, 1.0)));
	vec4 depth = projected;
	gl_Position = projected;

v_texcoord2 = scaleOffsetHeight.xyzw;
v_texcoord3 = tileDistortion.xyzw;
v_texcoord5 = texcoords.xyzw;
v_texcoord4 = fogDist.xyzw;
v_normal = normal.xyz;
v_texcoord6 = depth.xyzw;
v_texcoord7 = worldPosition.xyzw;

}

