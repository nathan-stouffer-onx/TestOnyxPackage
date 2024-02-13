$input a_position, a_texcoord7, a_color0
$output v_texcoord7, v_texcoord6, v_color0

//includes
#include <common.sh>
#include "OnyxFunctions.sc"

//samplers

//cubeSamplers

//definitions
uniform vec4 u_BackgroundColor;
uniform vec4 u_nearFarPlane;
uniform vec4 u_eyePos;
uniform vec4 u_camRight;
uniform vec4 u_camForward;
uniform vec4 u_camUp;
uniform vec4 u_time;
uniform vec4 u_tileMin;
uniform vec4 u_tileMax;

//functions

void main()
{

vec3 position = a_position.xyz;
vec4 texcoords = a_texcoord7.xyzw;
vec4 color = a_color0.xyzw;
//main start
	vec2 tileCoord = mix(u_tileMin.xy, u_tileMax.xy, position.xy);
	float baseHeight = u_tileMin.z + (position.z * u_tileMax.z);
	vec4 worldPosition = vec4(tileCoord, baseHeight, baseHeight);
	vec3 vertexPosition = worldPosition.xyz;
mat4 viewMat = u_view;

//lighting

//compose
v_color0 = color;
	vec4 projected = mul(u_proj, mul(viewMat, vec4(vertexPosition.xyz, 1.0)));
	vec4 depth = projected;
	gl_Position = projected;

v_texcoord7 = depth.xyzw;
v_texcoord6 = texcoords.xyzw;
v_color0 = color.xyzw;

}

