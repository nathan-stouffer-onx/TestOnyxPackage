$input v_position, v_texcoord0, v_texcoord7
//includes
#include <../examples/common/common.sh>
#include "layers.sc"

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

vec3 position = v_position.xyz;
vec4 texcoord0 = v_texcoord0.xyzw;
vec4 scaleOffsetTex = v_texcoord7.xyzw;
//main start
	// Get sprite texture color
	vec4 fragColor = vec4_splat(0.f);
	vec2 texcoords = texcoord0.xy;
	fragColor = read(s_spriteTex, texcoords, scaleOffsetTex);

//lighting

//compose
	gl_FragColor = fragColor;


}

