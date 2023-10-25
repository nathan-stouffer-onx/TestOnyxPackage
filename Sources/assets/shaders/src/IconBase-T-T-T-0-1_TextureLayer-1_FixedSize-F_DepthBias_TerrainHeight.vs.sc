$input a_texcoord0, a_position
$output v_color1, v_color0, v_texcoord0, v_position

//includes
#include <../examples/common/common.sh>
#include "map3dFunctions.sc"

//samplers
SAMPLER2D(s_heightTextureVert, 0);
uniform vec4 s_heightTextureVert_Res;
SAMPLER2D(s_texture0, 1);
uniform vec4 s_texture0_Res;

//cubeSamplers

//definitions
uniform vec4 u_tileSize;
uniform vec4 u_ScaleOffsetHeight;
uniform vec4 u_tileDistortion;
uniform vec4 u_MeshResolution;
uniform vec4 u_depthModifier;
uniform vec4 u_ScaleOffsetTex0;
uniform vec4 u_tileMin;
uniform vec4 u_tileMax;
uniform vec4 u_scale;
uniform vec4 color1;
uniform vec4 color0;
uniform vec4 quatRot;
uniform vec4 pixelSize;
uniform vec4 iconPos;

//functions
// expects uv to be in tile coordinates
float heightAt(vec2 uv)
{
	vec2 scaledUV = u_ScaleOffsetHeight.zw * uv + u_ScaleOffsetHeight.xy;
	return texture2DLod(s_heightTextureVert, scaledUV, 0).r;
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

vec4 texcoord0 = a_texcoord0.xyzw;
vec3 position = a_position.xyz;
//main start
	// Custom aliases
	vec3 midpoint = iconPos.xyz;
	float uniformScale = u_scale.x;
	vec2 scale = uniformScale * pixelSize.xy;
	vec2 tilePos = midpoint.xy;
	float tileZ = 0.f; // Height value
	// compute height at the actual mesh
	float z = meshHeightAtPlanes(tilePos, u_MeshResolution.x);
	tileZ += z * u_tileSize.z;
	// Midpoint is in canonical tile coordinates, including height
	midpoint.z = tileZ;
	midpoint.z = (midpoint.z - cHeightRange[0]) / (cHeightRange[1] - cHeightRange[0]);
	midpoint = mix(u_tileMin.xyz, u_tileMax.xyz, midpoint.xyz);
	// Keep sized fixed regardless of cam distance
	position = length(mul(u_view, vec4(midpoint.xyz, 1.0)).xyz) * position;
	// Scale, rotate, translate position
	position = vec3(scale.xy, 1.0) * position;
	position = rotate(quatRot, position) + midpoint + color0.xyz + color1.xyz;

//lighting

//compose
	gl_Position = mul(u_proj, mul(u_view, vec4(position.xyz, 1.0)));
	//Bias depth towards cam to draw vertex over occlusions
	gl_Position.z -= gl_Position.w * u_depthModifier.x;

v_color1 = color1.xyzw;
v_color0 = color0.xyzw;
v_texcoord0 = texcoord0.xyzw;
v_position = position.xyz;

}

