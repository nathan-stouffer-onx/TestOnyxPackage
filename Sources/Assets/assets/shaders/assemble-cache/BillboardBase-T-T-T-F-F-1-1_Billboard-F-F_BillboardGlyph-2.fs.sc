$input v_color0, v_texcoord0, v_normal, v_position
//includes
#include <../examples/common/common.sh>
#include "map3dFunctions.sc"

//samplers
SAMPLER2D(s_texture0, 0);
uniform vec4 s_texture0_Res;
SAMPLER2D(s_texture1, 1);
uniform vec4 s_texture1_Res;
SAMPLER2D(s_bbHeightTexture, 0);
uniform vec4 s_bbHeightTexture_Res;

//cubeSamplers

//definitions
uniform vec4 u_ScaleOffsetTex0;
uniform vec4 u_ScaleOffsetTex1;
uniform vec4 u_bbOffset;
uniform vec4 u_billboardSize;
uniform vec4 u_bbHeightUV;
uniform vec4 u_nearFarPlane;
uniform vec4 u_eyePos;
uniform vec4 u_camRight;
uniform vec4 u_camForward;
uniform vec4 u_camUp;

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

vec4 color0 = v_color0.xyzw;
vec4 texcoord0 = v_texcoord0.xyzw;
vec3 normal = v_normal.xyz;
vec3 position = v_position.xyz;
//main start
vec4 fragColor = vec4(0,0,0,0);
fragColor = BlendTextures(color0, texcoord0.xy);

//lighting

//compose
gl_FragColor = fragColor;
if (fragColor.w == 0.0) { discard; }


}
