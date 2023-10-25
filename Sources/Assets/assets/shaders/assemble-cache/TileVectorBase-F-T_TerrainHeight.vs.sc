$input a_texcoord7
$output v_texcoord4, v_texcoord5, v_texcoord6, v_texcoord7

//includes
#include <common.sh>
#include "map3dFunctions.sc"

//samplers
SAMPLER2D(s_heightTextureVert, 0);
uniform vec4 s_heightTextureVert_Res;
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
uniform vec4 u_nearFarPlane;
uniform vec4 u_screenResolution;
uniform vec4 u_eyePos;
uniform vec4 u_camRight;
uniform vec4 u_camForward;
uniform vec4 u_camUp;
uniform vec4 u_time;
uniform vec4 u_tileMin;
uniform vec4 u_vectorFade;
uniform vec4 u_tileMax;
uniform vec4 u_TileFragClip;

//functions
// expects uv to be in tile coordinates
float heightAt(vec2 uv)
{
	vec2 scaledUV = u_ScaleOffsetHeight.zw * uv + u_ScaleOffsetHeight.xy;
	return texture2DLod(s_heightTextureVert, scaledUV, 0).r;
}
// expects uv to be in tile coordinates
float distortedHeightAt(vec2 uv)
{
	float z = heightAt(uv);
	float distortion = mix(u_tileDistortion.x, u_tileDistortion.y, uv.y);
	return z * distortion;
}

void main()
{

vec4 tilePosition_style = a_texcoord7.xyzw;
//main start
	vec2 tilePos = vec2(tilePosition_style.xy);
	float tileZ = u_tileMin.z;
	vec3 worldPosition = vec3(mix(u_tileMin.xy, u_tileMax.xy, tilePos), u_tileMin.z);
	 vec4 vecPattern = texture2DLod(s_vectorPatterns, tilePosition_style.zw, 0);
	 vecPattern.zw = vecPattern.zw * s_patterns_Res.xy;
	 vec4 vecColor = texture2DLod(s_vectorColors, tilePosition_style.zw, 0);
	vecColor.a *= 0.5f;
mat4 viewMat = u_view;
	float z = distortedHeightAt(tilePos);
	tileZ += z * u_tileSize.z;

//lighting

//compose
	vec4 projected = mul(u_proj, mul(viewMat, vec4(worldPosition.xy, tileZ, 1.0)));
	projected.z -= (projected.w * 0.01);
	//projected.z -= (1.0 / 128.0);
	vec4 depth = projected;
	vec4 tilePosition = vec4(tilePos, 0.0, 0.0);
	gl_Position = projected;

v_texcoord4 = vecPattern.xyzw;
v_texcoord5 = vecColor.xyzw;
v_texcoord6 = depth.xyzw;
v_texcoord7 = tilePosition.xyzw;

}

