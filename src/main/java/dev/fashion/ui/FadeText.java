package dev.fashion.ui;

import dev.fashion.anim.Motion;
import dev.fashion.anim.Spring;

public final class FadeText {
    private String current;
    private String previous;
    private final Spring t = new Spring(1f, Motion.FLOW);

    public FadeText(String initial) {
        current = initial;
    }

    public void set(String s) {
        if (s.equals(current)) {
            return;
        }
        previous = current;
        current = s;
        t.snap(0f);
        t.to(1f);
    }

    public void update(float dt) {
        t.update(dt);
        if (t.settled()) {
            previous = null;
        }
    }

    public String current() {
        return current;
    }

    public String previous() {
        return previous;
    }

    public float progress() {
        return t.get();
    }
}
