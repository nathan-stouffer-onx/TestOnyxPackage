$input v_color2, v_position, v_normal, v_color1, v_texcoord0, v_color0, v_color3
//includes
#include <../examples/common/common.sh>
#include "layers.sc"

//samplers
SAMPLER2D(s_texture0, 0);
uniform vec4 s_texture0_Res;

//cubeSamplers

//definitions
uniform vec4 u_ScaleOffsetTex0;
uniform vec4 u_bbOffset;
uniform vec4 u_billboardSize;
uniform vec4 u_pivot;
uniform vec4 u_NearFarFocus;
uniform vec4 u_eyePos;
uniform vec4 u_camRight;
uniform vec4 u_camForward;
uniform vec4 u_camUp;

//functions
vec4 BlendTextures(vec4 color0, vec4 color1, vec4 color2, vec4 color3, vec2 uv0)
{
  vec4 tex = vec4(0.0, 0.0, 0.0, 0.0);
  vec4 color = vec4(0.0, 0.0, 0.0, 0.0);
  {
  vec2 modUV = u_ScaleOffsetTex0.xy + uv0 * u_ScaleOffsetTex0.zw; 
  tex = texture2D(s_texture0, modUV);
  vec3 expanded = tex.xyz * 2.0 - 1.0;
  float control0 = abs(min(expanded.x, 0.0));
  float control1 = abs(min(expanded.y, 0.0));
  float control2 = max(expanded.x, 0.0);
  float control3 = max(expanded.y, 0.0);
  vec4 mixed = color0 * control0;
  mixed = mix(mixed, color1, control1);
  mixed = mix(mixed, color2, control2);
  mixed = mix(mixed, color3, control3);
  float alpha = min(tex.a, mixed.a);
  color.xyz = mix(color.xyz, mixed.xyz, alpha);
  color.a += alpha;
  }
  color.a = min(1.0, color.a);
  return color;
}



void main()
{

vec4 color2 = v_color2.xyzw;
vec3 position = v_position.xyz;
vec3 normal = v_normal.xyz;
vec4 color1 = v_color1.xyzw;
vec4 texcoord0 = v_texcoord0.xyzw;
vec4 color0 = v_color0.xyzw;
vec4 color3 = v_color3.xyzw;
//main start
	vec4 fragColor = vec4(0,0,0,0);
fragColor = BlendTextures(color0, color1, color2, color3, texcoord0.xy);

//lighting

//compose
if (fragColor.w == 0.0) { discard; }
	gl_FragColor = fragColor;


}

