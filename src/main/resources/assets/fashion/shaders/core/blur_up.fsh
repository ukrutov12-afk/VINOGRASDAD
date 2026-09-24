#version 330

uniform sampler2D Source;

layout(std140) uniform BlurPass {
    vec4 Texel;
};

in vec2 vUv;
out vec4 fragColor;

void main() {
    vec2 o = Texel.xy;
    vec3 s = texture(Source, vUv + vec2(-o.x * 2.0, 0.0)).rgb;
    s += texture(Source, vUv + vec2(-o.x, o.y)).rgb * 2.0;
    s += texture(Source, vUv + vec2(0.0, o.y * 2.0)).rgb;
    s += texture(Source, vUv + vec2(o.x, o.y)).rgb * 2.0;
    s += texture(Source, vUv + vec2(o.x * 2.0, 0.0)).rgb;
    s += texture(Source, vUv + vec2(o.x, -o.y)).rgb * 2.0;
    s += texture(Source, vUv + vec2(0.0, -o.y * 2.0)).rgb;
    s += texture(Source, vUv + vec2(-o.x, -o.y)).rgb * 2.0;
    vec3 c = s / 12.0;
    fragColor = vec4(mix(c, c * Texel.w, Texel.z), 1.0);
}
