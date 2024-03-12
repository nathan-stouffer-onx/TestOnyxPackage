$input v_bitangent
//includes
#include <../examples/common/common.sh>
#include "OnyxFunctions.sc"

//samplers
SAMPLER2D(s_posLifeTex, 0);
uniform vec4 s_posLifeTex_Res;
SAMPLER2D(s_velocSeedTex, 1);
uniform vec4 s_velocSeedTex_Res;

//cubeSamplers

//definitions
uniform vec4 u_velLifeMinMax;
uniform vec4 u_initAccelDeltaProg;

//functions
// TODO: Replace with better, more well understood noise functions
float dirtyRand(float x)
{
	return 0.5f + 0.5 * sin(cos(x) * cos(x * x) / sin(12.321 * x)) * cos(cos(32.2121 * x));
}

float dirtyRand(vec2 x)
{
	float single = dot(x, vec2(13.9841, 4.2129));
	return dirtyRand(single);
}

void main()
{

vec3 uv = v_bitangent.xyz;
//main start
	float PI = PI_CONSTS.x;
	vec2 uvCoords = uv.xy;

	vec4 posLife = texture2D(s_posLifeTex, uvCoords);
	// Set life to 0 if first time initializing
	float firstInit = u_initAccelDeltaProg.x;
	posLife.w -= firstInit * posLife.w;
	vec4 velocSeed = texture2D(s_velocSeedTex, uvCoords);
	
	/*----- INIT -----*/
	if (posLife.w <= 0.f)
	{
		// Use random texture if first time initializing
		float rand = firstInit * dirtyRand(uvCoords) + (1.f - firstInit) * velocSeed.w;
		vec2 velMinMax = u_velLifeMinMax.xy;
		float velocMag = mix(velMinMax.x, velMinMax.y, rand);
		
		rand = dirtyRand(rand);
		float velocRho = mix(0.f, 2.f * PI, uvCoords.x);
		
		rand = dirtyRand(rand);
		float velocTheta = mix (0, PI / 90.f, uvCoords.y);
		
		float sinVelocTheta = sin(velocTheta);
		vec3 veloc = vec3(velocMag * sinVelocTheta * cos(velocRho), velocMag * sinVelocTheta * sin(velocRho), velocMag * cos(velocTheta));
		
		rand = dirtyRand(rand);
		vec2 lifeMinMax = u_velLifeMinMax.zw;
		float life = mix(lifeMinMax.x, lifeMinMax.y, rand);
		posLife = vec4(0, 0, 0, life);

		velocSeed = vec4(veloc, rand);
	}
	/*----- UPDATE -----*/
	else
	{
		float deltaT = u_initAccelDeltaProg.z;
		posLife.xyz += deltaT * velocSeed.xyz;
		float animProg = u_initAccelDeltaProg.w;
		posLife.w = clamp(posLife.w - animProg, 0, posLife.w);
		float accel = u_initAccelDeltaProg.y;
		velocSeed.z += (deltaT * accel);
	}
	
	gl_FragData[0] = posLife;
	gl_FragData[1] = velocSeed;
	//gl_FragData[0] = u_velLifeMinMax.xyzw;
	//gl_FragData[1] = u_initAccelDeltaProg.xyzw;

//lighting

//compose


}

