#ifndef __TERRAIN_SC__
#define __TERRAIN_SC__

#include "math.sc"

// constants for min/max possible elevations of the work (in km)
// NOTE: if these values are changed, update analogous values in Utils/MapMath.h
// [0] - cMinElevationKm;
// [1] - cMaxElevationKm;
CONST( vec4 cElevationRange = vec4(-10.0, 9.0, 0, 0) );

// expects uv to be in tile coordinates
float elevationAtVS(vec2 uv, vec4 scaleOffset, sampler2D tex)
{
    vec2 scaledUV = scaleOffset.zw * uv + scaleOffset.xy;
    return texture2DLod(tex, scaledUV, 0).r;
}

// expects uv to be in tile coordinates
float distort(float elevation, vec2 uv, vec2 distortion)
{
    float d = mix(distortion.x, distortion.y, uv.y);
    return elevation * d;
}

// expects uv to be in tile coordinates
float heightAtVS(vec2 uv, vec2 distortion, vec4 scaleOffset, sampler2D tex)
{
    float elevation = elevationAtVS(uv, scaleOffset, tex);
    return distort(elevation, uv, distortion);
}

// expects uv to be in tile coordinates",
float elevationAtFS(vec2 uv, vec4 scaleOffset, sampler2D tex)
{
    vec2 scaledUV = scaleOffset.zw * uv + scaleOffset.xy;
    return texture2DLod(tex, scaledUV, 0).r;
}

// expects uv to be in tile coordinates
float heightAtFS(vec2 uv, vec2 distortion, vec4 scaleOffset, sampler2D tex)
{
    float elevation = elevationAtFS(uv, scaleOffset, tex);
    return distort(elevation, uv, distortion);
}

// computes the height of the triangle mesh at the specified uv coordinate
//
//      v0 ----------- v1
//       |           * |
//       |   *p   *    |
//       |     *       |
//       |  *          |
//      v2 ----------- v3
//
// let the above diagram represent the mesh quad that contains the point p. the function
// first computes which three vertices make up the triangle. v1 and v2 are guaranteed to
// be part of the triangle. and then we just compute which of v0 and v3 is closer to p to
// determine the final point. then we compute the plane of the triangle and evaluate the
// function to determine the height
float meshHeightAtPlanes(vec2 uv, vec2 distortion, vec4 scaleOffset, float meshRes, sampler2D tex)
{
    // compute the top left corner of the quad that contains p -- clamp to [eps, 1 - eps] to avoid problems at 0 and 1
    float eps = 1.0 / 256.0;
    vec2 v0 = floor(meshRes * clamp(uv, vec2(eps, eps), vec2(1.0 - eps, 1.0 - eps)));
    vec2 a = vec2(v0.x + 1.0, v0.y) / meshRes; // v1
    vec2 b = vec2(v0.x, v0.y + 1.0) / meshRes; // v2
    vec2 c = closer(uv, vec2(b.x, a.y), vec2(a.x, b.y)); // choose between v0 and v3
    // compute the (translated) world coordinates of the vertex
    vec2 vertex = uv;
    // compute the (translated) world coordinates of the three points that define the plane of this triangle
    vec3 p = vec3(a.xy, heightAtVS(a.xy, distortion, scaleOffset, tex));
    vec3 q = vec3(b.xy, heightAtVS(b.xy, distortion, scaleOffset, tex));
    vec3 r = vec3(c.xy, heightAtVS(c.xy, distortion, scaleOffset, tex));
    // compute plane normal
    vec3 normal = cross(q - p, r - p);
    float z = -(dot(normal.xy, vertex.xy) - dot(normal, p)) / normal.z;
    return z;
}

// computes the height of the triangle mesh at the specified uv coordinate
//
//      v0 ----------- v1
//       |           * |
//       |   *p   *    |
//       |     *       |
//       |  *          |
//      v2 ----------- v3
//
// let the above diagram represent the mesh quad that contains the point p. the function
// first computes which three vertices make up the triangle. v1 and v2 are guaranteed to
// be part of the triangle. and then we just compute which of v0 and v3 is closer to p to
// determine the final point. then we compute the heights at the triangle vertices and
// use barycentric interpolation to compute the final height value
float meshHeightAtBarycentric(vec2 uv, vec2 distortion, vec4 scaleOffset, float meshRes, sampler2D tex)
{
    vec3 p = vec3(uv, 0.0);
    // compute the top left corner of the quad that contains p -- clamp to [eps, 1 - eps] to avoid problems at 0 and 1
    float eps = 1.0 / 256.0;
    vec2 v0 = floor(meshRes * clamp(uv, vec2(eps, eps), vec2(1.0 - eps, 1.0 - eps)));
    vec3 a = vec3(v0.x + 1.0, v0.y, 0.0) / meshRes; // v1
    vec3 b = vec3(v0.x, v0.y + 1.0, 0.0) / meshRes; // v2
    vec3 c = closer(p, vec3(b.x, a.y, 0.0), vec3(a.x, b.y, 0.0)); // choose between v0 and v3
    // compute heights at the three triangle vertices
    vec3 heights = vec3(heightAtVS(a.xy, distortion, scaleOffset, tex), heightAtVS(b.xy, distortion, scaleOffset, tex), heightAtVS(c.xy, distortion, scaleOffset, tex));
    // compute subtriangle A
    Triangle triA;
    triA.p0 = p;
    triA.p1 = b;
    triA.p2 = c;
    // compute subtriangle B
    Triangle triB;
    triB.p0 = p;
    triB.p1 = a;
    triB.p2 = c;
    // compute subtriangle C
    Triangle triC;
    triC.p0 = p;
    triC.p1 = a;
    triC.p2 = b;
    // compute the barycentric coordinates of the point p
    vec3 barycentric = vec3(area(triA), area(triB), area(triC));
    float area = barycentric.x + barycentric.y + barycentric.z;  // the full triangle area is the sum of the three subtriangles
    barycentric /= area;
    // return the interpolated point
    return dot(barycentric, heights);
}

// expects uv to be in tile coordinates
vec3 normalAt(vec2 uv, float tileSize, vec2 distortion, sampler2D tex, vec4 scaleOffset, float texelWidth)
{
    float tileDelta = 0.5 * texelWidth / scaleOffset.z; // step size is half a texel
    vec2 westUV = uv - vec2(tileDelta, 0);
    vec2 eastUV = uv + vec2(tileDelta, 0);
    vec2 northUV = uv - vec2(0, tileDelta);
    vec2 southUV = uv + vec2(0, tileDelta);
    float westZ = heightAtFS(westUV, distortion, scaleOffset, tex);
    float eastZ = heightAtFS(eastUV, distortion, scaleOffset, tex);
    float northZ = heightAtFS(northUV, distortion, scaleOffset, tex);
    float southZ = heightAtFS(southUV, distortion, scaleOffset, tex);
    float worldStep = tileDelta * tileSize;
    vec3 normal = vec3(westZ - eastZ, northZ - southZ, 2.0 * worldStep);
    return normalize(normal);
}

#endif