$input a_position
$output v_bitangent

//includes
#include <common.sh>
#include "map3dFunctions.sc"

//samplers

//cubeSamplers

//definitions
uniform vec4 u_skyColor;
uniform vec4 u_hazeColor;

//functions

void main()
{

vec3 position = a_position.xyz;
//main start
	vec3 worldPosition = position;
	vec4 projected = mul(u_proj, mul(u_view, vec4(position, 1.0)));
	gl_Position = projected;

//lighting

//compose

v_bitangent = worldPosition.xyz;

}

