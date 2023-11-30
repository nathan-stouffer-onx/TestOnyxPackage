$input a_position
$output v_texcoord7, v_color0, v_texcoord6, v_texcoord5

//includes
#include <common.sh>
#include "OnyxFunctions.sc"

//samplers
SAMPLER2D(s_DashSampler, 0);
uniform vec4 s_DashSampler_Res;

//cubeSamplers

//definitions
uniform vec4 u_dashUV;
uniform vec4 u_nearFarPlane;
uniform vec4 u_eyePos;
uniform vec4 u_camRight;
uniform vec4 u_camForward;
uniform vec4 u_camUp;
uniform vec4 u_time;
uniform vec4 u_tileMin;
uniform vec4 u_tileMax;
uniform vec4 u_origin;
uniform vec4 u_length;
uniform vec4 u_screenDimensions;
uniform vec4 u_color;
uniform vec4 u_drawColor;
uniform vec4 u_endCaps;

//functions

void main()
{

vec3 position = a_position.xyz;
//main start
	 float lineWidth = u_origin.w;
	 float lineLength = u_length.w;
	 float p1Cap = u_endCaps.x;
	 float p2Cap = u_endCaps.y;
	 vec3 p1 = u_origin.xyz;
	 vec3 p2 = p1 + ((u_length.xyz) * lineLength);
	 vec2 screenDir = u_length.xy;
	 vec3 screenPos = u_origin.xyz;
	 float pixelLength = u_length.w;
	 float wPos = 1.0;

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
	 screenPosition.z = clamp(screenPosition.z, 0.0, 1.0);
	gl_Position = vec4(screenPosition.xyz * wPos, wPos);

v_texcoord7 = linePosition.xyzw;
v_color0 = color.xyzw;
v_texcoord6 = screenPosition.xyzw;
v_texcoord5 = lineCenter.xyzw;

}

