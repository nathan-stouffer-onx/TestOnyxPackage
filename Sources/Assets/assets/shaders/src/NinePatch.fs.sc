$input v_texcoord7
//includes
#include <../examples/common/common.sh>
#include "OnyxFunctions.sc"

//samplers
SAMPLER2D(s_spriteTex, 0);
uniform vec4 s_spriteTex_Res;

//cubeSamplers

//definitions
uniform vec4 u_screenRes;
uniform vec4 i_offsets0;
uniform vec4 i_offsets1;
uniform vec4 i_screenPosSize;
uniform vec4 i_uv;

//functions

void main()
{

vec4 uv = v_texcoord7.xyzw;
//main start
	// Get sprite texture color
	vec4 fragColor = texture2D(s_spriteTex, uv.xy);

//lighting

//compose
	gl_FragColor = fragColor;


}

