$input v_color1, v_color0, v_texcoord0, v_position
//includes
#include <../examples/common/common.sh>
#include "map3dFunctions.sc"

//samplers
SAMPLER2D(s_heightTextureVert, 0);
uniform vec4 s_heightTextureVert_Res;
SAMPLER2D(s_texture0, 1);
uniform vec4 s_texture0_Res;

//cubeSamplers

//definitions
uniform vec4 u_tileSize;
uniform vec4 u_ScaleOffsetHeight;
uniform vec4 u_tileDistortion;
uniform vec4 u_MeshResolution;
uniform vec4 u_depthModifier;
uniform vec4 u_ScaleOffsetTex0;
uniform vec4 u_tileMin;
uniform vec4 u_tileMax;
uniform vec4 u_scale;
uniform vec4 color1;
uniform vec4 color0;
uniform vec4 quatRot;
uniform vec4 pixelSize;
uniform vec4 iconPos;

//functions
vec4 BlendTextures(vec4 color, vec2 uv)
{
	vec4 tex = vec4(0.0, 0.0, 0.0, 0.0);
	{
vec2 modUV = u_ScaleOffsetTex0.xy + uv * u_ScaleOffsetTex0.zw;
	tex = texture2D(s_texture0, modUV);
	color.xyz = mix(color.xyz, tex.xyz, tex.a);
	color.a += tex.a;
	}
	return color;
}



void main()
{

vec4 color1 = v_color1.xyzw;
vec4 color0 = v_color0.xyzw;
vec4 texcoord0 = v_texcoord0.xyzw;
vec3 position = v_position.xyz;
//main start
	vec2 texcoords = texcoord0.xy;
	vec4 fragColor = vec4_splat(0.f);
fragColor = BlendTextures(fragColor, texcoords.xy);

//lighting

//compose
	gl_FragColor = fragColor;


}

