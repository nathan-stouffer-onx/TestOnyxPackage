$input v_normal, v_texcoord7, v_texcoord6, v_texcoord5, v_texcoord4, v_texcoord3, v_texcoord2, v_texcoord1, v_texcoord0, v_color4
//includes
#include <common.sh>
#include "layers.sc"
#include "derivatives.sc"
#include "terrain.sc"

//samplers
SAMPLER2D(s_heightTexture, 2);
uniform vec4 s_heightTexture_Res;
SAMPLER2D(s_sunShadowDepth, 3);
uniform vec4 s_sunShadowDepth_Res;
SAMPLER2D(s_SlopeAspectShadeTexture, 1);
uniform vec4 s_SlopeAspectShadeTexture_Res;
SAMPLER2D(s_ElevationShadeTexture, 0);
uniform vec4 s_ElevationShadeTexture_Res;

//cubeSamplers

//definitions
uniform vec4 u_tileSize;
uniform vec4 u_tileDistortion;
uniform vec4 u_ScaleOffsetHeight;
uniform vec4 u_lightStrengthPow;
uniform vec4 u_ContourParams0;
uniform vec4 u_ContourColor0;
uniform vec4 u_ContourParams1;
uniform vec4 u_ContourColor1;
uniform vec4 u_ContourFade;
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
uniform vec4 u_MaxNormalZ;
uniform vec4 u_ElevationExtents;
uniform vec4 u_FogTransition;
uniform vec4 u_FogColor;
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
vec3 fog(vec3 underneath, vec4 color, vec2 transition, float d)
{
	float strength = smoothstep(transition.x, transition.y, d);
	return mix(underneath, color.rgb, strength * color.a);
}
vec3 slopeAspectShade(vec3 inputColor, vec3 normal)
{
	float TWO_PI = PI_CONSTS.y;
	vec4 aspectTexel = texture2D(s_SlopeAspectShadeTexture, vec2(calcSlopeDir(normal.xyz) / TWO_PI, 0.0));
	float strength = aspectTexel.a * float(abs(normal.z) <= u_MaxNormalZ.x);
	return mix(inputColor, aspectTexel.rgb, strength);
}
float linstep(float low, float high, float v)
{
	return clamp((v-low)/(high-low), 0.0, 1.0);
}
// def unpacks to (period, min, max, width)
vec3 calcContour(vec3 baseColor, vec4 color, vec4 def, float height, float opacity, float focus, float dist)
{
	float width = def.w * min(1.0, max(focus, 1.5) / dist); // thin lines beyond the focus distance (as long as focus is larger than 1.5 km)
	vec3 blended = mix(baseColor, color.rgb, color.a * min(width, 1.0)); // compute line color (reduce opacity if line width is smaller than 1)
	width = max(1.5, width); // minimum width for anti-aliasing
	float t = levelSets(height, def.x, 0.0, def.y, def.z, width);
	t = clamp(pow(t + 0.5, 3.0) - 0.5, 0.0, 1.0); // remove edge haze
	return mix(baseColor, blended, t * opacity);
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
fragColor.rgb = slopeAspectShade(fragColor.rgb, normal.xyz);
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
float elevationIndex = lerpInv(u_ElevationExtents.x, u_ElevationExtents.y, worldPosition.w) * s_ElevationShadeTexture_Res.x * s_ElevationShadeTexture_Res.y;
float i = mod(elevationIndex, s_ElevationShadeTexture_Res.x);
float j = floor(elevationIndex / s_ElevationShadeTexture_Res.y);
vec4 elevationTexel = texture2D(s_ElevationShadeTexture, vec2(i, j) / s_ElevationShadeTexture_Res.xy);
fragColor.rgb = mix(fragColor.rgb, elevationTexel.rgb, elevationTexel.a);
float contourFade = 1.0 - smoothstep(u_ContourFade.x, u_ContourFade.y, length(worldPosition.xyz));
fragColor.rgb = calcContour(fragColor.rgb, u_ContourColor0, u_ContourParams0, worldPosition.w, contourFade, u_NearFarFocus.z, length(worldPosition));
fragColor.rgb = calcContour(fragColor.rgb, u_ContourColor1, u_ContourParams1, worldPosition.w, contourFade, u_NearFarFocus.z, length(worldPosition));

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

