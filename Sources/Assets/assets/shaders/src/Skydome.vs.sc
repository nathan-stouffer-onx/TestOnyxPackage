$input a_position
$output v_bitangent

#include <common.sh>

void main()
{
	gl_Position = mul(u_proj, mul(u_view, vec4(a_position, 1.0)));
	v_bitangent = a_position;
}

