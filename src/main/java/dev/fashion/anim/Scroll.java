package dev.fashion.anim;

public final class Scroll {
    private static final float FRICTION = 5.2f;
    private static final float NOTCH = 56f;
    private static final float EDGE_K = 210f;
    private static final float EDGE_D = 23f;

    public float offset;
    public float velocity;
    private float max;
    private float activity;

    public void bounds(float contentHeight, float viewHeight) {
        max = Math.max(0f, contentHeight - viewHeight);
    }

    public float max() {
        return max;
    }

    public float activity() {
        return activity;
    }

    public void wheel(float notches) {
        float over = overscroll();
        float resist = 1f / (1f + Math.abs(over) / 26f);
        float dir = -notches;
        if (over < 0f && dir < 0f || over > 0f && dir > 0f) {
            velocity += dir * NOTCH * FRICTION * resist * 0.6f;
        } else {
            velocity += dir * NOTCH * FRICTION;
        }
        activity = 1f;
    }

    public void reset() {
        offset = 0f;
        velocity = 0f;
    }

    public float overscroll() {
        if (offset < 0f) {
            return offset;
        }
        if (offset > max) {
            return offset - max;
        }
        return 0f;
    }

    public void update(float dt) {
        float remaining = dt;
        while (remaining > 0f) {
            float h = Math.min(remaining, 1f / 240f);
            remaining -= h;
            float over = overscroll();
            float acc = -velocity * FRICTION;
            if (over != 0f) {
                acc += -EDGE_K * over - EDGE_D * velocity;
            }
            velocity += acc * h;
            offset += velocity * h;
        }
        if (overscroll() == 0f && Math.abs(velocity) < 2f) {
            velocity = 0f;
        }
        if (Math.abs(overscroll()) < 0.05f && Math.abs(velocity) < 0.5f) {
            offset = Math.max(0f, Math.min(max, offset));
        }
        activity = Math.max(0f, activity - dt * 0.9f);
        if (Math.abs(velocity) > 20f) {
            activity = 1f;
        }
    }
}
