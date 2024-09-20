$input v_bitangent

#include <common.sh>

uniform vec4 u_SkyColor;
uniform vec4 u_HazeColor;

void main()
{
	vec3 worldPosition = v_bitangent.xyz;
	gl_FragData[0] = vec4(mix(u_HazeColor, u_SkyColor, worldPosition.z).xyz, 1.0);
	gl_FragData[1] = vec4(1, 1, 1, 1);
}

