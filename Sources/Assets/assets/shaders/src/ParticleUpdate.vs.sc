$input a_position
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
uniform vec4 u_velLifeMinMax;
uniform vec4 u_initAccelDeltaProg;

//functions

void main()
{

vec3 quadPos = a_position.xyz;
//main start
	gl_Position = vec4(2.f * quadPos.xy - 1.f, 0, 1.f);
	vec3 uv = vec3(quadPos.xy, 0);

//lighting

//compose

v_bitangent = uv.xyz;

}

