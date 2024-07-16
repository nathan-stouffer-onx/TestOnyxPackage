$input v_texcoord7, v_texcoord6
//includes
#include <common.sh>
#include "layers.sc"
#include "derivatives.sc"

//samplers
SAMPLER2D(s_FrameTexture, 0);
uniform vec4 s_FrameTexture_Res;

//cubeSamplers

//definitions
uniform vec4 u_SharpenStrength;
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
vec3 texSample(const int x, const int y, vec2 origUV)
{
vec2 uv = origUV.xy + vec2(x,y) * s_FrameTexture_Res.zw;
  return texture2D(s_FrameTexture, uv).xyz;
}

void main()
{

vec4 depth = v_texcoord7.xyzw;
vec4 texcoords = v_texcoord6.xyzw;
//main start
vec4 fragColor = u_BackgroundColor;
vec2 pixelStep = s_FrameTexture_Res.zw;
vec2 modUV = texcoords.xy;
fragColor.xyz = texture2D(s_FrameTexture, modUV).xyz;
vec3 result = texSample(-1,-1, modUV) * -1. + texSample(0,-1, modUV) * -1. + texSample(1,-1, modUV) * -1. + texSample(-1,0, modUV) * -1. + fragColor.xyz * 9. + texSample(1,0, modUV) * -1. + texSample(-1,1, modUV) * -1. + texSample(0,1, modUV) * -1. + texSample(1,1, modUV) * -1.;
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

