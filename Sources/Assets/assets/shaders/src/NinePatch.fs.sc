$input v_texcoord7, v_texcoord6, v_depth
//includes
#include <../examples/common/common.sh>
#include "layers.sc"

//samplers
SAMPLER2D(s_spriteTex1, 0);
uniform vec4 s_spriteTex1_Res;
SAMPLER2D(s_spriteTex2, 1);
uniform vec4 s_spriteTex2_Res;

//cubeSamplers

//definitions
uniform vec4 u_screenRes;
uniform vec4 u_oriAngle;
uniform vec4 i_offsets0;
uniform vec4 i_offsets1;
uniform vec4 i_screenPosSize;
uniform vec4 i_uvXAxis;
uniform vec4 i_yAxisOpacityTexId;

//functions

void main()
{

vec4 uv = v_texcoord7.xyzw;
vec4 opacity = v_texcoord6.xyzw;
float texId = v_depth;
//main start
	// Get sprite texture color
	vec4 fragColor = (texId == 0.f) ? texture2D(s_spriteTex1, uv.xy) : texture2D(s_spriteTex2, uv.xy);

//lighting

//compose
	fragColor.w *= opacity.x;
	gl_FragColor = fragColor;


}

