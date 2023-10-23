$input i_data2, i_data3, i_data4, i_data1, a_position
$output v_texcoord5, v_texcoord6, v_color0, v_texcoord7

//includes
#include <common.sh>
#include "map3dFunctions.sc"

//samplers
SAMPLER2D(s_heightTextureVert, 1);
uniform vec4 s_heightTextureVert_Res;
SAMPLER2D(s_DashSampler, 0);
uniform vec4 s_DashSampler_Res;

//cubeSamplers

//definitions
uniform vec4 u_tileSize;
uniform vec4 u_ScaleOffsetHeight;
uniform vec4 u_tileDistortion;
uniform vec4 u_MeshResolution;
uniform vec4 u_dashUV;
uniform vec4 u_nearFarPlane;
uniform vec4 u_eyePos;
uniform vec4 u_camRight;
uniform vec4 u_camForward;
uniform vec4 u_camUp;
uniform vec4 u_time;
uniform vec4 u_tileMin;
uniform vec4 u_tileMax;
uniform vec4 u_screenDimensions;
uniform vec4 u_drawColor;

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

vec4 u_color = i_data2;
vec4 u_length = i_data3.xyzw;
vec4 u_origin = i_data4.xyzw;
vec4 u_endCaps = i_data1.xyzw;
vec3 position = a_position.xyz;
//main start
	 float lineWidth = u_origin.w;
	 float lineLength = u_length.w;
	 float p1Cap = u_endCaps.x;
	 float p2Cap = u_endCaps.y;
	 vec3 p1 = u_origin.xyz;
	 vec3 p2 = p1 + ((u_length.xyz) * lineLength);
	 vec2 tileP1 = mix(u_tileMin.xy, u_tileMax.xy, clamp(p1.xy, 0, 1));
	 vec2 tileP2 = mix(u_tileMin.xy, u_tileMax.xy, clamp(p2.xy, 0, 1));
	 vec2 tilePos = mix(tileP1, tileP2, position.y);
	 float tileZ = u_tileMin.z;
	// compute height at the actual mesh
	float z = meshHeightAtPlanes(tilePos, u_MeshResolution.x);
	tileZ += z * u_tileSize.z;
	 vec4 screenP1 = mul(u_proj, mul(u_view, vec4(tileP1, tileZ, 1.0)));
	 vec4 screenP2 = mul(u_proj, mul(u_view, vec4(tileP2, tileZ, 1.0)));
	 float wPos = mix(screenP1.w, screenP2.w, position.y);
	 wPos = wPos / abs(wPos);
	 screenP1 /= screenP1.w;
	 screenP2 /= screenP2.w;
	 vec2 screenDir = normalize(screenP2.xy - screenP1.xy);
	 vec4 transformPosition = mix(screenP1, screenP2, position.y);
	 vec3 screenPos = transformPosition.xyz;
	 float pixelLength = length((screenP2.xy - screenP1.xy) / u_screenDimensions.xy);

//lighting

//compose
	 float lineEdgeOffsetDist = lineWidth * 0.5;
	 float capMultiplier = (p1Cap * (1.0 - position.y)) + (p2Cap * position.y);
	 vec2 capOffset = capMultiplier * (lineEdgeOffsetDist * position.z) * (u_screenDimensions.xy * screenDir.xy);
	 float screenLength = (lineLength * position.y);
	 vec2 screenOffset = screenLength * screenDir;
	 vec2 pixelOffset = (u_screenDimensions.zw * screenDir.xy);
	 vec4 lineCenter = vec4(pixelLength * position.y + (lineWidth * 0.5f * position.z), 0, 0, 0);
	 screenOffset.xy = (screenOffset * u_screenDimensions.xy) + capOffset;
	 vec4 screenPosition = vec4(screenPos.xy + screenOffset, screenPos.z, 1);
	 vec2 right = u_screenDimensions.xy * vec2(-screenDir.y, screenDir.x);
	 screenPosition.xy += position.x * right * lineEdgeOffsetDist;
	 vec4 linePosition = vec4(position.x, position.z, 0, lineWidth);
	 vec4 color = vec4(mix(u_color.rgb, u_drawColor.rgb, u_drawColor.a), u_color.a);
	 screenPosition.w = sqrt(dot(u_screenDimensions.xy, u_screenDimensions.xy)) * lineEdgeOffsetDist * 0.5;
	 vec4 dashUV = vec4(position.xyz, 1);
	 screenPosition.z -= (1.0 / 128.0);
	gl_Position = vec4(screenPosition.xyz * wPos, wPos);

v_texcoord5 = lineCenter.xyzw;
v_texcoord6 = screenPosition.xyzw;
v_color0 = color.xyzw;
v_texcoord7 = linePosition.xyzw;

}

