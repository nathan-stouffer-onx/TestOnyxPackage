$input v_normal, v_texcoord7, v_texcoord6, v_texcoord5, v_texcoord4, v_texcoord3, v_texcoord2
//includes
#include <common.sh>
#include "layers.sc"
#include "derivatives.sc"
#include "terrain.sc"

//samplers
SAMPLER2D(s_heightTexture, 3);
uniform vec4 s_heightTexture_Res;
SAMPLER2D(s_SlopeAngleShadeTexture, 1);
uniform vec4 s_SlopeAngleShadeTexture_Res;
SAMPLER2D(s_ElevationShadeTexture, 0);
uniform vec4 s_ElevationShadeTexture_Res;
SAMPLER2D(s_texture0, 4);
uniform vec4 s_texture0_Res;
SAMPLER2D(s_texture1, 5);
uniform vec4 s_texture1_Res;

//cubeSamplers
SAMPLERCUBE(s_cubeDepth0, 2);
uniform vec4 s_cubeDepth0_Res;

//definitions
uniform vec4 u_viewshedTint0;
uniform vec4 u_viewshedRingTint0;
uniform vec4 u_viewshedRange0;
uniform vec4 u_viewshedPos0;
uniform vec4 u_viewshedFarPlane0;
uniform vec4 u_viewshedInverted0;
uniform vec4 u_viewshedBias0;
uniform vec4 u_tileSize;
uniform vec4 u_tileDistortion;
uniform vec4 u_ScaleOffsetHeight;
uniform vec4 u_lightStrengthPow;
uniform vec4 u_ElevationExtents;
uniform vec4 u_FogTransition;
uniform vec4 u_FogColor;
uniform vec4 u_ScaleOffsetTex0;
uniform vec4 u_OpacityTex0;
uniform vec4 u_ScaleOffsetTex1;
uniform vec4 u_OpacityTex1;
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
	return color;
}


vec3 fog(vec3 underneath, vec4 color, vec2 transition, float d)
{
	float strength = smoothstep(transition.x, transition.y, d);
	return mix(underneath, color.rgb, strength * color.a);
}
float cubemapRayTo(vec3 lightSource, vec3 pixelPos)
{
	vec3 rayDir = pixelPos - lightSource;
	// multiply y coordinate of cube map index by -1 because south is +y (matches slippy map)
	rayDir.y *= -1.0;
	float dist = textureCube(s_cubeDepth0, rayDir).x * u_viewshedFarPlane0.x;
	return dist;
}
vec3 calcViewshedRings(vec3 terrainColor, vec3 viewshedPos, vec3 pixelPos, float range, vec4 tint)
{
	float dist = length(pixelPos - viewshedPos);
	float inRange = float(dist < range);
	// compute level sets of the distance to the eye (concentric circles on the terrain)
	float lineStrength = levelSets(dist, range / 4.0, 0.0, 0.0, range, 1.5);
	// add a couple small circles close to the eye
	lineStrength += levelSets(dist, 0.02, 0.0, 0.0, 0.08, 1.5);
	vec4 lineColor = vec4(mix(terrainColor, tint.rgb, tint.a), tint.a);
	return mix(terrainColor, lineColor.rgb, lineStrength);
}
vec3 calcViewshed(vec3 terrainColor, vec3 viewshedPos, vec3 pixelPos, float inverted, float range, vec4 tint)
{
	float dist = length(pixelPos - viewshedPos);
	float biasedDist = dist - u_viewshedBias0.x; // simple depth bias
	float cubemapDist = cubemapRayTo(viewshedPos, pixelPos);
	float inRange = float(dist < range);
	float isVisible = abs(inverted - float(biasedDist < cubemapDist));
	float grey = (terrainColor.r + terrainColor.g + terrainColor.b) / 3.0;
	vec3 color = grey * tint.rgb;
	// compute the strength to apply to the the viewshed color
	float strength = inRange * isVisible * tint.a;
	return mix(terrainColor, color, strength);
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
fragColor = BlendTextures(fragColor, texcoords.xy);
float elevationIndex = lerpInv(u_ElevationExtents.x, u_ElevationExtents.y, worldPosition.w) * s_ElevationShadeTexture_Res.x * s_ElevationShadeTexture_Res.y;
float i = mod(elevationIndex, s_ElevationShadeTexture_Res.x);
float j = floor(elevationIndex / s_ElevationShadeTexture_Res.y);
vec4 elevationTexel = texture2D(s_ElevationShadeTexture, vec2(i, j) / s_ElevationShadeTexture_Res.xy);
fragColor.rgb = mix(fragColor.rgb, elevationTexel.rgb, elevationTexel.a);
float PI_HALVES = PI_CONSTS.z;
vec4 angleTexel = texture2D(s_SlopeAngleShadeTexture, vec2(calcSlopeAngle(normal.xyz) / PI_HALVES, 0.0));
fragColor.rgb = mix(fragColor.rgb, angleTexel.rgb, angleTexel.a);

//lighting
fragColor.rgb = calcViewshed(fragColor.rgb, u_viewshedPos0.xyz, worldPosition.xyz, u_viewshedInverted0.x, u_viewshedRange0.x, u_viewshedTint0);
fragColor.rgb = calcViewshedRings(fragColor.rgb, u_viewshedPos0.xyz, worldPosition.xyz, u_viewshedRange0.x, u_viewshedRingTint0);
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

