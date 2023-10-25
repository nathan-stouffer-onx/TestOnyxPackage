$input a_color0, a_texcoord0, a_normal, a_position
$output v_color0, v_texcoord0, v_normal, v_position

//includes
#include <../examples/common/common.sh>
#include "map3dFunctions.sc"

//samplers
SAMPLER2D(s_bbHeightTexture, 0);
uniform vec4 s_bbHeightTexture_Res;
SAMPLER2D(s_texture0, 0);
uniform vec4 s_texture0_Res;
SAMPLER2D(s_texture1, 1);
uniform vec4 s_texture1_Res;

//cubeSamplers

//definitions
uniform vec4 u_bbOffset;
uniform vec4 u_billboardSize;
uniform vec4 u_bbHeightUV;
uniform vec4 u_ScaleOffsetTex0;
uniform vec4 u_ScaleOffsetTex1;
uniform vec4 u_nearFarPlane;
uniform vec4 u_eyePos;
uniform vec4 u_camRight;
uniform vec4 u_camForward;
uniform vec4 u_camUp;

//functions

void main()
{

vec4 color0 = a_color0.xyzw;
vec4 texcoord0 = a_texcoord0.xyzw;
vec4 normal = a_normal.xyzw;
vec3 position = a_position.xyz;
//main start
position = mul(u_model[0], vec4(a_position, 1.0)).xyz;
normal = mul(u_model[0], vec4(a_normal.xyz, 0.0) );
vec3 pivot = vec3(a_normal.xyz);
vec2 size = u_billboardSize.xy;
pivot.z += u_bbOffset.x;
vec2 bbScale = vec2(size.x * (a_position.x - 0.5), size.y * a_position.y); 
vec3 bbPos = pivot + u_camRight.xyz * bbScale.x + u_camUp.xyz * bbScale.y;

//lighting

//compose
gl_Position = vec4(position, 1);
v_normal = normal.xyz;
v_position = bbPos;
gl_Position = mul(u_viewProj, vec4(bbPos, 1.0));

v_color0 = color0.xyzw;
v_texcoord0 = texcoord0.xyzw;
v_normal = normal.xyz;
v_position = position.xyz;

}
