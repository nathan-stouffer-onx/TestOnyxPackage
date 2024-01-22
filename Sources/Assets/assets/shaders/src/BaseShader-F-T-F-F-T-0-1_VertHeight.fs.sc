$input v_texcoord7, v_texcoord6, v_texcoord5, v_texcoord4
//includes
#include <common.sh>
#include "OnyxFunctions.sc"
#include "OnyxFragFunctions.sc"

//samplers
SAMPLER2D(s_heightTexture, 0);
uniform vec4 s_heightTexture_Res;

//cubeSamplers

//definitions
uniform vec4 u_tileSize;
uniform vec4 u_tileDistortion;
uniform vec4 u_heightTileSize;
uniform vec4 u_ScaleOffsetHeight;
uniform vec4 u_lightStrengthPow;
uniform vec4 u_nearFarPlane;
uniform vec4 u_eyePos;
uniform vec4 u_camRight;
uniform vec4 u_camForward;
uniform vec4 u_camUp;
uniform vec4 u_time;
uniform vec4 u_tileMin;
uniform vec4 u_tileMax;

//functions
// for pixel shader -  expects uv to be in tile coordinates
float heightAt(vec2 uv, vec4 scaleOffset)
{
	vec2 scaledUV = scaleOffset.zw * uv + scaleOffset.xy;
	return texture2D(s_heightTexture, scaledUV).r;
}
// expects uv to be in tile coordinates
float distortedHeightAt(vec2 uv, vec2 distortion, vec4 scaleOffset)
{
	float z = heightAt(uv, scaleOffset);
	float distort = mix(distortion.x, distortion.y, uv.y);
	return z * distort;
}

void main()
{

vec4 depth = v_texcoord7.xyzw;
vec4 texcoords = v_texcoord6.xyzw;
vec4 tileDistortion = v_texcoord5.xyzw;
vec4 scaleOffsetHeight = v_texcoord4.xyzw;
//main start
vec4 fragColor = vec4(1.0, 1.0, 1.0, 0.0);

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

