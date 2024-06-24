$input v_normal, v_texcoord7, v_texcoord6, v_texcoord5, v_texcoord4, v_texcoord3, v_texcoord2, v_texcoord1, v_texcoord0, v_color4
//includes
#include <common.sh>
#include "layers.sc"
#include "derivatives.sc"
#include "terrain.sc"

//samplers
SAMPLER2D(s_heightTexture, 0);
uniform vec4 s_heightTexture_Res;
SAMPLER2D(s_sunShadowDepth, 1);
uniform vec4 s_sunShadowDepth_Res;
SAMPLER2D(s_texture0, 2);
uniform vec4 s_texture0_Res;
SAMPLER2D(s_texture1, 3);
uniform vec4 s_texture1_Res;
SAMPLER2D(s_texture2, 4);
uniform vec4 s_texture2_Res;

//cubeSamplers

//definitions
uniform vec4 u_tileSize;
uniform vec4 u_tileDistortion;
uniform vec4 u_ScaleOffsetHeight;
uniform vec4 u_lightStrengthPow;
uniform vec4 u_SunTimeData;
uniform vec4 u_SunMinStrength;
uniform vec4 u_SunAmbient;
uniform vec4 u_sunTileMin;
uniform vec4 u_sunTileMax;
uniform vec4 u_sunShadowTileMin;
uniform vec4 u_sunShadowTileMax;
uniform vec4 u_sunShadowFarPlane;
uniform vec4 u_sunShadowBias;
uniform vec4 u_sunShadowStrength;
uniform mat4 u_sunShadowView;
uniform mat4 u_sunShadowProj;
uniform vec4 u_sunShadowVSMParams;
uniform vec4 u_CascadeDebug;
uniform vec4 u_FogTransition;
uniform vec4 u_FogColor;
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


vec3 fog(vec3 underneath, vec4 color, vec2 transition, float d)
{
	float strength = smoothstep(transition.x, transition.y, d);
	return mix(underneath, color.rgb, strength * color.a);
}
float linstep(float low, float high, float v)
{
	return clamp((v-low)/(high-low), 0.0, 1.0);
}

void main()
{

vec3 normal = v_normal.xyz;
vec4 worldPosition = v_texcoord7.xyzw;
vec4 depth = v_texcoord6.xyzw;
vec4 texcoords = v_texcoord5.xyzw;
vec4 fogDist = v_texcoord4.xyzw;
vec4 sunDir = v_texcoord3.xyzw;
vec4 sunUV = v_texcoord2.xyzw;
vec4 sunShadowUV = v_texcoord1.xyzw;
vec4 tileDistortion = v_texcoord0.xyzw;
vec4 scaleOffsetHeight = v_color4.xyzw;
//main start
normal.xyz = normalAt(texcoords.xy, u_tileSize.x, tileDistortion.xy, s_heightTexture, scaleOffsetHeight, s_heightTexture_Res.z);
vec4 fragColor = u_BackgroundColor;
fragColor = BlendTextures(fragColor, texcoords.xy);
	float hillshade = calcSunlightTangent(sunUV.xy, vec3(normal.x, normal.y, normal.z), sunDir.xyz);//dot(normalize(normal.xyz * 2.0 - 1.0), normalize(sunDir.xyz));
	float nightShadeAmount = 0.5;
	float nightShade = dot(normal.xyz * 2.0 - 1.0, normalize(vec3(0.2, 0.2, 1.0)));
	float nightShadeAmt = pow(1.0 - max(0, hillshade), 3.0); //only nightshade where its dark
	vec3 hillshadeColor = vec3(hillshade, hillshade, hillshade);
	vec3 nightshadeColor = vec3(0.75, 0.79, 1.0) * nightShade;
	fragColor = mix(fragColor, vec4(mix(hillshadeColor, nightshadeColor, nightShadeAmount * u_SunAmbient.x),1.0), u_SunMinStrength.x);
	float distToSun = sunShadowUV.z - u_sunShadowBias.x;
	vec2 projectedUV = sunShadowUV.xy;
	projectedUV  /= sunShadowUV.w;
	projectedUV  = projectedUV  * 0.5 + 0.5;
	vec2 moments = texture2D(s_sunShadowDepth, projectedUV.xy).xy;
	float p = float(distToSun <= moments.x);//step(distToSun, moments.x);
	float variance = max(moments.y - moments.x * moments.x, u_sunShadowVSMParams.x);
	float vsmD = distToSun - moments.x;
	float pMax = linstep(u_sunShadowVSMParams.y, 1.0, variance / (variance + vsmD*vsmD));
	float shadow = min(max(p, pMax), 1.0);
	fragColor.xyz = mix(fragColor.xyz, vec3(shadow, shadow, shadow), u_sunShadowStrength.x);
if(u_CascadeDebug.x > 0.5) fragColor.xyz = texture2D(s_sunShadowDepth, projectedUV.xy).xyz;
//fragColor.xyz = vec3(texture2D(s_sunShadowDepth, projectedUV.xy).x);

//lighting
fragColor.rgb = fog(fragColor.rgb, u_FogColor, u_FogTransition.xy, fogDist.x);

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
vec3 eyeDir = normalize(-worldPosition.xyz);
float camDown = max(0.0, dot( eyeDir, vec3(0,0,1)));
vec3 lightDir = vec3(eyeDir.y * 0.5,-eyeDir.x * 0.5, max(0.7 * (1.0 - camDown), 0.2));
vec3 norm = normal.xyz;
//norm = normalize(vec3(norm.x, norm.y, norm.z * 0.25 + norm.z * 0.75 * max(0, 1.0 - worldPosition.z / 2000.0))); //accentuate normals for shading as we zoom out
float nDotL = dot(norm.xyz, lightDir);
vec3 reflection = normalize(2.0 * norm.xyz * nDotL - lightDir);
float rDotV = min(max(0, dot(reflection, eyeDir)), 1.0);
float strength = u_lightStrengthPow.x;
float power = u_lightStrengthPow.y;
float vertDim = 1.0 - max(0.0, dot(vec3(0,0,1), norm.xyz)); //reduce the strenth as things approach straight vertical
vertDim *= 2.0;
float heightFade = max(0.0, min(1.0, (-worldPosition.z - 5.0) / 100.0)); //fade out the shading as we get close to the terrain and make sure its off completely when closer
gl_FragData[0].xyz += pow(rDotV, power) * strength * (vertDim * 0.9 + 0.1) * heightFade;
//the following lines are helpful when debugging to let us see normal/height/raw height easily in app
//vec3 res = gl_FragData[0].xyz;
//if(u_lightStrengthPow.z > 0.5) res = norm.xyz * 0.5 + 0.5;
//if(u_lightStrengthPow.z > 1.0) { float z = worldPosition.z - u_tileMin.z; res = vec3(z,z,z);}
//if(u_lightStrengthPow.z > 1.5) res = texture2D(s_heightTexture, texcoords.xy * scaleOffsetHeight.z + scaleOffsetHeight.xy).xyz;
//gl_FragData[0].xyz = res;


}

