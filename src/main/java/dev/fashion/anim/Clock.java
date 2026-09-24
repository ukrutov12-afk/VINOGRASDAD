package dev.fashion.anim;

public final class Clock {
    private static long last = -1L;
    private static float time;
    private static float dt;
    private static float fixedStep = -1f;

    private Clock() {
    }

    public static void fixed(float step) {
        fixedStep = step;
    }

    public static float tick() {
        long now = System.nanoTime();
        if (fixedStep > 0f) {
            dt = fixedStep;
        } else if (last < 0L) {
            dt = 1f / 60f;
        } else {
            dt = Math.min((now - last) / 1.0e9f, 1f / 15f);
        }
        last = now;
        time += dt;
        return dt;
    }

    public static float dt() {
        return dt;
    }

    public static float time() {
        return time;
    }
}
