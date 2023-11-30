$input a_position, i_data0, i_data1, i_data2
$output v_texcoord7, v_texcoord6, v_color0, v_depth, v_texcoord5, v_texcoord4, v_texcoord3, v_texcoord2, v_bitangent, v_tangent, v_texcoord1, v_texcoord0, v_color4, v_color3

//includes
#include <common.sh>
#include "OnyxFunctions.sc"

//samplers
SAMPLER2D(s_heightTextureVert, 4);
uniform vec4 s_heightTextureVert_Res;
SAMPLER2D(s_VectorColors, 2);
uniform vec4 s_VectorColors_Res;
SAMPLER2D(s_VectorWidths, 3);
uniform vec4 s_VectorWidths_Res;
SAMPLER2D(s_DashCoords, 0);
uniform vec4 s_DashCoords_Res;
SAMPLER2D(s_DashSampler, 1);
uniform vec4 s_DashSampler_Res;

//cubeSamplers

//definitions
uniform vec4 u_tileSize;
uniform vec4 u_ScaleOffsetHeight;
uniform vec4 u_tileDistortion;
uniform vec4 u_MeshResolution;
uniform vec4 u_time;
uniform vec4 u_tileMin;
uniform vec4 u_tileMax;
uniform vec4 u_TileFragClip;
uniform vec4 u_screenDimensions;
uniform vec4 u_drawColor;
uniform vec4 u_vectorFade;
uniform vec4 u_nearFarPlane;

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

vec3 position = a_position.xyz;
vec4 u_endAngles = i_data0.xyzw;
vec4 u_p1p2 = i_data1.xyzw;
vec4 u_params = i_data2;
//main start
	 float cMaxLineWidth = 16.0; // analogous value in LineStyleAtlas.cpp
	 vec2 texParams = u_params.zw;
	 vec4 vecColor = texture2DLod(s_VectorColors, texParams, 0);
	 vec4 lineWidth = texture2DLod(s_VectorWidths, texParams, 0) * cMaxLineWidth;
	 float dashRow = texture2DLod(s_DashCoords, texParams, 0).r;
	 vec4 line_lengthTotal = vec4(u_params.xy,0,0);
	 float p2Cap = u_params.y;
	 vec2 p1 = u_p1p2.xy;
	 vec2 p2 = u_p1p2.zw;
	 vec2 tileP1 = mix(u_tileMin.xy, u_tileMax.xy, p1.xy);
	 vec2 tileP2 = mix(u_tileMin.xy, u_tileMax.xy, p2.xy);
	 vec2 tilePos = mix(p1, p2, position.y);
	 float tileZ = u_tileMin.z;
vec4 texcoords = vec4(position.xy,0,0);
float lineLengthSoFar = position.z;
position.xyz = vec3(0, position.y, 0); //repacking to match the previous pos/uv setup, but with all data just in pos
vec4 line_size = lineWidth; //override lineWidth here to use debug values for line dimensions
float widthExpansion = max(line_size.x, line_size.y) + line_size.z + line_size.w + 2.0;//x is solid width, y is dash width, zw are outline gap and width params, constant on the end is excess padding so we have room for AA
widthExpansion /= 2.0; //account for x uv being -1 to both side so need to cut width in half to get proper pixel count
float endA = 1.0 - texcoords.y; //uv.y is 0 for this one, so make it 1.0
float endB = texcoords.y; //uv.y is 1 for this one
	// compute height at the actual mesh
	float z = meshHeightAtPlanes(tilePos, u_MeshResolution.x);
	tileZ += z * u_tileSize.z;
float tileZ1 = u_tileMin.z;
float z1 = meshHeightAtPlanes(p1, u_MeshResolution.x);
tileZ1 += z1 * u_tileSize.z;
float tileZ2 = u_tileMin.z;
float z2 = meshHeightAtPlanes(p2, u_MeshResolution.x);
tileZ2 += z2 * u_tileSize.z;
vec4 wp1 = vec4(tileP1, tileZ1, 1.0);
vec4 wp2 = vec4(tileP2, tileZ2, 1.0);
vec4 screen1 = mul(u_proj, mul(u_view, wp1));
vec4 screen2 = mul(u_proj, mul(u_view, wp2));
float origW = mix(screen1.w, screen2.w, position.y);
screen1 /= screen1.w;
screen2 /= screen2.w;
screen1.xy = screen1.xy * 0.5 + 0.5;
screen2.xy = screen2.xy * 0.5 + 0.5;
screen1.xy /= u_viewTexel.xy;
screen2.xy /= u_viewTexel.xy;
vec4 line_endPointsScreen = vec4(screen1.xy, screen2.xy);
vec4 transformPosition = mix(screen1, screen2, position.y);
	 vec3 screenPos = transformPosition.xyz / transformPosition.w;
//float wPos = transformPosition.w / abs(transformPosition.w);
vec3 line_dir = screen2.xyz - screen1.xyz; //should this be screen or world distance?
float lineLen = length(line_dir);
line_dir /= lineLen;
vec3 line_side = vec3(normalize(vec2(-line_dir.y, line_dir.x)),0);
screenPos.xyz += line_side.xyz * texcoords.x * widthExpansion;
//overlap ends for the pixel miter
vec2 endExpansion = (-line_dir.xy * endA * widthExpansion + line_dir.xy * endB * widthExpansion);
screenPos.xy += endExpansion;
//move for miters
vec4 line_endFlags = vec4(u_endAngles.xz,0,0);
vec2 prevTP = mix(u_tileMin.xy, u_tileMax.xy, u_endAngles.xy);
vec2 nextTP = mix(u_tileMin.xy, u_tileMax.xy, u_endAngles.zw);
vec3 prevPos = vec3(prevTP, u_tileMin.z + meshHeightAtPlanes(u_endAngles.xy, u_MeshResolution.x) * u_tileSize.z);
vec3 nextPos = vec3(nextTP, u_tileMin.z + meshHeightAtPlanes(u_endAngles.zw, u_MeshResolution.x) * u_tileSize.z);
vec4 screenPrev = mul(u_proj, mul(u_view, vec4(prevPos,1.0)));
vec4 screenNext = mul(u_proj, mul(u_view, vec4(nextPos,1.0)));
screenPrev /= screenPrev.w;
screenNext /= screenNext.w;
screenPrev.xy = screenPrev.xy * 0.5 + 0.5;
screenNext.xy = screenNext.xy * 0.5 + 0.5;
screenPrev.xy /= u_viewTexel.xy;
screenNext.xy /= u_viewTexel.xy;
vec3 prevDir = normalize(screenPrev.xyz - screen1.xyz);
vec3 nextDir = normalize(screenNext.xyz - screen2.xyz);
if(u_endAngles.x < -9999.0) prevDir = line_dir;
if(u_endAngles.z < -9999.0) nextDir = -line_dir;
vec3 endADir = normalize(prevDir + line_dir);
vec3 endBDir = normalize(-line_dir + nextDir);
//pass over the xy vectors for the mitered ends
vec4 line_endAngles = vec4(endADir.xy * sign(length(u_endAngles.xy)), endBDir.xy * sign(length(u_endAngles.zw))); //angle goes to 0 if theres no connecting segment to indicate its an end point
float end = texcoords.y;
texcoords.y *= lineLen / widthExpansion;
texcoords.y += sign(end - 0.5) * length(endExpansion) / widthExpansion;
texcoords.z = lineLen / widthExpansion;

//lighting

//compose
	 float lineEdgeOffsetDist = lineWidth.x * 0.5;
	 vec4 lineCenter = vec4(1,1,1,1);// vec4(pixelLength * position.y + (lineWidth * 0.5f * position.z), 0.0, 0.0, 0.0);
	 vec4 screenPosition = vec4(screenPos.xyz, 1.0);// vec4(screenPos.xy + screenOffset, screenPos.z, 1.0);
	 vec4 linePosition = vec4(position.x, position.z, 0, lineWidth.x);
	 vec4 color = vec4(mix(vecColor.rgb, u_drawColor.rgb, u_drawColor.a), vecColor.a);
//	 screenPosition.w = sqrt(dot(u_screenDimensions.xy, u_screenDimensions.xy)) * lineEdgeOffsetDist * 0.5;
	 vec4 dashUV = vec4(position.xyz, 1.0);
	 float zBias = (1.0 / 2500.0) * max(1.0,widthExpansion*2.0) * pow(max(1.0, 1.0 + (u_nearFarPlane.y - u_nearFarPlane.x - 500.0) / 100.0),2.5);
	 screenPosition.z -= zBias / origW;
	vec4 tilePosition = vec4(tilePos, 0.0, 0.0);
vec4 glPos = vec4(screenPosition.xyz, 1.0);
glPos.xy *= u_viewTexel.xy;
glPos.xy = glPos.xy * 2.0 - 1.0;
glPos.xyz *= origW;
glPos.w = origW;
	gl_Position = glPos;

v_texcoord7 = tilePosition.xyzw;
v_texcoord6 = linePosition.xyzw;
v_color0 = color.xyzw;
v_depth = dashRow;
v_texcoord5 = screenPosition.xyzw;
v_texcoord4 = lineCenter.xyzw;
v_texcoord3 = texcoords.xyzw;
v_texcoord2 = line_endAngles.xyzw;
v_bitangent = line_dir.xyz;
v_tangent = line_side.xyz;
v_texcoord1 = line_size.xyzw;
v_texcoord0 = line_endPointsScreen.xyzw;
v_color4 = line_lengthTotal.xyzw;
v_color3 = line_endFlags.xyzw;

}

