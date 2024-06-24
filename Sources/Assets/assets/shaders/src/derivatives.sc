#ifndef __DERIVATIVES_SC__
#define __DERIVATIVES_SC__

// file containing functions with calls to screen space derivatives

float levelSets(float value, float period, float phase, float minimum, float maximum, float lineWidth)
{
    // compute frequncy
    float freq = 1.0 / period;
    // compute input to gradient computation
    float x = freq * (value - phase);
    // compute the magnitude of the gradient using the manhattan distance metric
    float manhattanMag = abs(dFdx(x)) + abs(dFdy(x));
    // compute scaled distance to line
    float w = 1.0 / lineWidth;
    float dist = w * abs(fract(x - 0.5) - 0.5) / manhattanMag;
    // compute the strength of the line
    float strength = 1.0 - min(dist, 1.0);
    // acount for the level set at the min/max
    float boundaryError = 0.5 * period;
    strength *= inRange(value, minimum - boundaryError, maximum + boundaryError);
    // return strength if the value is in the specified range
    return strength;
}

#endif