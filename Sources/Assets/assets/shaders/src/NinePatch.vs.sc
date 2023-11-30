$input a_tangent, a_normal
$output v_texcoord7

//includes
#include <../examples/common/common.sh>
#include "OnyxFunctions.sc"

//samplers
SAMPLER2D(s_spriteTex, 0);
uniform vec4 s_spriteTex_Res;

//cubeSamplers

//definitions
uniform vec4 u_screenRes;
uniform vec4 i_offsets0;
uniform vec4 i_offsets1;
uniform vec4 i_screenPosSize;
uniform vec4 i_uv;

//functions

void main()
{

vec4 tangent = a_tangent.xyzw;
vec4 normal = a_normal.xyzw;
//main start

//lighting

//compose
	// The patches are laid out on a grid:
	//
	//   0   1          2   3          4   5
	// 0 +---+----------+---+----------+---+
	//   | A |     B    | C |    D     | E |
	// 1 +---+----------+---+----------+---+
	//   |   |                         |   |
	//   | F |            G            | H |
	//   |   |                         |   |
	// 2 +---+----------+---+----------+---+
	//   | I |     J    | K |    L     | M |
	// 3 +---+----------+---+----------+---+
	vec4 grid0 = normal;
	vec4 grid1 = tangent;
	float x0 = 1.0 - grid0.x - grid0.y - grid0.z - grid0.w - grid1.x; // for x0, all cells will be 0 so we'll get 1.0
	float y0 = 1.0 - grid1.y - grid1.z - grid1.w; // for y0, all cells will be 0 so we'll get 1.0
	vec2 screenSize = i_screenPosSize.zw;
	vec2 screenPos = i_screenPosSize.xy;
	float midPatchSizePx = i_offsets0.z - i_offsets0.y;
	float halfWidth = screenSize.x * 0.5f;
	// Compute x screen offsets
	float vert0xOfs = -i_offsets0.x;
	float vert2xOfs = floor(halfWidth - (midPatchSizePx * 0.5f));
	float vert3xOfs = floor(vert2xOfs + midPatchSizePx);
	float vert4xOfs = screenSize.x;
	float vert5xOfs = screenSize.x + i_offsets1.x - i_offsets0.w;
	vec4 vertXOffsets = vec4(vert0xOfs, vert2xOfs, vert3xOfs, vert4xOfs);
	float widthOffset = dot(vec4(x0, grid0.yzw), vertXOffsets) + (grid1.x * vert5xOfs);
	// Compute y screen offsets
	float vert0yOfs = -i_offsets1.y;
	float vert2yOfs = screenSize.y;
	float vert3yOfs = screenSize.y + i_offsets1.w - i_offsets1.z;
	vec3 vertYOffsets = vec3(vert0yOfs, vert2yOfs, vert3yOfs);
	float heightOffset = dot(vec3(y0, grid1.z, grid1.w), vertYOffsets);
	// Compute screen position px/normalized
	float xScreenPx = screenPos.x + widthOffset;
	float yScreenPx = screenPos.y + heightOffset;
	float xScreenN = (xScreenPx - (u_screenRes.x * 0.5)) * u_screenRes.z * 2.0;
	float yScreenN = (yScreenPx - (u_screenRes.y * 0.5)) * u_screenRes.w * 2.0;
	vec3 pos =  vec3(xScreenN, -yScreenN, 0);
	gl_Position = vec4(pos.xy, 0, 1);
	// Compute UV offset
	float xofs = (dot(grid0, i_offsets0) + (grid1.x * i_offsets1.x)) * s_spriteTex_Res.z;
	float yofs = (dot(grid1.yzw, i_offsets1.yzw)) * s_spriteTex_Res.w;
	//vec4 uv = vertXOffsets;
	vec4 uv = vec4(i_uv.x + xofs, i_uv.y + yofs, xScreenPx, yScreenPx);  // xScreenPx, yScreenPx, xScreenN, yScreenN);

v_texcoord7 = uv.xyzw;

}

