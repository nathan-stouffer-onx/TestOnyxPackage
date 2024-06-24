$input a_position, a_texcoord0, a_color0, a_color1, a_color2
$output v_color2, v_position, v_color1, v_texcoord0, v_color0

//includes
#include <../examples/common/common.sh>
#include "layers.sc"

//samplers
SAMPLER2D(s_texture0, 0);
uniform vec4 s_texture0_Res;

//cubeSamplers

//definitions
uniform vec4 u_ScaleOffsetTex0;
uniform vec4 u_depthModifier;
uniform vec4 u_midpoint;
uniform vec4 u_scale;
uniform vec4 u_quatRotation;
uniform vec4 u_translation;
uniform vec4 u_NearFarFocus;
uniform vec4 u_eyePos;
uniform vec4 u_camRight;
uniform vec4 u_camForward;
uniform vec4 u_camUp;

//functions

void main()
{

vec3 position = a_position.xyz;
vec4 texcoord0 = a_texcoord0.xyzw;
vec4 color0 = a_color0.xyzw;
vec4 color1 = a_color1.xyzw;
vec4 color2 = a_color2.xyzw;
//main start
	vec3 midpoint = vec3_splat(0.0);
	midpoint = u_midpoint.xyz;
	// Keep sized fixed regardless of cam distance
	position = length(mul(u_view, vec4(midpoint.xyz, 1.0)).xyz) * position;
	// Scale, rotate, translate position
	float scale = u_scale.x;
	position = scale * position;
	position = rotate(u_quatRotation, position) + u_translation.xyz;

//lighting

//compose
	gl_Position = vec4(position, 1.0);
	mat4 viewMat = u_view;
	gl_Position = mul(u_proj, mul(viewMat, vec4(position.xyz, 1.0)));
	//Bias depth towards cam to draw vertex over occlusions
	gl_Position.z -= gl_Position.w * u_depthModifier.x;

v_color2 = color2.xyzw;
v_position = position.xyz;
v_color1 = color1.xyzw;
v_texcoord0 = texcoord0.xyzw;
v_color0 = color0.xyzw;

}

