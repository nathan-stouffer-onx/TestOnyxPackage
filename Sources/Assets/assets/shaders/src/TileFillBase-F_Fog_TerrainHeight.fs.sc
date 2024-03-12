$input v_texcoord4, v_bitangent, v_texcoord7, v_texcoord6, v_depth, v_texcoord5, v_texcoord3
//includes
#include <common.sh>
#include "OnyxFunctions.sc"

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
uniform vec4 u_FogTransition;
uniform vec4 u_FogColor;
uniform vec4 u_tileVectorData;
uniform vec4 u_nearFarPlane;
uniform vec4 u_screenResolution;
uniform vec4 u_eyePos;
uniform vec4 u_camRight;
uniform vec4 u_camForward;
uniform vec4 u_camUp;
uniform vec4 u_time;
uniform vec4 u_tileMin;
uniform vec4 u_TileVectorOpacityTransition;
uniform vec4 u_vectorFade;
uniform vec4 u_tileMax;
uniform vec4 u_TileVertClip;
uniform vec4 u_TileFragClip;

//functions
vec3 calcFogResult(vec3 color, vec2 transition, float t)
{
	float d = smoothstep(transition.x, transition.y, t);
	return mix(color, u_FogColor.rgb, d);
}

void main()
{

vec4 vecPattern = v_texcoord4.xyzw;
vec3 worldPosition = v_bitangent.xyz;
vec4 tilePosition = v_texcoord7.xyzw;
vec4 depth = v_texcoord6.xyzw;
float distFade = v_depth;
vec4 vecColor = v_texcoord5.xyzw;
vec4 fogDist = v_texcoord3.xyzw;
//main start
	float inX = inRange(tilePosition.x, u_TileFragClip.x, u_TileFragClip.z);
	float inY = inRange(tilePosition.y, u_TileFragClip.y, u_TileFragClip.w);
	if (inX * inY == 0.0)
	{
		discard;
	}
vec4 fragColor = vecColor;

//lighting
fragColor.rgb = calcFogResult(fragColor.rgb, u_FogTransition.xy, fogDist.x);


//compose
	vec2 normalized = (depth.xy / depth.w) * 0.5f + 0.5f;
	vec2 worldUV = worldPosition.xy + u_eyePos.xy;
	float vectorLevel = 20.0 - u_tileVectorData.x;
	worldUV *= 1000.0;
	worldUV /= pow(2.0, vectorLevel);
	vec2 uvOffset = vec2(mod(worldUV.x, vecPattern.z), mod(worldUV.y, vecPattern.w)) * s_patterns_Res.zw;
	vec4 pattern = texture2DLod(s_patterns, vecPattern.xy + uvOffset, 0);
	gl_FragData[0] = fragColor * pattern * distFade;
	gl_FragData[1] = vec4(0, 0, 0, 0);


}

