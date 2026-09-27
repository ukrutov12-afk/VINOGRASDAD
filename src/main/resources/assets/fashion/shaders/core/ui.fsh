#version 330

layout(std140) uniform UiFrame {
    vec4 Screen;
    vec4 Params;
    vec4 Params2;
};

uniform sampler2D Atlas;
uniform sampler2D Backdrop;
uniform sampler2D BackdropLight;
uniform sampler2D Image;

in vec2 vLocal;
in vec3 vFillLab;
in float vFillA;
in vec4 vUv;
flat in vec4 vRect;
flat in vec4 vRadii;
flat in vec4 vLine;
flat in vec4 vGlow;
flat in vec4 vP1;
flat in vec4 vP2;
flat in vec4 vClip;

out vec4 fragColor;

const vec3 SHADOW = vec3(0.012, 0.006, 0.03);

vec3 toSrgb(vec3 c) {
    c = max(c, vec3(0.0));
    return mix(c * 12.92, 1.055 * pow(c, vec3(1.0 / 2.4)) - 0.055, step(0.0031308, c));
}

vec3 fromOklab(vec3 lab) {
    float l = lab.x + 0.3963377774 * lab.y + 0.2158037573 * lab.z;
    float m = lab.x - 0.1055613458 * lab.y - 0.0638541728 * lab.z;
    float s = lab.x - 0.0894841775 * lab.y - 1.2914855480 * lab.z;
    l = l * l * l;
    m = m * m * m;
    s = s * s * s;
    return vec3(
        4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
        -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
        -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s);
}

float sdRound(vec2 p, vec2 b, vec4 r) {
    float rr = p.x < 0.0 ? (p.y < 0.0 ? r.x : r.w) : (p.y < 0.0 ? r.y : r.z);
    vec2 q = abs(p) - b + rr;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - rr;
}

vec2 normalRound(vec2 p, vec2 b, vec4 r) {
    float rr = p.x < 0.0 ? (p.y < 0.0 ? r.x : r.w) : (p.y < 0.0 ? r.y : r.z);
    vec2 q = abs(p) - b + rr;
    vec2 s = vec2(p.x < 0.0 ? -1.0 : 1.0, p.y < 0.0 ? -1.0 : 1.0);
    if (q.x > 0.0 && q.y > 0.0) {
        return normalize(q) * s;
    }
    return q.x > q.y ? vec2(s.x, 0.0) : vec2(0.0, s.y);
}

float ign(vec2 p) {
    return fract(52.9829189 * fract(dot(p, vec2(0.06711056, 0.00583715))));
}

float erfApprox(float x) {
    float s = sign(x);
    float a = abs(x);
    float t = 1.0 + (0.278393 + (0.230389 + 0.078108 * (a * a)) * a) * a;
    t *= t;
    return s - s / (t * t);
}

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float vnoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), u.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x), u.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    mat2 r = mat2(0.8, 0.6, -0.6, 0.8);
    for (int i = 0; i < 5; i++) {
        v += a * vnoise(p);
        p = r * p * 2.03 + 11.7;
        a *= 0.5;
    }
    return v;
}

vec3 chrome(float ndl) {
    float t = ndl * 0.5 + 0.5;
    vec3 dark = vec3(0.20, 0.17, 0.30);
    vec3 mid = vec3(0.56, 0.56, 0.66);
    vec3 hi = vec3(0.97, 0.97, 1.0);
    vec3 c = t < 0.5 ? mix(dark, mid, smoothstep(0.0, 0.5, t)) : mix(mid, hi, smoothstep(0.5, 1.0, t));
    c += vec3(1.0) * 0.55 * pow(max(ndl, 0.0), 18.0);
    c += vec3(0.55, 0.45, 1.0) * 0.22 * pow(max(-ndl, 0.0), 2.0);
    return c;
}

vec3 chromeText(float t) {
    vec3 top = vec3(1.0, 1.0, 1.0);
    vec3 upper = vec3(0.80, 0.80, 0.90);
    vec3 horizon = vec3(0.42, 0.39, 0.56);
    vec3 low = vec3(0.86, 0.84, 0.98);
    if (t < 0.45) {
        return mix(top, upper, smoothstep(0.0, 0.45, t));
    }
    if (t < 0.56) {
        return mix(upper, horizon, smoothstep(0.45, 0.56, t));
    }
    return mix(horizon, low, smoothstep(0.56, 1.0, t));
}

float glint(vec2 frag) {
    float phase = fract(Params.x / 6.5);
    float s = (frag.x + frag.y * 0.55) / (Screen.x + Screen.y * 0.55);
    float c = phase * 1.9 - 0.45;
    float d = (s - c) / 0.045;
    return exp(-d * d);
}

float clouds(vec2 frag) {
    vec2 q = frag / (Screen.y * 0.22);
    float t = Params.x;
    vec2 drift = vec2(t * 0.011, -t * 0.004);
    float w = fbm(q + drift + vec2(fbm(q * 0.7 - drift * 1.3), fbm(q * 0.8 + drift)) * 1.1);
    return smoothstep(0.38, 0.92, w);
}

vec4 over(vec4 top, vec4 bottom) {
    return top + bottom * (1.0 - top.a);
}

vec4 shapeBody(float mode, vec4 fill, vec2 p, vec2 hsz) {
    if (mode < 0.5) {
        return fill;
    }
    if (mode < 1.5) {
        vec3 back = texture(Backdrop, gl_FragCoord.xy * Screen.zw).rgb;
        float ready = Params.y;
        float lum = dot(back, vec3(0.2126, 0.7152, 0.0722));
        back = mix(vec3(lum), back, 0.82);
        float presence = max(vUv.w, 1e-3);
        float tint = clamp(fill.a / presence, 0.0, 1.0);
        vec3 c = mix(back, fill.rgb, tint);
        c = mix(fill.rgb, c, ready);
        if (vUv.z > 0.0) {
            vec2 frag = vec2(gl_FragCoord.x, Screen.y - gl_FragCoord.y);
            float cl = clouds(frag);
            c += vec3(0.075, 0.062, 0.13) * cl * vUv.z;
        }
        float grain = (ign(gl_FragCoord.xy * 1.37 + 3.1) - 0.5) * 0.012;
        return vec4(c + grain, vUv.w * mix(max(tint, 0.94), 1.0, ready));
    }
    if (mode > 4.5) {
        vec3 back = texture(BackdropLight, gl_FragCoord.xy * Screen.zw).rgb;
        vec3 c = mix(back, fill.rgb, fill.a / max(vUv.w, 1e-3));
        return vec4(c, vUv.w * Params.y);
    }
    if (mode < 3.5) {
        vec4 t = texture(Image, vUv.xy);
        return vec4(t.rgb * fill.rgb, t.a * fill.a);
    }
    vec2 q = p / 46.0;
    float time = Params.x;
    vec2 drift = vec2(time * 0.018, time * 0.007);
    float w = fbm(q * 0.9 + drift + vec2(fbm(q + drift * 1.7), fbm(q - drift)) * 0.9);
    float cloud = smoothstep(0.30, 0.86, w);
    vec3 c = mix(fill.rgb, vLine.rgb, cloud * vLine.a);
    return vec4(c, fill.a);
}

void main() {
    vec2 frag = vec2(gl_FragCoord.x, Screen.y - gl_FragCoord.y);
    vec2 cc = (vClip.xy + vClip.zw) * 0.5;
    vec2 ch = max((vClip.zw - vClip.xy) * 0.5, vec2(0.0));
    float feather = floor(vRect.z / 1024.0);
    float clipR = vRect.z - feather * 1024.0;
    float clipD = sdRound(frag - cc, ch, vec4(clipR));
    float clip = feather > 0.0 ? clamp(-clipD / feather, 0.0, 1.0) : clamp(0.5 - clipD, 0.0, 1.0);
    if (clip <= 0.0) {
        discard;
    }

    float mode = vRect.w;
    vec4 fill = vec4(toSrgb(fromOklab(vFillLab)), vFillA);
    vec4 col = vec4(0.0);

    if (mode > 1.5 && mode < 2.5) {
        float s = texture(Atlas, vUv.xy).r;
        float sd = (s - 0.5) * 2.0 * Params2.x;
        float spx = 1.41421 / max(length(fwidth(vUv.xy * Params.zw)), 1e-5);
        float bias = vUv.z;
        float fc = clamp(sd * spx + 0.5 + bias, 0.0, 1.0);
        if (vP1.y > 0.0 && vGlow.a > 0.0) {
            float t = max(-sd, 0.0) / vP1.y;
            float g = (exp(-t * t * 2.0) * 0.8 + exp(-t * 2.6) * 0.2) * vGlow.a * (1.0 - fc);
            g *= smoothstep(Params2.x, Params2.x * 0.6, -sd);
            col = vec4(vGlow.rgb * g, g * 0.45);
        }
        if (vP1.x > 0.0 && vLine.a > 0.0) {
            float oc = clamp((sd + vP1.x) * spx + 0.5 + bias, 0.0, 1.0) * vLine.a;
            col = over(vec4(vLine.rgb * oc, oc), col);
        }
        vec3 rgb = fill.rgb;
        if (vP2.w > 0.0) {
            rgb = mix(rgb, rgb * chromeText(clamp(vLocal.y, 0.0, 1.0)), vP2.w);
            rgb += vec3(1.0, 0.97, 1.0) * glint(frag) * 0.85 * vP2.w;
        }
        float a = fc * fill.a;
        col = over(vec4(rgb * a, a), col);
    } else {
        vec2 p = vLocal;
        vec2 hsz = vRect.xy;
        float d = sdRound(p, hsz, vRadii);
        float aa = max(0.7071 * length(fwidth(p)), 1e-4);
        float cov = clamp(0.5 - d / aa, 0.0, 1.0);

        if (vP1.w > 0.0) {
            float blur = max(vP1.z, aa);
            float ds = sdRound(p - vP2.xy, hsz, vRadii);
            float sh = (0.5 - 0.5 * erfApprox(ds / (blur * 0.5))) * vP1.w * (1.0 - cov);
            col = vec4(SHADOW * sh, sh);
        }

        if (vP1.y > 0.0 && vGlow.a > 0.0) {
            float t = max(d, 0.0) / vP1.y;
            float g = (exp(-t * t * 2.4) * 0.72 + exp(-t * 3.2) * 0.28) * vGlow.a * (1.0 - cov);
            col = over(vec4(vGlow.rgb * g, g * 0.55), col);
        }

        if (cov > 0.0) {
            vec4 body = shapeBody(mode, fill, p, hsz);
            if (vP1.y > 0.0 && vGlow.a > 0.0) {
                float ig = exp(-max(-d, 0.0) / (vP1.y * 0.45)) * vGlow.a * 0.38;
                body.rgb += vGlow.rgb * ig;
                body.a = max(body.a, ig);
            }
            if (vP2.z > 0.0) {
                float tt = clamp((p.y + hsz.y) / max(2.0 * hsz.y, 1e-3), 0.0, 1.0);
                float sheen = vP2.z * pow(1.0 - tt, 2.2);
                body.rgb += vec3(0.86, 0.84, 1.0) * sheen;
                body.a = max(body.a, min(sheen * 2.0, 1.0) * step(0.001, body.a + sheen));
            }
            float lw = vP1.x;
            if (lw > 0.0 && vLine.a > 0.0) {
                float bd = abs(d + lw * 0.5) - lw * 0.5;
                float bc = clamp(0.5 - bd / aa, 0.0, 1.0) * min(lw / aa, 1.0) * vLine.a;
                vec3 lc = vLine.rgb;
                if (vP2.w > 0.0) {
                    vec2 n = normalRound(p, hsz, vRadii);
                    float ndl = dot(n, normalize(vec2(-0.42, -1.0)));
                    lc = mix(lc, lc * chrome(ndl), vP2.w);
                    lc += vec3(1.0, 0.97, 1.0) * glint(frag) * 1.1 * vP2.w * (0.55 + 0.45 * max(ndl, 0.0));
                }
                body.rgb = mix(body.rgb, lc, bc);
                body.a = max(body.a, bc);
            }
            float a = clamp(body.a, 0.0, 1.0) * cov;
            col = over(vec4(body.rgb * a, a), col);
        }
    }

    col *= clip;
    float n = ign(gl_FragCoord.xy) + ign(gl_FragCoord.xy + vec2(47.0, 17.0)) - 1.0;
    col.rgb += n / 255.0 * min(col.a * 12.0, 1.0);
    if (col.a <= 0.0 && dot(col.rgb, vec3(1.0)) <= 0.0) {
        discard;
    }
    fragColor = col;
}
