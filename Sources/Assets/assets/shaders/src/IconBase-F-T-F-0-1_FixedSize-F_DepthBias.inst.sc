$input a_position, a_texcoord0, i_data0, i_data1, i_data2
$output v_position, v_texcoord0, v_texcoord7

//includes
#include <../examples/common/common.sh>
#include "OnyxFunctions.sc"

//samplers
SAMPLER2D(s_spriteTex, 0);
uniform vec4 s_spriteTex_Res;

//cubeSamplers

//definitions
uniform vec4 u_depthModifier;
uniform vec4 u_scale;

//functions

void main()
{

vec3 position = a_position.xyz;
vec4 texcoord0 = a_texcoord0.xyzw;
vec4 i_iconPos = i_data0.xyzw;
vec4 i_pixelSize = i_data1.xyzw;
vec4 i_scaleOffsetTex = i_data2.xyzw;
//main start
	// Custom aliases
	vec3 midpoint = i_iconPos.xyz; // For compatibility with FixedSize
	float uniformScale = u_scale.x;
	vec2 pixelSize = i_pixelSize.xy;
	vec2 scale = uniformScale * pixelSize;
	// Keep sized fixed regardless of cam distance
	position = length(mul(u_view, vec4(midpoint.xyz, 1.0)).xyz) * position;

//lighting

//compose
	gl_Position = mul(u_proj, mul(u_view, vec4(position.xyz, 1.0)));
	// Aliases for output to pixel shader
	vec4 scaleOffsetTex = i_scaleOffsetTex;
	//Bias depth towards cam to draw vertex over occlusions
	gl_Position.z -= gl_Position.w * u_depthModifier.x;

v_position = position.xyz;
v_texcoord0 = texcoord0.xyzw;
v_texcoord7 = scaleOffsetTex.xyzw;

}

