$input v_texcoord7, v_texcoord6, v_texcoord5, v_texcoord4
//includes
#include <../examples/common/common.sh>
#include "layers.sc"

//samplers

//cubeSamplers
SAMPLERCUBE(s_fontAtlas, 0);
uniform vec4 s_fontAtlas_Res;

//definitions
uniform vec4 u_sdfParams;
uniform vec4 u_dropShadowColor;
uniform vec4 u_NearFarFocus;
uniform vec4 u_eyePos;
uniform vec4 u_camRight;
uniform vec4 u_camForward;
uniform vec4 u_camUp;
#define u_distanceMultiplier     u_sdfParams.y
#define u_dropShadowSoftener     u_sdfParams.z
#define u_outlineWidth           u_sdfParams.w

//functions

void main()
{

vec4 sdf_tex0 = v_texcoord7.xyzw;
vec4 sdf_options = v_texcoord6.xyzw;
vec4 fontColor0 = v_texcoord5.xyzw;
vec4 fontColor1 = v_texcoord4.xyzw;
//main start
vec4 fragColor = vec4(0,0,0,0);
//fragColor = vec4(1,1,0,1);
//return; //temp for testing
if (sdf_options.w == 1.0f)
{
	vec4 imageColor = textureCube(s_fontAtlas, sdf_tex0.xyz);
	gl_FragColor = vec4(imageColor.xyz, imageColor.w * fontColor0.w);
	return;
}
if (sdf_options.w == -1.0f)
{
	vec4 shadowTexCoord = sdf_tex0;
	vec4 shadowDistanceColor = textureCube(s_fontAtlas, shadowTexCoord.xyz);
	int index2 = int(shadowTexCoord.w * 4.0 + 0.5);
	float rgba2[4];
	rgba2[0] = shadowDistanceColor.z;
	rgba2[1] = shadowDistanceColor.y;
	rgba2[2] = shadowDistanceColor.x;
	rgba2[3] = shadowDistanceColor.w;
	float shadowDistance = rgba2[index2];
	float shadowSmoothing = 16.0 * length(fwidth(shadowTexCoord.xyz)) / sqrt(2.0) * u_distanceMultiplier;
	float outlineWidth = u_outlineWidth * shadowSmoothing;
	float outerEdgeCenter = 0.5 - outlineWidth;
	shadowSmoothing = shadowSmoothing * u_dropShadowSoftener;
	float shadowAlpha = smoothstep(outerEdgeCenter - shadowSmoothing, outerEdgeCenter + shadowSmoothing, shadowDistance);
	vec4 shadowColor = vec4(fontColor0.xyz, shadowAlpha * fontColor0.w);
	gl_FragColor = shadowColor;
	return;
}
vec4 color = textureCube(s_fontAtlas, sdf_tex0.xyz);
int index = int(sdf_tex0.w * 4.0 + 0.5);
float rgba[4];
rgba[0] = color.z;
rgba[1] = color.y;
rgba[2] = color.x;
rgba[3] = color.w;
float distance = rgba[index];
float smoothing = 16.0 * length(fwidth(sdf_tex0.xyz)) / sqrt(2.0) * u_distanceMultiplier;
float outlineWidth = u_outlineWidth * smoothing;
float outerEdgeCenter = 0.5 - outlineWidth;
float alpha = smoothstep(outerEdgeCenter - smoothing, outerEdgeCenter + smoothing, distance);
float borderBlendCorrection = 0.47; //not sure why, but at 0.5 our text color was being blended with the border beyond the edges of the border, .47 seems to look a lot better so far
float border = smoothstep(borderBlendCorrection - smoothing, borderBlendCorrection + smoothing, distance);
vec4 sdfColor = vec4(mix(fontColor1.xyz, fontColor0.xyz, border), alpha * fontColor0.w);
fragColor = sdfColor;

//lighting

//compose
gl_FragColor = fragColor;


}

