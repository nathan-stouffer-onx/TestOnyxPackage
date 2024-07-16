$input v_texcoord7, v_texcoord6
//includes
#include <common.sh>
#include "layers.sc"
#include "derivatives.sc"

//samplers
SAMPLER2D(s_FrameTexture, 1);
uniform vec4 s_FrameTexture_Res;
SAMPLER2D(s_DepthTexture, 0);
uniform vec4 s_DepthTexture_Res;

//cubeSamplers

//definitions
uniform vec4 u_SharpenStrength;
uniform vec4 u_DOFParams;
uniform vec4 u_BlurParams;
uniform vec4 u_CameraParams;
uniform vec4 u_BackgroundColor;
uniform vec4 u_NearFarFocus;
uniform vec4 u_eyePos;
uniform vec4 u_camRight;
uniform vec4 u_camForward;
uniform vec4 u_camUp;
uniform vec4 u_time;
uniform vec4 u_tileMin;
uniform vec4 u_tileMax;

//functions
vec3 texSample(const int x, const int y, vec2 origUV)
{
vec2 uv = origUV.xy + vec2(x,y) * s_FrameTexture_Res.zw;
	return texture2D(s_FrameTexture, uv).xyz;
}
float dbsize = 1.25; // depth blur size
const float CoC = 0.03; //circle of confusion size in mm (35mm film = 0.03mm)
const int rings = 2;
const int samples = 4;
const int maxringsamples = 3 * 4;

// generating noise / pattern texture for dithering
vec2 rand(vec2 coord)
{
	//float noiseX = ((fract(1.0 - coord.x * s_FrameTexture_Res.x * 0.5) * 0.125) + (fract(coord.y * s_FrameTexture_Res.y * 0.5) * 0.875)) * 2.0 - 1.0;
	//float noiseY = ((fract(1.0 - coord.x * s_FrameTexture_Res.x * 0.5) * 0.875) + (fract(coord.y * s_FrameTexture_Res.y * 0.5) * 0.125)) * 2.0 - 1.0;

	//if (noise) {
		float noiseX = clamp(fract(sin(dot(coord, vec2(12.9898, 78.233)) + u_time.x) * 43758.5453), 0.0, 1.0) * 2.0 - 1.0;
		float noiseY = clamp(fract(sin(dot(coord, vec2(12.9898, 78.233) * 2.0) + u_time.x) * 43758.5453), 0.0, 1.0) * 2.0 - 1.0;
	//}

	return vec2(noiseX,noiseY);
}

// processing the sample ------------------------
vec3 color(vec2 coords, float blur) {
	vec3 col = vec3(0, 0, 0);

	// read from the render buffer at an offset
	col.r = texture2D(s_FrameTexture, coords + vec2(0.0, 1.0) * s_DepthTexture_Res.zw * u_BlurParams.x /* fringe */ * blur).r;
	col.g = texture2D(s_FrameTexture, coords + vec2(-0.866, -0.5) * s_DepthTexture_Res.zw * u_BlurParams.x /* fringe */ * blur).g;
	col.b = texture2D(s_FrameTexture, coords + vec2(0.866, -0.5) * s_DepthTexture_Res.zw * u_BlurParams.x /* fringe */ * blur).b;

	vec3 lumcoeff = vec3(0.299, 0.587, 0.114); // arbitrary numbers???
	float lum = dot(col.rgb, lumcoeff);
	float thresh = max((lum - u_BlurParams.y /* threshold */) * u_BlurParams.z /* gain*/, 0.0);
	return col + mix(vec3(0, 0, 0), col, thresh * blur);
}
//----------------------------------------------
float gather(float i, float j, int ringsamples, inout vec3 col, float w, float h, float blur, vec2 modUV) {
	float rings2 = float(rings);
	float step = PI_CONSTS.y / float(ringsamples);
	float pw = cos(j * step) * i;
	float ph = sin(j * step) * i;
	float p = 1.0;
	col += color(modUV + vec2(pw * w, ph * h), blur) * mix(1.0, i / rings2, u_BlurParams.w /* bias */) * p;
	return 1.0 * mix(1.0, i / rings2, u_BlurParams.w /* bias */) * p;
}

float linearize(float depth, float znear, float zfar) {
	return -zfar * znear / (depth * (zfar - znear) - zfar);
}
//----------------------------------------------
vec4 blur(vec2 modUV, float amount)
{
	vec4 result = vec4(0, 0, 0, 1);
	vec2 spread = s_FrameTexture_Res.zw * amount;
//	// calculation of pattern for dithering
//	vec2 noise = rand(modUV) * amount;
//result.rg = noise;
//	// getting blur x and y step factor
//	// calculation of final color,
	vec3 col = texture2D(s_FrameTexture, modUV).rgb;
	if (amount >= 0.05) {
			float totalSamples = 3.0;
			col *= totalSamples;
			for (float i = -2.0; i <= 2.0; i++) {
				for (float j = -2.0; j <= 2.0; j++) {
					float scale = abs(i) + abs(j);
					float factor = 1.0 / (1.0 + (scale * scale));
					totalSamples += factor;
					col += (texture2D(s_FrameTexture, modUV + (vec2(i, j) * spread)) * factor).rgb;
				}
			}
			col /= totalSamples; // divide by sample count
	}
	result.rgb = col.rgb;
	return result;
}

void main()
{

vec4 depth = v_texcoord7.xyzw;
vec4 texcoords = v_texcoord6.xyzw;
//main start
vec4 fragColor = u_BackgroundColor;
vec2 modUV = texcoords.xy;
fragColor = texture2D(s_FrameTexture, modUV);
//vec3 result = (fragColor.rgb * 9) - (texSample(-1,-1, modUV) + texSample(0,-1, modUV) + texSample(1,-1, modUV) + texSample(-1,0, modUV) + texSample(1,0, modUV) + texSample(-1,1, modUV) + texSample(0,1, modUV) + texSample(1,1, modUV));
vec4 depthPx = texture2D(s_DepthTexture, modUV);
float depthDist = abs(u_DOFParams.x - depthPx.r);
float blurDist = smoothstep(u_DOFParams.y * 0.5, 1.0, abs(u_DOFParams.x - depthPx.r));
float focus = step(u_DOFParams.y, depthDist);
//vec4 blur(vec2 modUV, float focalLength, float focalDepth, float fstop, float dithering, float maxblur)
float spread = u_DOFParams.z;
vec4 blurred = blur(modUV, spread * clamp(blurDist * 1000.0, 0.0, 1.0));
fragColor.rgb = mix(fragColor.rgb, blurred.rgb, u_DOFParams.w /* * focus */);
// uncomment these 2 lines to show DOF params
//fragColor.r = mix(depthPx.r, fragColor.r, u_DOFParams.w);
//fragColor.b = mix(blurDist, fragColor.b, u_DOFParams.w);

//lighting

//compose
	gl_FragData[0] = fragColor;
	//scaling from -1,1 to 0,1, but I suspect that may only be needed for opengl?
	float d = ((depth.z / depth.w) * 0.5 + 0.5) * 256.0;
	float r = floor(d);
	d = (d - r) * 256.0;
	float g = floor(d);
	d = (d - g) * 256.0;
	float b = floor(d);
	gl_FragData[1] = vec4(r / 256.0, g / 256.0, b / 256.0, 1.0);


}

