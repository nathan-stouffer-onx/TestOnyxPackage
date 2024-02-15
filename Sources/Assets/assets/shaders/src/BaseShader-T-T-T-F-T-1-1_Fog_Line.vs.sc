$input a_texcoord5, a_normal, a_position, a_texcoord7, a_texcoord6
$output v_normal, v_texcoord7, v_texcoord6, v_texcoord5, v_texcoord4, v_texcoord3, v_texcoord2

//includes
#include <common.sh>
#include "OnyxFunctions.sc"

//samplers

//cubeSamplers

//definitions
uniform vec4 u_tileLevel;
uniform vec4 u_screenDimensions;
uniform vec4 u_lineWidth;
uniform vec4 u_dashLength;
uniform vec4 u_gapLength;
uniform vec4 u_FogTransition;
uniform vec4 u_FogColor;
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
vec3 convertToLineNormal(vec3 dataFromBuffer)
{
  vec3 lineNormal = vec3(-dataFromBuffer.y, dataFromBuffer.x, 0.0);
  lineNormal /= length(lineNormal);
  return lineNormal;
}


void main()
{

vec4 line_color = a_texcoord5.xyzw;
vec4 normal = a_normal.xyzw;
vec3 position = a_position.xyz;
vec4 texcoords = a_texcoord7.xyzw;
vec4 line_texcoord = a_texcoord6.xyzw;
//main start
	vec2 tileCoord = mix(u_tileMin.xy, u_tileMax.xy, position.xy);
	float baseHeight = u_tileMin.z + (position.z * u_tileMax.z);
	vec4 worldPosition = vec4(tileCoord, baseHeight, baseHeight);
	vec3 vertexPosition = worldPosition.xyz;
normal = mul(u_model[0], vec4(normal.xyz, 0.0));
vec4 clipPos = mul(u_viewProj, vec4(position.xyz, 1.0));
vec2 pixelDims = 2.0 * (1.0 / u_screenDimensions.xy);
float lineWidth = u_lineWidth.x * length(pixelDims) * clipPos.w;
int zoomLevelDifference = int(u_tileLevel.y) - int(u_tileLevel.x);
lineWidth /= pow(2.0, zoomLevelDifference);
vec3 lineNormal = convertToLineNormal(a_normal.xyz);
vec4 clipNormal = normalize(mul(u_viewProj, vec4(lineNormal, 0.0)));
clipPos.xy += clipNormal.xy * line_texcoord.x * (lineWidth / 2.0);
mat4 viewMat = u_view;

//lighting
vec4 fogDist = vec4(length(worldPosition.xyz) / u_nearFarPlane.y, 0.0, 0.0, 0.0);


//compose
	vec4 projected = mul(u_proj, mul(viewMat, vec4(vertexPosition.xyz, 1.0)));
	vec4 depth = projected;
gl_Position = clipPos;

	gl_Position = projected;

v_normal = normal.xyz;
v_texcoord7 = worldPosition.xyzw;
v_texcoord6 = depth.xyzw;
v_texcoord5 = texcoords.xyzw;
v_texcoord4 = fogDist.xyzw;
v_texcoord3 = line_texcoord.xyzw;
v_texcoord2 = line_color.xyzw;

}

