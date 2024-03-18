$input v_texcoord7, v_texcoord6
//includes
#include <common.sh>
#include "OnyxFunctions.sc"
#include "OnyxFragFunctions.sc"

//samplers
SAMPLER2D(s_texture0, 0);
uniform vec4 s_texture0_Res;

//cubeSamplers

//definitions
uniform vec4 u_SharpenStrength;
uniform vec4 u_ScaleOffsetTex0;
uniform vec4 u_OpacityTex0;
uniform vec4 u_BackgroundColor;
uniform vec4 u_nearFarPlane;
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
	return color;
}


vec3 texSample(const int x, const int y, vec2 origUV)
{
vec2 uv = origUV.xy + vec2(x,y) * s_texture0_Res.zw;
  return texture2D(s_texture0, uv).xyz;
}

void main()
{

vec4 depth = v_texcoord7.xyzw;
vec4 texcoords = v_texcoord6.xyzw;
//main start
vec4 fragColor = u_BackgroundColor;
fragColor = BlendTextures(fragColor, texcoords.xy);
vec2 pixelStep = vec2(1.0, 1.0) * s_texture0_Res.zw;
vec2 modUV = u_ScaleOffsetTex0.xy + texcoords.xy * u_ScaleOffsetTex0.zw;
vec3 result = texSample(-1,-1, modUV) * -1. + texSample(0,-1, modUV) * -1. + texSample(1,-1, modUV) * -1. + texSample(-1,0, modUV) * -1. + texSample(0,0, modUV) * 9. + texSample(1,0, modUV) * -1. + texSample(-1,1, modUV) * -1. + texSample(0,1, modUV) * -1. + texSample(1,1, modUV) * -1.;
fragColor.rgb = mix(fragColor.xyz, result, u_SharpenStrength.x);

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
