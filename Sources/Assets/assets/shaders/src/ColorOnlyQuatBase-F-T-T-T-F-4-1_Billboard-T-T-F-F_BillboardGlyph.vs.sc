$input a_normal, a_position, a_color3, a_texcoord0, a_color0, a_color1, a_color2
$output v_color2, v_position, v_normal, v_color1, v_texcoord0, v_color0, v_color3

//includes
#include <../examples/common/common.sh>
#include "layers.sc"

//samplers
SAMPLER2D(s_texture0, 0);
uniform vec4 s_texture0_Res;

//cubeSamplers

//definitions
uniform vec4 u_ScaleOffsetTex0;
uniform vec4 u_bbOffset;
uniform vec4 u_billboardSize;
uniform vec4 u_pivot;
uniform vec4 u_NearFarFocus;
uniform vec4 u_eyePos;
uniform vec4 u_camRight;
uniform vec4 u_camForward;
uniform vec4 u_camUp;

//functions

void main()
{

vec4 normal = a_normal.xyzw;
vec3 position = a_position.xyz;
vec4 color3 = a_color3.xyzw;
vec4 texcoord0 = a_texcoord0.xyzw;
vec4 color0 = a_color0.xyzw;
vec4 color1 = a_color1.xyzw;
vec4 color2 = a_color2.xyzw;
//main start
	vec3 midpoint = vec3_splat(0.0);
vec3 pivot = vec3(a_normal.xyz);
vec2 size = u_billboardSize.xy;
float sizeCorrection = length(mul(u_view, vec4(pivot, 1.0)).xyz);
size *= sizeCorrection;
pivot.z += u_bbOffset.x;
vec2 bbScale = vec2(size.x * (a_position.x - 0.5), size.y * a_position.y); 
vec3 bbPos = pivot + u_camRight.xyz * bbScale.x + u_camUp.xyz * bbScale.y;

//lighting

//compose
	gl_Position = vec4(position, 1.0);
	v_normal = normal.xyz;
v_position = bbPos;
gl_Position = mul(u_viewProj, vec4(bbPos, 1.0));

v_color2 = color2.xyzw;
v_position = position.xyz;
v_normal = normal.xyz;
v_color1 = color1.xyzw;
v_texcoord0 = texcoord0.xyzw;
v_color0 = color0.xyzw;
v_color3 = color3.xyzw;

}

