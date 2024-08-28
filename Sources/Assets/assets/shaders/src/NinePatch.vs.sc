$input a_tangent, a_normal
$output v_texcoord7, v_texcoord6, v_depth

//includes
#include <../examples/common/common.sh>
#include "layers.sc"

//samplers
SAMPLER2D(s_spriteTex1, 0);
uniform vec4 s_spriteTex1_Res;
SAMPLER2D(s_spriteTex2, 1);
uniform vec4 s_spriteTex2_Res;

//cubeSamplers

//definitions
uniform vec4 u_screenRes;
uniform vec4 u_oriAngle;
uniform vec4 i_offsets0;
uniform vec4 i_offsets1;
uniform vec4 i_screenPosSize;
uniform vec4 i_uvXAxis;
uniform vec4 i_yAxisOpacityTexId;

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
	vec2 uvIn = i_uvXAxis.xy;
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
	vec3 iconRgt = vec3(i_uvXAxis.zw, 0);
	vec2 screenYAxis = i_yAxisOpacityTexId.xy;
	vec3 iconDown = vec3(screenYAxis, 0);
	vec3 offset = widthOffset * iconRgt + heightOffset * iconDown;
	vec2 finalPosScrSp = ((offset + vec3(screenPos.xy, 0))).xy;
	finalPosScrSp = floor(finalPosScrSp);
	vec2 finalPosN = (finalPosScrSp - 0.5*u_screenRes.xy) * (2.0 * u_screenRes.zw);
	gl_Position = vec4(finalPosN.x, -finalPosN.y, 0, 1);
	// Compute UV offset
	// Assumes both sprite textures are the same res
	float xofs = (dot(grid0, i_offsets0) + (grid1.x * i_offsets1.x)) * s_spriteTex1_Res.z;
	float yofs = (dot(grid1.yzw, i_offsets1.yzw)) * s_spriteTex1_Res.w;
	//vec4 uv = vertXOffsets;
	// Set outputs
	vec4 opacity = vec4_splat(i_yAxisOpacityTexId.z);
	float xScreenPx = screenPos.x + widthOffset;
	float yScreenPx = screenPos.y + heightOffset;
	vec4 uv = vec4(uvIn.x + xofs, uvIn.y + yofs, xScreenPx, yScreenPx);  // xScreenPx, yScreenPx, xScreenN, yScreenN);
	float texId = i_yAxisOpacityTexId.w;

v_texcoord7 = uv.xyzw;
v_texcoord6 = opacity.xyzw;
v_depth = texId;

}

