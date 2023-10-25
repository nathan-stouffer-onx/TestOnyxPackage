$input v_normal, v_texcoord5, v_texcoord6, v_texcoord7
//includes
#include <common.sh>
#include "map3dFunctions.sc"
#include "map3dFragFunctions.sc"

//samplers
SAMPLER2D(s_texture0, 0);
uniform vec4 s_texture0_Res;
SAMPLER2D(s_texture1, 1);
uniform vec4 s_texture1_Res;

//cubeSamplers

//definitions
uniform vec4 u_ScaleOffsetTex0;
uniform vec4 u_ScaleOffsetTex1;
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
	color.xyz = mix(color.xyz, tex.xyz, tex.a);
	color.a += tex.a;
	}
	{
vec2 modUV = u_ScaleOffsetTex1.xy + uv * u_ScaleOffsetTex1.zw;
	tex = texture2D(s_texture1, modUV);
	color.xyz = mix(color.xyz, tex.xyz, tex.a);
	color.a += tex.a;
	}
	return color;
}



void main()
{

vec3 normal = v_normal.xyz;
vec4 texcoords = v_texcoord5.xyzw;
vec4 depth = v_texcoord6.xyzw;
vec4 worldPosition = v_texcoord7.xyzw;
//main start
vec4 fragColor = vec4(1.0, 1.0, 1.0, 0.0);
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

