$input v_normal, v_texcoord7, v_texcoord6, v_texcoord5, v_texcoord4, v_texcoord3, v_texcoord2
//includes
#include <common.sh>
#include "layers.sc"
#include "derivatives.sc"
#include "terrain.sc"

//samplers
SAMPLER2D(s_heightTexture, 1);
uniform vec4 s_heightTexture_Res;
SAMPLER2D(s_SlopeAspectShadeTexture, 0);
uniform vec4 s_SlopeAspectShadeTexture_Res;

//cubeSamplers

//definitions
uniform vec4 u_tileSize;
uniform vec4 u_tileDistortion;
uniform vec4 u_ScaleOffsetHeight;
uniform vec4 u_lightStrengthPow;
uniform vec4 u_SlopeAspectMaxNormalZ;
uniform vec4 u_HillshadeLightDir;
uniform vec4 u_HillshadeAlbedo;
uniform vec4 u_HillshadeParams;
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
vec3 hillshade(vec3 inputColor, vec3 normal, vec3 lightDir, vec4 albedo, float ambientIntensity, float exaggeration)
{
	normal = normalize(vec3(exaggeration * normal.xy, normal.z));
	float strength = 0.5 * (1.0 + dot(normal, -lightDir));
	float intensity = ambientIntensity + (1.0 - ambientIntensity) * strength;
	return mix(inputColor, intensity * albedo.rgb, albedo.a);
}
vec3 slopeAspectShade(vec3 inputColor, vec3 normal)
{
	float TWO_PI = PI_CONSTS.y;
	vec4 aspectTexel = texture2D(s_SlopeAspectShadeTexture, vec2(calcSlopeDir(normal.xyz) / TWO_PI, 0.0));
	float strength = aspectTexel.a * float(abs(normal.z) <= u_SlopeAspectMaxNormalZ.x);
	return mix(inputColor, aspectTexel.rgb, strength);
}

void main()
{

vec3 normal = v_normal.xyz;
vec4 worldPosition = v_texcoord7.xyzw;
vec4 depth = v_texcoord6.xyzw;
vec4 texcoords = v_texcoord5.xyzw;
vec4 fogDist = v_texcoord4.xyzw;
vec4 tileDistortion = v_texcoord3.xyzw;
vec4 scaleOffsetHeight = v_texcoord2.xyzw;
//main start
normal.xyz = normalAt(texcoords.xy, u_tileSize.x, tileDistortion.xy, s_heightTexture, scaleOffsetHeight, s_heightTexture_Res.z);
vec4 fragColor = u_BackgroundColor;
fragColor.rgb = hillshade(fragColor.rgb, normal.xyz, u_HillshadeLightDir.xyz, u_HillshadeAlbedo.rgba, u_HillshadeParams.x, u_HillshadeParams.y);
fragColor.rgb = slopeAspectShade(fragColor.rgb, normal.xyz);

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

