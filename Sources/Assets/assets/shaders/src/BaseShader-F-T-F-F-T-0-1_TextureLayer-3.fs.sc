$input v_texcoord7, v_texcoord6
//includes
#include <common.sh>
#include "layers.sc"
#include "derivatives.sc"

//samplers
SAMPLER2D(s_texture0, 0);
uniform vec4 s_texture0_Res;
SAMPLER2D(s_texture1, 1);
uniform vec4 s_texture1_Res;
SAMPLER2D(s_texture2, 2);
uniform vec4 s_texture2_Res;

//cubeSamplers

//definitions
uniform vec4 u_ScaleOffsetTex0;
uniform vec4 u_OpacityTex0;
uniform vec4 u_ScaleOffsetTex1;
uniform vec4 u_OpacityTex1;
uniform vec4 u_ScaleOffsetTex2;
uniform vec4 u_OpacityTex2;
uniform vec4 u_BackgroundColor;
uniform vec4 u_NearFarFocus;
uniform vec4 u_eyePos;
uniform vec4 u_camRight;
uniform vec4 u_camForward;
uniform vec4 u_camUp;
uniform vec4 u_time;
uniform vec4 u_tileMin;
uniform vec4 u_tileMax;

//functions
vec4 BlendTextures(vec4 color, vec2 uv)
{
	vec4 tex = vec4(0.0, 0.0, 0.0, 0.0);
	{
vec2 modUV = u_ScaleOffsetTex0.xy + uv * u_ScaleOffsetTex0.zw;
	tex = texture2D(s_texture0, modUV);
	float t = tex.a * u_OpacityTex0.x;
	color.xyz = mix(color.xyz, tex.xyz, t);
	color.a = max(color.a, tex.a);
	}
	{
vec2 modUV = u_ScaleOffsetTex1.xy + uv * u_ScaleOffsetTex1.zw;
	tex = texture2D(s_texture1, modUV);
	float t = tex.a * u_OpacityTex1.x;
	color.xyz = mix(color.xyz, tex.xyz, t);
	color.a = max(color.a, tex.a);
	}
	{
vec2 modUV = u_ScaleOffsetTex2.xy + uv * u_ScaleOffsetTex2.zw;
	tex = texture2D(s_texture2, modUV);
	float t = tex.a * u_OpacityTex2.x;
	color.xyz = mix(color.xyz, tex.xyz, t);
	color.a = max(color.a, tex.a);
	}
	return color;
}



void main()
{

vec4 depth = v_texcoord7.xyzw;
vec4 texcoords = v_texcoord6.xyzw;
//main start
vec4 fragColor = u_BackgroundColor;
fragColor = BlendTextures(fragColor, texcoords.xy);

//lighting

//compose
	gl_FragData[0] = fragColor;
	//scaling from -1,1 to 0,1, but I suspect that may only be needed for opengl?
	float d = ((depth.z / depth.w) * 0.5 + 0.5) * 256.0;
	float r = floor(d);
	d = (d - r) * 256.0;
	float g = floor(d);
	d = (d - g) * 256.0;
	float b = floor(d);
	gl_FragData[1] = vec4(r / 256.0, g / 256.0, b / 256.0, 1.0);


}

