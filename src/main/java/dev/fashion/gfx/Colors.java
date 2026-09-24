package dev.fashion.gfx;

public final class Colors {
    private Colors() {
    }

    public static int rgb(int rgb) {
        return 0xFF000000 | rgb;
    }

    public static int argb(int a, int r, int g, int b) {
        return (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | (b & 255);
    }

    public static int alpha(int c) {
        return c >>> 24;
    }

    public static int withAlpha(int c, float a) {
        int ia = Math.round(Math.max(0f, Math.min(1f, a)) * 255f);
        return ia << 24 | (c & 0xFFFFFF);
    }

    public static int mulAlpha(int c, float f) {
        int a = Math.round((c >>> 24) * Math.max(0f, Math.min(1f, f)));
        return a << 24 | (c & 0xFFFFFF);
    }

    private static float toLinear(float c) {
        return c <= 0.04045f ? c / 12.92f : (float) Math.pow((c + 0.055f) / 1.055f, 2.4f);
    }

    private static float toSrgb(float c) {
        c = Math.max(0f, c);
        return c <= 0.0031308f ? c * 12.92f : 1.055f * (float) Math.pow(c, 1.0 / 2.4) - 0.055f;
    }

    public static void toOklab(int c, float[] out) {
        float r = toLinear(((c >> 16) & 255) / 255f);
        float g = toLinear(((c >> 8) & 255) / 255f);
        float b = toLinear((c & 255) / 255f);
        float l = (float) Math.cbrt(0.4122214708f * r + 0.5363325363f * g + 0.0514459929f * b);
        float m = (float) Math.cbrt(0.2119034982f * r + 0.6806995451f * g + 0.1073969566f * b);
        float s = (float) Math.cbrt(0.0883024619f * r + 0.2817188376f * g + 0.6299787005f * b);
        out[0] = 0.2104542553f * l + 0.7936177850f * m - 0.0040720468f * s;
        out[1] = 1.9779984951f * l - 2.4285922050f * m + 0.4505937099f * s;
        out[2] = 0.0259040371f * l + 0.7827717662f * m - 0.8086757660f * s;
        out[3] = (c >>> 24) / 255f;
    }

    public static int fromOklab(float L, float A, float B, float alpha) {
        float l = L + 0.3963377774f * A + 0.2158037573f * B;
        float m = L - 0.1055613458f * A - 0.0638541728f * B;
        float s = L - 0.0894841775f * A - 1.2914855480f * B;
        l = l * l * l;
        m = m * m * m;
        s = s * s * s;
        float r = 4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s;
        float g = -1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s;
        float b = -0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s;
        return argb(Math.round(Math.max(0f, Math.min(1f, alpha)) * 255f),
                Math.round(Math.min(1f, toSrgb(r)) * 255f),
                Math.round(Math.min(1f, toSrgb(g)) * 255f),
                Math.round(Math.min(1f, toSrgb(b)) * 255f));
    }

    private static final float[] A = new float[4];
    private static final float[] B = new float[4];

    public static int mix(int a, int b, float t) {
        if (t <= 0f) {
            return a;
        }
        if (t >= 1f) {
            return b;
        }
        toOklab(a, A);
        toOklab(b, B);
        float aa = A[3];
        float ba = B[3];
        if (aa < 0.001f) {
            A[0] = B[0];
            A[1] = B[1];
            A[2] = B[2];
        } else if (ba < 0.001f) {
            B[0] = A[0];
            B[1] = A[1];
            B[2] = A[2];
        }
        return fromOklab(A[0] + (B[0] - A[0]) * t, A[1] + (B[1] - A[1]) * t, A[2] + (B[2] - A[2]) * t, aa + (ba - aa) * t);
    }

    public static int oklch(float L, float C, float hueDeg, float alpha) {
        double h = Math.toRadians(hueDeg);
        return fromOklab(L, (float) (C * Math.cos(h)), (float) (C * Math.sin(h)), alpha);
    }
}
