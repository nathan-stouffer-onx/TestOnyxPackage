$input v_texcoord7, v_texcoord6
//includes
#include <common.sh>
#include "layers.sc"
#include "derivatives.sc"

//samplers
SAMPLER2D(s_BlurTex, 0);
uniform vec4 s_BlurTex_Res;

//cubeSamplers

//definitions
uniform vec4 u_GaussBlurScale;
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

void main()
{

vec4 depth = v_texcoord7.xyzw;
vec4 texcoords = v_texcoord6.xyzw;
//main start
vec4 fragColor = u_BackgroundColor;
	vec4 color = vec4(0.0, 0.0, 0.0, 0.0);
	vec4 c[7];
	float gaussScale[7];
	gaussScale[0] = 1.0 / 64.0;
	gaussScale[1] = 6.0/64.0;
	gaussScale[2] = 15.0/64.0;
	gaussScale[3] = 20.0/64.0;
	gaussScale[4] = 15.0/64.0;
	gaussScale[5] = 6.0/64.0;
	gaussScale[6] = 1.0/64.0;
	float count = 0.0;
	c[0] = texture2D(s_BlurTex, texcoords.xy + (vec2(-3.0, -3.0) * u_GaussBlurScale.xy));
	c[1] = texture2D(s_BlurTex, texcoords.xy + (vec2(-2.0, -2.0) * u_GaussBlurScale.xy));
	c[2] = texture2D(s_BlurTex, texcoords.xy + (vec2(-1.0, -1.0) * u_GaussBlurScale.xy));
	c[3] = texture2D(s_BlurTex, texcoords.xy + (vec2(0.0, 0.0)  * u_GaussBlurScale.xy));
	c[4] = texture2D(s_BlurTex, texcoords.xy + (vec2(1.0, 1.0)  * u_GaussBlurScale.xy));
	c[5] = texture2D(s_BlurTex, texcoords.xy + (vec2(2.0, 2.0)  * u_GaussBlurScale.xy));
	c[6] = texture2D(s_BlurTex, texcoords.xy + (vec2(3.0, 3.0)  * u_GaussBlurScale.xy));
	for(int i = 0; i < 7; i++)
	{
		float fakeAlpha = 1.0 - float(c[i].y >= 0.99999);//RG32 so no alpha, but only the clearcolor will have a 1.0 value here
		color += c[i] * fakeAlpha * gaussScale[i];
		count += fakeAlpha * gaussScale[i];
	}
	color /= count;
	if(count <= 0.0001) color = vec4(1.0,1.0,1.0,1.0); //todo - find non branching math if this improves things
	fragColor = color;

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

