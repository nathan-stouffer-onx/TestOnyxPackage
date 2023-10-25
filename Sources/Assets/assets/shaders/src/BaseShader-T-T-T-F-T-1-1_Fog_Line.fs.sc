$input v_texcoord2, v_texcoord3, v_texcoord5, v_texcoord4, v_normal, v_texcoord6, v_texcoord7
//includes
#include <common.sh>
#include "map3dFunctions.sc"
#include "map3dFragFunctions.sc"

//samplers

//cubeSamplers

//definitions
uniform vec4 u_tileLevel;
uniform vec4 u_screenDimensions;
uniform vec4 u_lineWidth;
uniform vec4 u_dashLength;
uniform vec4 u_gapLength;
uniform vec4 u_fogVars;
uniform vec4 u_fogColor;
uniform vec4 u_nearFarPlane;
uniform vec4 u_eyePos;
uniform vec4 u_camRight;
uniform vec4 u_camForward;
uniform vec4 u_camUp;
uniform vec4 u_time;
uniform vec4 u_tileMin;
uniform vec4 u_tileMax;

//functions
vec3 calcFogResult(vec3 color, float dist)
{
	float d = smoothstep(u_fogVars.x, 1.0, dist);
	return mix(color, u_fogColor.rgb, d);
}
vec3 convertToLineNormal(vec3 dataFromBuffer)
{
  vec3 lineNormal = vec3(-dataFromBuffer.y, dataFromBuffer.x, 0.0);
  lineNormal /= length(lineNormal);
  return lineNormal;
}

float lineStyleAlpha(float distAlongLine, float dashLen, float gapLen)
{
  float period = dashLen + gapLen;
  float theta = mod(distAlongLine, period);
  return float(theta <= dashLen);
}


void main()
{

vec4 line_color = v_texcoord2.xyzw;
vec4 line_texcoord = v_texcoord3.xyzw;
vec4 texcoords = v_texcoord5.xyzw;
vec4 fogDist = v_texcoord4.xyzw;
vec3 normal = v_normal.xyz;
vec4 depth = v_texcoord6.xyzw;
vec4 worldPosition = v_texcoord7.xyzw;
//main start
vec4 fragColor = vec4(1.0, 1.0, 1.0, 0.0);
float styleAlpha = lineStyleAlpha(line_texcoord.y, u_dashLength.x, u_gapLength.x);

fragColor = vec4(line_color.xyz, line_color.a * styleAlpha);


//lighting
fragColor.xyz = calcFogResult(fragColor.xyz, fogDist.x);


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
