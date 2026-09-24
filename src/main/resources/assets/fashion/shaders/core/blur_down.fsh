#version 330

uniform sampler2D Source;

layout(std140) uniform BlurPass {
    vec4 Texel;
};

in vec2 vUv;
out vec4 fragColor;

void main() {
    vec2 o = Texel.xy;
    vec3 s = texture(Source, vUv).rgb * 4.0;
    s += texture(Source, vUv - o).rgb;
    s += texture(Source, vUv + o).rgb;
    s += texture(Source, vUv + vec2(o.x, -o.y)).rgb;
    s += texture(Source, vUv - vec2(o.x, -o.y)).rgb;
    fragColor = vec4(s * 0.125, 1.0);
}
