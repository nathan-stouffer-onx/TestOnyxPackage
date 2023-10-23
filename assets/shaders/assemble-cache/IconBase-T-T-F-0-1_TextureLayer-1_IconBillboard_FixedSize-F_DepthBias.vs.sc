$input a_texcoord0, a_position
$output v_color1, v_color0, v_texcoord0, v_position

//includes
#include <../examples/common/common.sh>
#include "map3dFunctions.sc"

//samplers
SAMPLER2D(s_texture0, 1);
uniform vec4 s_texture0_Res;
SAMPLER2D(s_heightTextureVert, 0);
uniform vec4 s_heightTextureVert_Res;

//cubeSamplers

//definitions
uniform vec4 u_depthModifier;
uniform vec4 u_camRight;
uniform vec4 u_camUp;
uniform vec4 u_ScaleOffsetTex0;
uniform vec4 u_tileMin;
uniform vec4 u_tileMax;
uniform vec4 u_scale;
uniform vec4 u_ScaleOffsetHeight;
uniform vec4 color1;
uniform vec4 color0;
uniform vec4 quatRot;
uniform vec4 pixelSize;
uniform vec4 iconPos;

//functions

void main()
{

vec4 texcoord0 = a_texcoord0.xyzw;
vec3 position = a_position.xyz;
//main start
	vec3 midpoint = iconPos.xyz;
	float uniformScale = u_scale.x;
	vec2 scale = uniformScale * pixelSize.xy;
	// Calculate height from tile height texture
	vec2 uv = midpoint.xy;
	vec2 scaledUV = u_ScaleOffsetHeight.zw * uv.xy + u_ScaleOffsetHeight.xy;
	midpoint.z = texture2DLod(s_heightTextureVert, scaledUV, 0).r;
	// Midpoint is in canonical tile coordinates, including height
	midpoint.z = (midpoint.z - cHeightRange[0]) / (cHeightRange[1] - cHeightRange[0]);
	midpoint = mix(u_tileMin.xyz, u_tileMax.xyz, midpoint.xyz);
	// Keep sized fixed regardless of cam distance
	position = length(mul(u_view, vec4(midpoint.xyz, 1.0)).xyz) * position;
	// Billboard transformations
	vec3 pivot = midpoint.xyz;
	vec2 size = vec2(pixelSize.x, pixelSize.y) * uniformScale;
	vec2 bbScale = vec2(size.x * position.x, size.y * position.y);
	// Subtract up component to adapt to y-axis pointing down in world
	position = pivot + (u_camRight.xyz * bbScale.x) - (u_camUp.xyz * bbScale.y);

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

