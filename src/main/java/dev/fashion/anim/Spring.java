package dev.fashion.anim;

public final class Spring {
    public float value;
    public float velocity;
    public float target;
    private float omega;
    private float zeta;
    private float delay;
    private float pending;
    private boolean hasPending;

    public Spring(float value, float omega, float zeta) {
        this.value = value;
        this.target = value;
        this.omega = omega;
        this.zeta = zeta;
    }

    public Spring(float value, Motion motion) {
        this(value, motion.omega, motion.zeta);
    }

    public Spring motion(Motion m) {
        omega = m.omega;
        zeta = m.zeta;
        return this;
    }

    public Spring motion(float omega, float zeta) {
        this.omega = omega;
        this.zeta = zeta;
        return this;
    }

    public float get() {
        return value;
    }

    public void to(float t) {
        hasPending = false;
        target = t;
    }

    public void to(float t, float delaySeconds) {
        if (delaySeconds <= 0f) {
            to(t);
            return;
        }
        if (hasPending && pending == t) {
            return;
        }
        if (!hasPending && target == t) {
            return;
        }
        pending = t;
        delay = delaySeconds;
        hasPending = true;
    }

    public float destination() {
        return hasPending ? pending : target;
    }

    public void snap(float v) {
        value = v;
        target = v;
        velocity = 0f;
        hasPending = false;
    }

    public void impulse(float dv) {
        velocity += dv;
    }

    public boolean settled() {
        return !hasPending && Math.abs(value - target) < 1e-3f && Math.abs(velocity) < 1e-3f;
    }

    public float update(float dt) {
        if (hasPending) {
            delay -= dt;
            if (delay <= 0f) {
                float spill = -delay;
                step(dt - spill);
                target = pending;
                hasPending = false;
                step(spill);
                return value;
            }
        }
        step(dt);
        return value;
    }

    private void step(float t) {
        if (t <= 0f) {
            return;
        }
        float x0 = value - target;
        float v0 = velocity;
        if (Math.abs(x0) < 1e-5f && Math.abs(v0) < 1e-5f) {
            value = target;
            velocity = 0f;
            return;
        }
        float w = omega;
        if (zeta < 0.999f) {
            float wd = w * (float) Math.sqrt(1f - zeta * zeta);
            float e = (float) Math.exp(-zeta * w * t);
            float c = (float) Math.cos(wd * t);
            float s = (float) Math.sin(wd * t);
            float x = e * (x0 * c + (v0 + zeta * w * x0) / wd * s);
            float v = e * (v0 * c - (zeta * w * v0 + w * w * x0) / wd * s);
            value = target + x;
            velocity = v;
        } else if (zeta <= 1.001f) {
            float e = (float) Math.exp(-w * t);
            float b = v0 + w * x0;
            value = target + (x0 + b * t) * e;
            velocity = (v0 - w * b * t) * e;
        } else {
            float r = (float) Math.sqrt(zeta * zeta - 1f);
            float r1 = -w * (zeta - r);
            float r2 = -w * (zeta + r);
            float c1 = (v0 - r2 * x0) / (r1 - r2);
            float c2 = x0 - c1;
            float e1 = (float) Math.exp(r1 * t);
            float e2 = (float) Math.exp(r2 * t);
            value = target + c1 * e1 + c2 * e2;
            velocity = c1 * r1 * e1 + c2 * r2 * e2;
        }
    }
}
