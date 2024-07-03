$input a_position
$output v_texcoord7, v_texcoord6, v_color0, v_depth, v_texcoord5, v_texcoord4, v_texcoord3, v_texcoord2, v_bitangent, v_tangent, v_texcoord1, v_texcoord0, v_color4, v_color3, v_color2

//includes
#include <common.sh>
#include "layers.sc"
#include "terrain.sc"

//samplers
SAMPLER2D(s_heightTexture, 4);
uniform vec4 s_heightTexture_Res;
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
uniform vec4 u_p1p2;
uniform vec4 u_PrevNext;
uniform vec4 u_screenDimensions;
uniform vec4 u_params;
uniform vec4 u_vectorFade;
uniform vec4 u_TileLineOpacityTransition;
uniform vec4 u_NearFarFocus;

//functions
vec4 toScreenCoords(vec4 projected, vec2 viewTexel)
{
	vec4 screen = projected / projected.w;
	screen.xy = screen.xy * 0.5 + 0.5;
	screen.xy /= viewTexel;
	return screen;
}
// we assume u and v to be normal vectors
vec2 jointBisector(vec2 u, vec2 v)
{
	float theta = atan2(u.y, u.x);
	float phi = atan2(v.y, v.x);
	float alpha = 0.5 * (theta + phi);
	return vec2(cos(alpha), sin(alpha));
}

void main()
{

vec3 position = a_position.xyz;
//main start
float cMaxLineWidth = 64.0; // analogous value in LineStyleAtlas.cpp
vec2 texParams = u_params.zw;
vec4 vecColor = texture2DLod(s_VectorColors, texParams, 0);
vec4 lineWidth = texture2DLod(s_VectorWidths, texParams, 0) * cMaxLineWidth;
float dashRow = texture2DLod(s_DashCoords, texParams, 0).r;
vec4 line_lengthTotal = vec4(u_params.xy,0,0);
vec2 p1 = u_p1p2.xy;
vec2 p2 = u_p1p2.zw;
// declare tile positions and their corresponding z values
vec2 tileP1 = mix(u_tileMin.xy, u_tileMax.xy, p1.xy);
vec2 tileP2 = mix(u_tileMin.xy, u_tileMax.xy, p2.xy);
vec2 prevTP = mix(u_tileMin.xy, u_tileMax.xy, u_PrevNext.xy);
vec2 nextTP = mix(u_tileMin.xy, u_tileMax.xy, u_PrevNext.zw);
float tileZ1 = u_tileMin.z;
float tileZ2 = u_tileMin.z;
float prevZ = u_tileMin.z;
float nextZ = u_tileMin.z;
vec2 tilePos = mix(p1, p2, position.y);
vec4 texcoords = vec4(position.xy,0,0);
float lineLengthSoFar = position.z;
vec4 line_size = lineWidth; //override lineWidth here to use debug values for line dimensions
float widthExpansion = max(line_size.x, line_size.y) + line_size.z + line_size.w + 2.0; // x is solid width, y is dash width, zw are outline gap and width params, constant on the end is excess padding so we have room for AA
widthExpansion /= 2.0; //account for x uv being -1 to both side so need to cut width in half to get proper pixel count
float endA = 1.0 - texcoords.y; //uv.y is 0 for this one, so make it 1.0
float endB = texcoords.y; //uv.y is 1 for this one
// add heights to vertices
tileZ1 += u_tileSize.z * meshHeightAtPlanes(p1, u_tileDistortion.xy, u_ScaleOffsetHeight, u_MeshResolution.x, s_heightTexture);
tileZ2 += u_tileSize.z * meshHeightAtPlanes(p2, u_tileDistortion.xy, u_ScaleOffsetHeight, u_MeshResolution.x, s_heightTexture);
prevZ += u_tileSize.z * meshHeightAtPlanes(u_PrevNext.xy, u_tileDistortion.xy, u_ScaleOffsetHeight, u_MeshResolution.x, s_heightTexture);
nextZ += u_tileSize.z * meshHeightAtPlanes(u_PrevNext.zw, u_tileDistortion.xy, u_ScaleOffsetHeight, u_MeshResolution.x, s_heightTexture);
// convert to screen space and expand the line
vec3 wp1 = vec3(tileP1, tileZ1);
vec3 wp2 = vec3(tileP2, tileZ2);
vec3 worldPosition = mix(wp1.xyz, wp2.xyz, position.y); // write to this variable so fog works correctly
vec4 distFade = vec4(1.0 - smoothstep(u_TileLineOpacityTransition.x, u_TileLineOpacityTransition.y, length(worldPosition.xyz)), 0.0, 0.0, 0.0);
vec3 fromEye = normalize(worldPosition);
widthExpansion *= min(1.0, u_NearFarFocus.z / length(worldPosition.xyz)); // scale based on distance
widthExpansion = max(1.5, widthExpansion);
float biasKm = max(0.020, 0.004 * u_NearFarFocus.z);
wp1 *= max(0.5, 1.0 - biasKm / length(wp1));
wp2 *= max(0.5, 1.0 - biasKm / length(wp2));
vec4 screen1 = mul(u_viewProj, vec4(wp1, 1.0));
vec4 screen2 = mul(u_viewProj, vec4(wp2, 1.0));
float origW = mix(screen1.w, screen2.w, position.y);
screen1 = toScreenCoords(screen1, u_viewTexel.xy);
screen2 = toScreenCoords(screen2, u_viewTexel.xy);
vec4 line_endPointsScreen = vec4(screen1.xy, screen2.xy);
vec4 transformPosition = mix(screen1, screen2, position.y);
vec3 screenPos = transformPosition.xyz / transformPosition.w;
vec2 lineDirection = screen2.xy - screen1.xy;
float lineLength = length(lineDirection);
lineDirection /= lineLength;
vec2 lineSide = normalize(vec2(-lineDirection.y, lineDirection.x));
screenPos.xy += lineSide * texcoords.x * widthExpansion;
// overlap ends for the pixel miter
vec2 endExpansion = (-lineDirection * endA * widthExpansion + lineDirection * endB * widthExpansion);
screenPos.xy += endExpansion;
float deltaZ = screen2.z - screen1.z;
float depthAdjustment = widthExpansion * deltaZ / lineLength;
screenPos.z -= endA * depthAdjustment;
screenPos.z += endB * depthAdjustment;
vec4 line_endFlags = vec4(u_PrevNext.xz,0,0);
vec3 prevPos = vec3(prevTP, prevZ);
vec3 nextPos = vec3(nextTP, nextZ);
vec2 screenPrev = toScreenCoords(mul(u_viewProj, vec4(prevPos, 1.0)), u_viewTexel.xy).xy;
vec2 screenNext = toScreenCoords(mul(u_viewProj, vec4(nextPos, 1.0)), u_viewTexel.xy).xy;
vec2 prevDir = normalize(screenPrev.xy - screen1.xy);
vec2 nextDir = normalize(screenNext.xy - screen2.xy);
if (u_PrevNext.x < -9999.0) prevDir = lineDirection;
if (u_PrevNext.z < -9999.0) nextDir = -lineDirection;
vec2 jointADir = jointBisector(prevDir, lineDirection);
vec2 jointBDir = jointBisector(-lineDirection, nextDir);
// bias the joint normals in opposite directions
vec2 jointANormal = vec2(-jointADir.y, jointADir.x);
vec2 jointBNormal = vec2(jointBDir.y, -jointBDir.x);
// compute similarity between normals and the appropriate line direction
float similarityA = dot(jointANormal, lineDirection);
float similarityB = dot(jointBNormal, -lineDirection);
// calculate if the normals are facing towards the center of the line (with some floating point error), and flip if not
float flipA = sign(similarityA) * float(abs(similarityA) > 0.001);
float flipB = sign(similarityB) * float(abs(similarityB) > 0.001);
// pass over the joint normal vectors for the mitered ends
vec4 jointNormals = vec4(flipA * jointANormal, flipB * jointBNormal);
float end = texcoords.y;
texcoords.y *= lineLength / widthExpansion;
texcoords.y += sign(end - 0.5) * length(endExpansion) / widthExpansion;
texcoords.z = lineLength / widthExpansion;
vec3 line_dir = vec3(lineDirection, 0.0);
vec3 line_side = vec3(lineSide, 0.0);

//lighting

//compose
float lineEdgeOffsetDist = lineWidth.x * 0.5;
vec4 lineCenter = vec4(1,1,1,1); // vec4(pixelLength * position.y + (lineWidth * 0.5f * position.z), 0.0, 0.0, 0.0);
vec4 screenPosition = vec4(screenPos.xyz, 1.0);
vec4 linePosition = vec4(position, lineWidth.x);
vec4 color = vecColor;
vec4 tilePosition = vec4(tilePos, 0.0, 0.0);
// convert from screen position back to clip space position
vec4 glPos = vec4(screenPos.xyz, 1.0);
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
v_texcoord2 = jointNormals.xyzw;
v_bitangent = line_dir.xyz;
v_tangent = line_side.xyz;
v_texcoord1 = line_size.xyzw;
v_texcoord0 = line_endPointsScreen.xyzw;
v_color4 = line_lengthTotal.xyzw;
v_color3 = line_endFlags.xyzw;
v_color2 = distFade.xyzw;

}

