$input a_texcoord7
$output v_texcoord4, v_bitangent, v_texcoord7, v_texcoord6, v_depth, v_texcoord5

//includes
#include <common.sh>
#include "OnyxFunctions.sc"

//samplers
SAMPLER2D(s_heightTexture, 0);
uniform vec4 s_heightTexture_Res;
SAMPLER2D(s_vectorColors, 2);
uniform vec4 s_vectorColors_Res;
SAMPLER2D(s_vectorPatterns, 3);
uniform vec4 s_vectorPatterns_Res;
SAMPLER2D(s_patterns, 1);
uniform vec4 s_patterns_Res;

//cubeSamplers

//definitions
uniform vec4 u_tileSize;
uniform vec4 u_ScaleOffsetHeight;
uniform vec4 u_tileDistortion;
uniform vec4 u_MeshResolution;
uniform vec4 u_TileFillData;
uniform vec4 u_nearFarPlane;
uniform vec4 u_screenResolution;
uniform vec4 u_eyePos;
uniform vec4 u_camRight;
uniform vec4 u_camForward;
uniform vec4 u_camUp;
uniform vec4 u_time;
uniform vec4 u_tileMin;
uniform vec4 u_TileFillOpacityTransition;
uniform vec4 u_vectorFade;
uniform vec4 u_tileMax;
uniform vec4 u_TileVertClip;
uniform vec4 u_TileFragClip;

//functions
// expects uv to be in tile coordinates
float heightAt(vec2 uv)
{
	vec2 scaledUV = u_ScaleOffsetHeight.zw * uv + u_ScaleOffsetHeight.xy;
	return texture2DLod(s_heightTexture, scaledUV, 0).r;
}
// expects uv to be in tile coordinates
float distortedHeightAt(vec2 uv)
{
	float z = heightAt(uv);
	float distortion = mix(u_tileDistortion.x, u_tileDistortion.y, uv.y);
	return z * distortion;
}
// computes the height of the triangle mesh at the specified uv coordinate
//
//      v0 ----------- v1
//       |           * |
//       |   *p   *    |
//       |     *       |
//       |  *          |
//      v2 ----------- v3
//
// let the above diagram represent the mesh quad that contains the point p. the function
// first computes which three vertices make up the triangle. v1 and v2 are guaranteed to
// be part of the triangle. and then we just compute which of v0 and v3 is closer to p to
// determine the final point. then we compute the heights at the triangle vertices and
// use barycentric interpolation to compute the final height value
float meshHeightAtBarycentric(vec2 uv, float meshRes)
{
	vec3 p = vec3(uv, 0.0);
	// compute the top left corner of the quad that contains p -- clamp to [eps, 1 - eps] to avoid problems at 0 and 1
	float eps = 1.0 / 256.0;
	vec2 v0 = floor(meshRes * clamp(uv, vec2(eps, eps), vec2(1.0 - eps, 1.0 - eps)));
	vec3 a = vec3(v0.x + 1.0, v0.y, 0.0) / meshRes; // v1
	vec3 b = vec3(v0.x, v0.y + 1.0, 0.0) / meshRes; // v2
	vec3 c = closer(p, vec3(b.x, a.y, 0.0), vec3(a.x, b.y, 0.0)); // choose between v0 and v3
	// compute heights at the three triangle vertices
	vec3 heights = vec3(distortedHeightAt(a.xy), distortedHeightAt(b.xy), distortedHeightAt(c.xy));
	// compute subtriangle A
	Triangle triA;
	triA.p0 = p;
	triA.p1 = b;
	triA.p2 = c;
	// compute subtriangle B
	Triangle triB;
	triB.p0 = p;
	triB.p1 = a;
	triB.p2 = c;
	// compute subtriangle C
	Triangle triC;
	triC.p0 = p;
	triC.p1 = a;
	triC.p2 = b;
	// compute the barycentric coordinates of the point p
	vec3 barycentric = vec3(area(triA), area(triB), area(triC));
	float area = barycentric.x + barycentric.y + barycentric.z;  // the full triangle area is the sum of the three subtriangles
	barycentric /= area;
	// return the interpolated point
	return dot(barycentric, heights);
}
// computes the height of the triangle mesh at the specified uv coordinate
//
//      v0 ----------- v1
//       |           * |
//       |   *p   *    |
//       |     *       |
//       |  *          |
//      v2 ----------- v3
//
// let the above diagram represent the mesh quad that contains the point p. the function
// first computes which three vertices make up the triangle. v1 and v2 are guaranteed to
// be part of the triangle. and then we just compute which of v0 and v3 is closer to p to
// determine the final point. then we compute the plane of the triangle and evaluate the
// function to determine the height
float meshHeightAtPlanes(vec2 uv, float meshRes)
{
	// compute the top left corner of the quad that contains p -- clamp to [eps, 1 - eps] to avoid problems at 0 and 1
	float eps = 1.0 / 256.0;
	vec2 v0 = floor(meshRes * clamp(uv, vec2(eps, eps), vec2(1.0 - eps, 1.0 - eps)));
	vec2 a = vec2(v0.x + 1.0, v0.y) / meshRes; // v1
	vec2 b = vec2(v0.x, v0.y + 1.0) / meshRes; // v2
	vec2 c = closer(uv, vec2(b.x, a.y), vec2(a.x, b.y)); // choose between v0 and v3
	// compute the world coordinates of the vertex 
	vec2 vertex = mix(u_tileMin.xy, u_tileMax.xy, uv);
	// compute the world coordinates of the three points that define the plane of this triangle
	vec3 p = vec3(mix(u_tileMin.xy, u_tileMax.xy, a.xy), distortedHeightAt(a.xy));
	vec3 q = vec3(mix(u_tileMin.xy, u_tileMax.xy, b.xy), distortedHeightAt(b.xy));
	vec3 r = vec3(mix(u_tileMin.xy, u_tileMax.xy, c.xy), distortedHeightAt(c.xy));
	// compute plane normal
	vec3 normal = cross(q - p, r - p);
	float z = -(dot(normal.xy, vertex.xy) - dot(normal, p)) / normal.z;
	return z;
}

void main()
{

vec4 tilePosition_style = a_texcoord7.xyzw;
//main start
	vec2 tilePos = vec2(tilePosition_style.xy);
	float tileZ = u_tileMin.z;
	vec3 worldPosition = vec3(mix(u_tileMin.xy, u_tileMax.xy, tilePos), u_tileMin.z);
	 vec4 vecPattern = texture2DLod(s_vectorPatterns, tilePosition_style.zw, 0);
	 vecPattern.zw = vecPattern.zw * s_patterns_Res.xy;
	 vec4 vecColor = texture2DLod(s_vectorColors, tilePosition_style.zw, 0);
mat4 viewMat = u_view;
	// compute height at the actual mesh
	float z = meshHeightAtPlanes(tilePos, u_MeshResolution.x);
	tileZ += z * u_tileSize.z;

//lighting

//compose
	float distFade = 1.0 - smoothstep(u_TileFillOpacityTransition.x, u_TileFillOpacityTransition.y, length(worldPosition.xyz) / u_nearFarPlane.y);
	vec4 projected = mul(u_proj, mul(viewMat, vec4(worldPosition.xy, tileZ, 1.0)));
	projected.z -= (projected.w * 0.001);
	vec4 depth = projected;
	vec4 tilePosition = vec4(tilePos, 0.0, 0.0);
	float inX = inRange(tilePosition.x, u_TileVertClip.x, u_TileVertClip.z);
	float inY = inRange(tilePosition.y, u_TileVertClip.y, u_TileVertClip.w);
	if (inX * inY == 0.0) { projected = vec4(0, 0, 0, 0); }
	gl_Position = projected;

v_texcoord4 = vecPattern.xyzw;
v_bitangent = worldPosition.xyz;
v_texcoord7 = tilePosition.xyzw;
v_texcoord6 = depth.xyzw;
v_depth = distFade;
v_texcoord5 = vecColor.xyzw;

}

