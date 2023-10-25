$input a_texcoord7, a_position
$output v_texcoord6, v_texcoord7

//includes
#include <common.sh>
#include "map3dFunctions.sc"

//samplers
SAMPLER2D(s_texture0, 0);
uniform vec4 s_texture0_Res;

//cubeSamplers

//definitions
uniform vec4 u_scale;
uniform vec4 u_quatRotation;
uniform vec4 u_translation;
uniform vec4 u_midpoint;
uniform vec4 u_ScaleOffsetTex0;
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

vec4 texcoords = a_texcoord7.xyzw;
vec3 position = a_position.xyz;
//main start
	vec2 tileCoord = mix(u_tileMin.xy, u_tileMax.xy, position.xy);
	float baseHeight = u_tileMin.z + (position.z * u_tileMax.z);
	vec4 worldPosition = vec4(tileCoord, baseHeight, baseHeight);
	vec3 vertexPosition = worldPosition.xyz;
	vec3 midpoint = vec3(0.0, 0.0, 0.0);
mat4 viewMat = u_view;
	midpoint = u_midpoint.xyz;
	// Keep sized fixed regardless of cam distance
	position = length(mul(u_view, vec4(midpoint, 1.0)).xyz) * position;
	float scale = u_scale.x;
	vertexPosition = scale * vertexPosition;
	vertexPosition = rotate(u_quatRotation, vertexPosition) + u_translation.xyz;

//lighting

//compose
	vec4 projected = mul(u_proj, mul(viewMat, vec4(vertexPosition.xyz, 1.0)));
	vec4 depth = projected;
	gl_Position = projected;

v_texcoord6 = texcoords.xyzw;
v_texcoord7 = depth.xyzw;

}

