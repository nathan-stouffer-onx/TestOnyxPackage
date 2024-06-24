#ifndef __MATH_SC__
#define __MATH_SC__

// Constants. BGFX Metal only supports float4, mat4, and mat3 datatypes for uniforms
// [1] - PI
// [2] - TWO_PI
// [3] - PI_HALVES 
// [4] - PI_FOURTHS 
CONST( vec4 PI_CONSTS = vec4(3.1415926535, 2.0 * 3.1415926535, 3.1415926535 * 0.5, 3.1415926535 * 0.25) );

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

mat4 getRotMat(vec3 axis, float rad)
{
    float cosA = cos(rad);
	float sinA = sin(rad);
	float omCosA = 1.0 - cosA;

    float X = axis.x;
    float Y = axis.y;
    float Z = axis.z;

    return mtxFromRows(
        vec4(cosA + X * X * omCosA, X * Y * omCosA - Z * sinA, X * Z  * omCosA + Y * sinA, 0),
        vec4(X * Y * omCosA + Z * sinA, cosA + Y * Y * omCosA, Y * Z * omCosA - X * sinA, 0),
        vec4(Z * X * omCosA - Y * sinA, Z * Y * omCosA + X * sinA, cosA + Z * Z * omCosA, 0),
        vec4(0, 0, 0, 1)
    );
}

vec3 rotate(vec3 v, vec3 axis, float rad)
{
    return mul(getRotMat(axis, rad), vec4(v, 1.0)).xyz;
}

// Creates a quaternion rotating "from" to "to". Assumes that "from" and "to" are normalized. 
// If rotation is ill-defined, will return identity quaternion
vec4 getQuatRot(vec3 from, vec3 to)
{
	// Invalid if vectors are equal/negations of each other or if either are zero
    float epsilon = 1e-4;
	if (equWeak(from, to, epsilon) ||
		equWeak(from, -to, epsilon) ||
		equWeak(from, vec3(0, 0, 0), epsilon) || equWeak(to, vec3(0, 0, 0), epsilon))
	{
		return vec4(0, 0, 0, 1);
	}

    from = normalize(from);
    to = normalize(to);

    float angle = acos(dot(from, to));
    float hTheta = 0.5 * angle;
    vec3 rotAxis = normalize(cross(from, to)) * sin(hTheta);

    return vec4(rotAxis, cos(hTheta));
}

vec4 quatMult(vec4 lhs, vec4 rhs)
{
    return vec4(
        lhs.y * rhs.z - lhs.z * rhs.y + lhs.w * rhs.x + lhs.x * rhs.w,
		lhs.z * rhs.x - lhs.x * rhs.z + lhs.w * rhs.y + lhs.y * rhs.w,
		lhs.x * rhs.y - lhs.y * rhs.x + lhs.w * rhs.z + lhs.z * rhs.w,
		lhs.w * rhs.w - lhs.x * rhs.x - lhs.y * rhs.y - lhs.z * rhs.z
    );
}

vec4 composeQuatRots(vec4 firstRot, vec4 secondRot)
{
    return quatMult(secondRot, firstRot);
}

#endif