$input a_position, a_texcoord0
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
uniform vec4 i_iconPos;
uniform vec4 i_pixelSize;
uniform vec4 i_scaleOffsetTex;

//functions

void main()
{

vec3 position = a_position.xyz;
vec4 texcoord0 = a_texcoord0.xyzw;
//main start
	// Custom aliases
	vec3 midpoint = i_iconPos.xyz; // For compatibility with FixedSize
	float uniformScale = u_scale.x;
	vec2 pixelSize = i_pixelSize.xy;
	vec2 scale = uniformScale * pixelSize;

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

