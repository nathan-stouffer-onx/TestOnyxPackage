// file containing general functions for use in our shaders
// Don't name your function parameters as sampler. The metal compler doesn't like that

// Constants. BGFX Metal only supports float4, mat4, and mat3 datatypes for uniforms
// [1] - PI
// [2] - TWO_PI
// [3] - PI_HALVES 
// [4] - PI_FOURTHS 
CONST( vec4 PI_CONSTS = vec4(3.1415926535, 2.0 * 3.1415926535, 3.1415926535 * 0.5, 3.1415926535 * 0.25) );

// constants for min/max possible elevations of the work (in km)
// NOTE: if these values are changed, update analogous values in Utils/MapMath.h
// [1] - cMinHeightKm;
// [2] - cMaxHeightKm;
CONST( vec4 cHeightRange = vec4(-10.0, 9.0, 0, 0) );

vec4 map3dBlendTextures(sampler2D textureSampler, vec4 color0, vec2 uv0, vec3 scaleOffset)
{
    vec4 tex = vec4(0.0, 0.0, 0.0, 0.0);
    vec4 color = color0;
    {
        vec2 modUV = scaleOffset.xy + uv0 * scaleOffset.z;
        tex = texture2D(textureSampler, modUV);
        color.xyz = mix(color.xyz, tex.xyz, tex.a);
        color.a += tex.a;
    }
    color.a = min(1.0, color.a);
    return color;
}

vec4 sampleTextureFunc(sampler2D textureSampler, vec2 uv, vec4 scaleOffset)
{
    vec2 modUV = scaleOffset.xy + scaleOffset.zw * uv;
    return texture2D(textureSampler, modUV);
}

bool equWeak(vec4 lhs, vec4 rhs, float epsilon)
{
    vec4 diff = lhs - rhs;
    return (dot(diff, diff) < epsilon);
}

bool equWeak(vec3 lhs, vec3 rhs, float epsilon)
{
    vec3 diff = lhs - rhs;
    return (dot(diff, diff) < epsilon);
}

bool equWeak(vec2 lhs, vec2 rhs, float epsilon)
{
    vec2 diff = lhs - rhs;
    return (dot(diff, diff) < epsilon);
}


// returns 1 if in the range [minimum, maximum] and 0 if not in the range
float inRange(float x, float minimum, float maximum)
{
    // TODO possibly benchmark the difference between these two methods?
    // NOTE: the commented out method actually returns 1 for the range (minimum, maximum)
    //float t = lerpInv(minimum, maximum, x);
    //float in = max(sign(t), 0.0) * max(sign(1.0 - t), 0.0);
    return float(minimum <= x) * float(x <= maximum);
}

// returns 1 if in the range [minimum, maximum] and 0 if not in the range
// - also considers the case where maximum < minimum and computes the appropriate range on the circle
// - becuase min/max are on a circle, it is somewhat arbitrary if min = max specifies a single angle
//   or the entire circle. for now, we assume that it means a single angle
float inRangeOnCircle(float theta, float minimum, float maximum)
{
    // indicator for if the maximum is less than the minimum
    float isInverted = float(maximum < minimum);
    // value if this is a regular situation (min <= max)
    float regularVal = inRange(theta, minimum, maximum);
    // value if this is an inverted situation (max < min)
    float invertedVal = 1.0 - inRange(theta, maximum, minimum);
    return (1.0 - isInverted) * regularVal + isInverted * invertedVal;
}

// returns the t value that would satisfy the equation x = lerp(a, b, t)
float lerpInv(float a, float b, float x)
{
    float areNotEqual = float(a != b);
    // set diff to avoid division by zero (results in different behavior on different hardware)
    // we account for this case when returning t
    float diff = areNotEqual * (b - a) + (1.0 - areNotEqual) * 1.0;
    // compute t
    float t = (x - a) / diff;
    // return t if max != min and 0.0 if max == min
    return areNotEqual * t + (1.0 - areNotEqual) * 0.0;
}

// returns the t value that would satisfy the equation theta = lerp(a, b, t) where we wind
// clockwise from a -> b on the circle as t ranges from [0, 1]
float circleLerpInv(float a, float b, float theta)
{
    float TWO_PI = PI_CONSTS.y;

    // compute the arclength and its complement
    float length = (b - a) + TWO_PI * float(b < a);
    float lengthComplement = TWO_PI - length;
    // compute the midpoint of the complement of the range [a, b] on the circle
    float dist = lengthComplement / 2.0;
    float m = a - dist + TWO_PI * float(a < dist);
    // m is guaranteed to not be in the range, so measure our angle a, b, and theta relative
    // to m. then just use regular lerpInv
    float relA = (a - m) + TWO_PI * float(a < m);
    float relB = (b - m) + TWO_PI * float(b < m);
    float relTheta = (theta - m) + TWO_PI * float(theta < m);
    return lerpInv(relA, relB, relTheta);
}

// compute the slope angle (in radians) based on the normal
float calcSlopeAngle(vec3 normal)
{
    // trying a slightly more complex but accurate way to test results
    return acos(dot(normal, vec3(0, 0, 1)));
}

// compute the slope direction (in radians) based on the normal -- north is 0 (= 2 * pi) going counterclockwise
float calcSlopeDir(vec3 normal)
{
    float TWO_PI = PI_CONSTS.y;
    float PI_HALVES = PI_CONSTS.z;

    // compute the arctangent of the ray (x, -y) -- negative y to flip north to being positive
    float rad = atan2(-normal.y, normal.x);
    // rotate so zero is north
    rad -= PI_HALVES;
    // if radians is negative, just add 2 * pi to get into positive radians
    rad += float(rad < 0.0) * TWO_PI;
    // flip so the angle goes clockwise
    rad = TWO_PI - rad;
        
    return rad;
}

float calcCompassMask(vec3 normal, int compassDir, float compassClip)
{
    float compassAmount = sign(float(compassDir)); //1.0 if anything but 0
    float compassMidRad = radians(360.0 - float(compassDir - 1) * 45.0 - 22.5 + 90.0); //should be north at 1, going clockwise in 45 deg steps from there
    vec3 compassVec = vec3(cos(compassMidRad), sin(compassMidRad), 0);
    float compassFacingAmount = max((dot(normalize(normal.xy), compassVec.xy) - compassClip) / (1.0 - compassClip) , 0.0); //ignore z and calculate what normal is facing int he compass direction
    compassFacingAmount = pow(compassFacingAmount, 2.0); //sharpen it up a bit further
    compassFacingAmount = min(compassFacingAmount + 1.0 - compassAmount, 1.0); //disable if compass is 0

    return compassFacingAmount;
}

vec3 calcSunDir(float julianCentury, float localTime) //JC calculated on cpu side and passed in, since its fixed for all verts/pixels 
{
    //we want a sun angle relative to the globe not a pixel, so set lat and lon to 0 when doing the sun angle calculations
   float PI = PI_CONSTS.x;

   float geomMeanLongSunDeg = mod((280.46646 + julianCentury * (36000.76983 + julianCentury * 0.0003032)), 360.0);
   float geomMeanAnomSunDeg = 357.52911 + julianCentury * (35999.05029 - 0.0001537 * julianCentury);
   float eccentEarthOrbit = 0.016708634 - julianCentury * (0.000042037 + 0.0000001267 * julianCentury);
   float sunEqofCtr = sin(geomMeanAnomSunDeg * PI / 180.0) * (1.914602 - julianCentury * (0.004817 + 0.000014 * julianCentury)) + sin(2.0 * geomMeanAnomSunDeg * PI / 180.0) * (0.019993 - 0.000101 * julianCentury) + sin(PI / 180.0 * 3.0 * geomMeanAnomSunDeg) * 0.000289;

   float sunTrueLongDeg = geomMeanLongSunDeg + sunEqofCtr;
   float sunAppLongDeg = sunTrueLongDeg - 0.00569 - 0.00478 * sin((125.04 - 1934.136 * julianCentury) * PI / 180.0);
   float sunDeclinDeg = 180.0 / PI * asin(0.397767f * sin(sunAppLongDeg * PI / 180.0));
      
   float varY = 0.043031;
            
   float eqOfTimeMinutes = 4.0 * 180.0 / PI * (varY * sin(2.0 * PI / 180.0 * geomMeanLongSunDeg) - 2.0 * eccentEarthOrbit * sin(geomMeanAnomSunDeg * PI / 180.0) + 4.0 * eccentEarthOrbit * varY * sin(PI / 180.0 * geomMeanAnomSunDeg) * cos(2.0 * geomMeanLongSunDeg * PI / 180.0) - 0.5 * varY * varY * sin(4.0 * geomMeanLongSunDeg * PI / 180.0) - 1.25 * eccentEarthOrbit * eccentEarthOrbit * sin(2.0 * PI / 180.0 * geomMeanAnomSunDeg));
            
   float trueSolarTimeMinutes = mod((localTime * 1440.0 + eqOfTimeMinutes), 1440.0);
   float hourAngleDeg = trueSolarTimeMinutes / 4.0 < 0.0 ? trueSolarTimeMinutes / 4.0 + 180.0 : trueSolarTimeMinutes / 4.0 - 180.0;
   float solarZenithAngleDeg = 180.0 / PI * (acos(cos(sunDeclinDeg * PI / 180.0) * cos(hourAngleDeg * PI / 180.0)));
   float solarElevationAngleDeg = 90.0 - solarZenithAngleDeg;
 
    float solarAzimuthAngleDeg = 0.0;
    float offset = 0.0;
    float offsetSign = 1.0;
    float flip = 180.0;
    if(hourAngleDeg <= 0.0)
    {
        offset = 540.0;
        offsetSign = -1.0;
        flip = 0.0;
    }
    float ac = -sin(PI / 180.0 * sunDeclinDeg);
    float s = sin(PI / 180.0 * solarZenithAngleDeg);
    float divided = ac / s;
    float buggyAcos = sign(divided) * min(abs(divided), 1.0);
    float result = offset + offsetSign * 180.0 / PI * acos( buggyAcos)  + flip;
    solarAzimuthAngleDeg = mod(result, 360.0);
        //solarAzimuthAngleDeg = mod(           
        //offset + offsetSign * 180.0 / PI * acos( -sin(PI / 180.0 * sunDeclinDeg) / sin(PI / 180.0 * solarZenithAngleDeg) ) + flip
        //, 360.0);


    vec3 sunNormal;
    sunNormal.z = cos(solarElevationAngleDeg * PI / 180.0) * cos(solarAzimuthAngleDeg * PI / 180.0);
    sunNormal.y = cos(solarElevationAngleDeg * PI / 180.0) * sin(solarAzimuthAngleDeg * PI / 180.0);
    sunNormal.x = sin(solarElevationAngleDeg * PI / 180.0);

    return sunNormal;
}

float calcSunlightTangent(vec2 worldUV, vec3 tangentNormal, vec3 sunDir)
{
    float PI = PI_CONSTS.x;

    float longitude = (worldUV.x - 0.5) * 2.0 * PI; 
    float latitude = ((1.0 - worldUV.y) - 0.5) * 2.0 * PI / 2.0;
  
    vec3 planetNormal;
    planetNormal.x = cos(latitude) * cos(longitude);
    planetNormal.y = cos(latitude) * sin(longitude);
    planetNormal.z = sin(latitude);
            
    vec3 tangent;
    vec3 binormal;

    tangent = cross(planetNormal, vec3(1.0, 0.0, 0.0));
    tangent = normalize(tangent);

    binormal = cross(planetNormal, tangent);
    binormal = normalize(binormal);

    vec3 tangentSun = vec3(0,0,0);
    tangentSun.x  = -dot( tangent, sunDir.xyz );
    tangentSun.y  = -dot( binormal, sunDir.xyz );
    tangentSun.z  = dot( planetNormal, sunDir.xyz );

    return saturate(dot(tangentSun, tangentNormal));
}

float calcSunlight(vec2 worldUV, vec3 sunDir)
{
    float PI = PI_CONSTS.x;

    float longitude = (worldUV.x - 0.5) * 2.0 * 180.0; 
    float latitude = ((1.0 - worldUV.y) - 0.5) * 2.0 * 90.0;
  
    vec3 Normal;
    Normal.x = cos(latitude * PI / 180.0) * cos(longitude * PI / 180.0);
    Normal.y = cos(latitude * PI / 180.0) * sin(longitude * PI / 180.0);
    Normal.z = sin(latitude * PI / 180.0);

    float d = saturate(dot(Normal, sunDir));
    return d;
}

// compute which of a, b is closer to p. defaults to a if distances are equal
vec3 closer(vec3 p, vec3 a, vec3 b)
{
    float distA = length(a - p);
    float distB = length(b - p);
    return (distA <= distB) ? a : b;
}

// compute which of a, b is closer to p. defaults to a if distances are equal
vec2 closer(vec2 p, vec2 a, vec2 b)
{
    float distA = length(a - p);
    float distB = length(b - p);
    return (distA <= distB) ? a : b;
}

struct Triangle
{
    vec3 p0;
    vec3 p1;
    vec3 p2;
};

// compute triangle area
float area(Triangle tri)
{
	vec3 u = tri.p1 - tri.p0;
	vec3 v = tri.p2 - tri.p0;
	return 0.5 * length(cross(u, v));
}

// Rotation by the quaternion sandwich product. 
vec3 rotate(vec4 unitQuat, vec3 toRot)
{
    vec3 b = unitQuat.xyz;
    float bSqrd = b.x * b.x + b.y * b.y + b.z * b.z;
    
    return (unitQuat.w * unitQuat.w - bSqrd) * toRot
     + 2.0 * dot(toRot, b) * b 
     + 2.0 * unitQuat.w * cross(b, toRot);
}