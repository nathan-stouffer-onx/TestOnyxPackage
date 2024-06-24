$input a_position, i_data0
$output v_bitangent

//includes
#include <../examples/common/common.sh>
#include "layers.sc"

//samplers
SAMPLER2D(s_posLifeTex, 0);
uniform vec4 s_posLifeTex_Res;
SAMPLER2D(s_velocSeedTex, 1);
uniform vec4 s_velocSeedTex_Res;

//cubeSamplers

//definitions
uniform vec4 u_bbSize;
uniform vec4 u_emitterPos;
uniform vec4 u_camUp;
uniform vec4 u_camRight;

//functions

void main()
{

vec3 quadPos = a_position.xyz;
vec4 i_uv = i_data0.xyzw;
//main start
	vec2 uvCoord = i_uv.xy;
	vec4 posLife = texture2DLod(s_posLifeTex, uvCoord, 0);

	// Use camera vectors to define quad
	vec2 bbScale = vec2(u_bbSize.x * quadPos.x, u_bbSize.y * (quadPos.y));

	vec3 worldOffset = u_emitterPos.xyz + posLife.xyz;
	//tvec3 worldOffset = u_emitterPos.xyz;
	vec3 bbPos = worldOffset + bbScale.x * u_camRight.xyz + bbScale.y * u_camUp.xyz;

	// Transform to world position
	gl_Position = mul(u_viewProj, vec4(bbPos, 1.f));

	vec3 uvLife = vec3(quadPos.xy, posLife.w);

//lighting

//compose

v_bitangent = uvLife.xyz;

}

