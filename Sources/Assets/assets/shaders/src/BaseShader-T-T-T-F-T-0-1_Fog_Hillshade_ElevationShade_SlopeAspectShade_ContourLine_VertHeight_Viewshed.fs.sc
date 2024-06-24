$input v_normal, v_texcoord7, v_texcoord6, v_texcoord5, v_texcoord4, v_texcoord3, v_texcoord2
//includes
#include <common.sh>
#include "layers.sc"
#include "derivatives.sc"
#include "terrain.sc"

//samplers
SAMPLER2D(s_heightTexture, 3);
uniform vec4 s_heightTexture_Res;
SAMPLER2D(s_SlopeAspectShadeTexture, 1);
uniform vec4 s_SlopeAspectShadeTexture_Res;
SAMPLER2D(s_ElevationShadeTexture, 0);
uniform vec4 s_ElevationShadeTexture_Res;

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
uniform vec4 u_ContourParams0;
uniform vec4 u_ContourColor0;
uniform vec4 u_ContourParams1;
uniform vec4 u_ContourColor1;
uniform vec4 u_ContourFade;
uniform vec4 u_MaxNormalZ;
uniform vec4 u_ElevationExtents;
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
	float strength = aspectTexel.a * float(abs(normal.z) <= u_MaxNormalZ.x);
	return mix(inputColor, aspectTexel.rgb, strength);
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
fragColor.rgb = hillshade(fragColor.rgb, normal.xyz, u_HillshadeLightDir.xyz, u_HillshadeAlbedo.rgba, u_HillshadeParams.x, u_HillshadeParams.y);
fragColor.rgb = slopeAspectShade(fragColor.rgb, normal.xyz);
float elevationIndex = lerpInv(u_ElevationExtents.x, u_ElevationExtents.y, worldPosition.w) * s_ElevationShadeTexture_Res.x * s_ElevationShadeTexture_Res.y;
float i = mod(elevationIndex, s_ElevationShadeTexture_Res.x);
float j = floor(elevationIndex / s_ElevationShadeTexture_Res.y);
vec4 elevationTexel = texture2D(s_ElevationShadeTexture, vec2(i, j) / s_ElevationShadeTexture_Res.xy);
fragColor.rgb = mix(fragColor.rgb, elevationTexel.rgb, elevationTexel.a);
float contourFade = 1.0 - smoothstep(u_ContourFade.x, u_ContourFade.y, length(worldPosition.xyz));
fragColor.rgb = calcContour(fragColor.rgb, u_ContourColor0, u_ContourParams0, worldPosition.w, contourFade, u_NearFarFocus.z, length(worldPosition));
fragColor.rgb = calcContour(fragColor.rgb, u_ContourColor1, u_ContourParams1, worldPosition.w, contourFade, u_NearFarFocus.z, length(worldPosition));

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

