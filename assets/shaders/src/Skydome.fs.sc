$input v_bitangent
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

vec3 worldPosition = v_bitangent.xyz;
//main start

//lighting

//compose
	gl_FragData[0] = vec4(mix(u_hazeColor, u_skyColor, worldPosition.z).xyz, 1.0);
	gl_FragData[1] = vec4(1, 1, 1, 1);


}

