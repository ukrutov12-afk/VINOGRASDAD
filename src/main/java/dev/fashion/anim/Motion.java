package dev.fashion.anim;

public enum Motion {
    FLOW(15f, 0.8f),
    FOLLOW(26f, 0.78f),
    HOVER(19f, 0.74f),
    PRESS(30f, 0.62f),
    TOGGLE(21f, 0.6f),
    PILL(17f, 0.66f),
    ARRIVAL(10.5f, 0.66f),
    DEPART(17f, 1f),
    FADE(12f, 1f),
    SOFT(6f, 1f),
    DRAG(24f, 0.72f);

    public final float omega;
    public final float zeta;

    Motion(float omega, float zeta) {
        this.omega = omega;
        this.zeta = zeta;
    }

    public static float stretch(float velocity, float k, float max) {
        return 1f + Math.min(Math.abs(velocity) * k, max);
    }

    public static float clamp01(float v) {
        return v < 0f ? 0f : Math.min(v, 1f);
    }

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    public static float smooth(float t) {
        t = clamp01(t);
        return t * t * (3f - 2f * t);
    }
}
