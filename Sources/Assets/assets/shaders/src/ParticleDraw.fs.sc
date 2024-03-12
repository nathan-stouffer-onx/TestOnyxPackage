$input v_bitangent
//includes
#include <../examples/common/common.sh>
#include "OnyxFunctions.sc"

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
uniform vec4 i_uv;

//functions

void main()
{

vec3 uvLife = v_bitangent.xyz;
//main start
	float life = uvLife.z;
	life = life * life * life * life * life;
	gl_FragData[0] = vec4(0.8f, 0.8f, 1.0f, life);

//lighting

//compose


}

