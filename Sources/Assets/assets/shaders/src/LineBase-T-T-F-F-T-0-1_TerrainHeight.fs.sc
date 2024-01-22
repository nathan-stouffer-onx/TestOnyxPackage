$input v_texcoord7, v_color0, v_texcoord6, v_texcoord5
//includes
#include <common.sh>
#include "OnyxFunctions.sc"

//samplers
SAMPLER2D(s_heightTexture, 1);
uniform vec4 s_heightTexture_Res;
SAMPLER2D(s_DashSampler, 0);
uniform vec4 s_DashSampler_Res;

//cubeSamplers

//definitions
uniform vec4 u_tileSize;
uniform vec4 u_ScaleOffsetHeight;
uniform vec4 u_tileDistortion;
uniform vec4 u_MeshResolution;
uniform vec4 u_dashUV;
uniform vec4 u_nearFarPlane;
uniform vec4 u_eyePos;
uniform vec4 u_camRight;
uniform vec4 u_camForward;
uniform vec4 u_camUp;
uniform vec4 u_time;
uniform vec4 u_tileMin;
uniform vec4 u_tileMax;
uniform vec4 u_origin;
uniform vec4 u_length;
uniform vec4 u_screenDimensions;
uniform vec4 u_color;
uniform vec4 u_drawColor;
uniform vec4 u_endCaps;

//functions

void main()
{

vec4 linePosition = v_texcoord7.xyzw;
vec4 color = v_color0.xyzw;
vec4 screenPosition = v_texcoord6.xyzw;
vec4 lineCenter = v_texcoord5.xyzw;
//main start
	 vec2 d = abs(linePosition.xy);
	 float pos = dot(d, d);
//	 vec4 tex = texture2D(s_DashSampler, lineCenter.xy / (linePosition.w * s_DashSampler_Res.x));
	 vec4 tex = texture2D(s_DashSampler, lineCenter.xy / (linePosition.w * 8.0));
	 vec4 outColor = vec4(color.xyz, tex.r * color.w * ceil(1.0 - pos));
	// vec4 outColor = vec4(color.xyz, color.w * ceil(1.0 - pos));

//lighting

//compose
	gl_FragData[0] = outColor;
	gl_FragData[1] = vec4(0, 0, 0, 0);


}

