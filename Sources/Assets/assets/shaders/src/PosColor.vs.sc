$input a_position, a_color0
$output v_color0

#include <common.sh>

void main()
{
	gl_Position = mul(u_proj, mul(u_view, vec4(a_position.xyz, 1.0)));
	v_color0 = a_color0;
}