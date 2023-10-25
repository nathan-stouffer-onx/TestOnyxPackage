$input v_color1, v_color0, v_texcoord0, v_position
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
uniform vec4 u_ScaleOffsetTex0;
uniform vec4 u_depthModifier;
uniform vec4 u_camRight;
uniform vec4 u_camUp;
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
vec4 BlendTextures(vec4 color0, vec2 uv0)
{
  vec4 tex = vec4(0.0, 0.0, 0.0, 0.0);
  vec4 color = vec4(0.0, 0.0, 0.0, 0.0);
  {
  vec2 modUV = u_ScaleOffsetTex0.xy + uv0 * u_ScaleOffsetTex0.zw; 
  tex = texture2D(s_texture0, modUV);
  vec3 expanded = tex.xyz * 2.0 - 1.0;
  float control0 = abs(min(expanded.x, 0.0));
  vec4 mixed = color0 * control0;
  float alpha = min(tex.a, mixed.a);
  color.xyz = mix(color.xyz, mixed.xyz, alpha);
  color.a += alpha;
  }
  color.a = min(1.0, color.a);
  return color;
}



void main()
{

vec4 color1 = v_color1.xyzw;
vec4 color0 = v_color0.xyzw;
vec4 texcoord0 = v_texcoord0.xyzw;
vec3 position = v_position.xyz;
//main start
	vec2 texcoords = texcoord0.xy;
	vec4 fragColor = vec4_splat(0.f);
fragColor = BlendTextures(color0, texcoord0.xy);

//lighting

//compose
if (fragColor.w == 0.0) { discard; }
	gl_FragColor = fragColor;


}
