$input v_texcoord7, v_texcoord6, v_color0
//includes
#include <common.sh>
#include "layers.sc"
#include "derivatives.sc"

//samplers

//cubeSamplers

//definitions
uniform vec4 u_overrideColor;
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
vec4 color = v_color0.xyzw;
//main start
vec4 fragColor = u_BackgroundColor;
fragColor = vec4(color.x, 0, 0, 1.0);


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

