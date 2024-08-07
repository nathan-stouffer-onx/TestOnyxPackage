$input a_texcoord5, a_position, a_texcoord7, a_texcoord6, a_texcoord4
$output v_position, v_texcoord7, v_texcoord6, v_texcoord5, v_texcoord4

//includes
#include <../examples/common/common.sh>
#include "layers.sc"

//samplers

//cubeSamplers
SAMPLERCUBE(s_fontAtlas, 0);
uniform vec4 s_fontAtlas_Res;

//definitions
uniform vec4 u_sdfParams;
uniform vec4 u_dropShadowColor;
uniform vec4 u_NearFarFocus;
uniform vec4 u_eyePos;
uniform vec4 u_camRight;
uniform vec4 u_camForward;
uniform vec4 u_camUp;

//functions

void main()
{

vec4 fontColor0 = a_texcoord5.xyzw;
vec3 position = a_position.xyz;
vec4 sdf_tex0 = a_texcoord7.xyzw;
vec4 sdf_options = a_texcoord6.xyzw;
vec4 fontColor1 = a_texcoord4.xyzw;
//main start
position.xy = mul(u_model[0], vec4(position.xy, 0, 1)).xy;  // Preserve Z as depth

//lighting

//compose
gl_Position = vec4(position, 1);

v_position = position.xyz;
v_texcoord7 = sdf_tex0.xyzw;
v_texcoord6 = sdf_options.xyzw;
v_texcoord5 = fontColor0.xyzw;
v_texcoord4 = fontColor1.xyzw;

}

