#version 330

layout(std140) uniform UiFrame {
    vec4 Screen;
    vec4 Params;
    vec4 Params2;
};

in vec2 aPos;
in vec2 aLocal;
in vec4 aRect;
in vec4 aRadii;
in vec4 aFill;
in vec4 aLine;
in vec4 aGlow;
in vec4 aP1;
in vec4 aP2;
in vec4 aUv;
in vec4 aClip;

out vec2 vLocal;
out vec3 vFillLab;
out float vFillA;
out vec4 vUv;
flat out vec4 vRect;
flat out vec4 vRadii;
flat out vec4 vLine;
flat out vec4 vGlow;
flat out vec4 vP1;
flat out vec4 vP2;
flat out vec4 vClip;

vec3 toLinear(vec3 c) {
    return mix(c / 12.92, pow((c + 0.055) / 1.055, vec3(2.4)), step(0.04045, c));
}

float cbrt(float x) {
    return sign(x) * pow(abs(x), 1.0 / 3.0);
}

vec3 toOklab(vec3 c) {
    float l = 0.4122214708 * c.r + 0.5363325363 * c.g + 0.0514459929 * c.b;
    float m = 0.2119034982 * c.r + 0.6806995451 * c.g + 0.1073969566 * c.b;
    float s = 0.0883024619 * c.r + 0.2817188376 * c.g + 0.6299787005 * c.b;
    l = cbrt(l);
    m = cbrt(m);
    s = cbrt(s);
    return vec3(
        0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
        1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
        0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s);
}

void main() {
    gl_Position = vec4(aPos.x * Screen.z * 2.0 - 1.0, 1.0 - aPos.y * Screen.w * 2.0, 0.0, 1.0);
    vLocal = aLocal;
    vFillLab = toOklab(toLinear(aFill.rgb));
    vFillA = aFill.a;
    vUv = aUv;
    vRect = aRect;
    vRadii = aRadii;
    vLine = aLine;
    vGlow = aGlow;
    vP1 = aP1;
    vP2 = aP2;
    vClip = aClip;
}
