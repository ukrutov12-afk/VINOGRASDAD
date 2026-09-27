package dev.fashion.ui.gui;

import dev.fashion.anim.Motion;
import dev.fashion.anim.Spring;
import dev.fashion.core.setting.BoolSetting;
import dev.fashion.core.setting.ModeSetting;
import dev.fashion.core.setting.MultiSetting;
import dev.fashion.core.setting.NumberSetting;
import dev.fashion.core.setting.Setting;
import dev.fashion.gfx.Canvas;
import dev.fashion.gfx.Colors;
import dev.fashion.gfx.Font;
import dev.fashion.ui.Theme;
import dev.fashion.ui.Ui;

public abstract class Row {
    public final Spring shown = new Spring(1f, Motion.FLOW);
    public final Spring wave = new Spring(0f, 15f, 0.7f);
    protected final Spring hover = new Spring(0f, Motion.HOVER);
    protected float rx;
    protected float ry;
    protected float rw;
    protected float rh;

    public static Row of(Setting<?> s) {
        if (s instanceof BoolSetting b) {
            return new BoolRow(b);
        }
        if (s instanceof NumberSetting n) {
            return new SliderRow(n);
        }
        if (s instanceof ModeSetting m) {
            return new ModeRow(m);
        }
        if (s instanceof MultiSetting m) {
            return new MultiRow(m);
        }
        return null;
    }

    protected boolean visibleNow() {
        return true;
    }

    public void init() {
        shown.snap(visibleNow() ? 1f : 0f);
    }

    public abstract float height(float width);

    public float animatedHeight(float width) {
        return height(width) * Math.max(0f, Math.min(1f, shown.get()));
    }

    public void update(float dt) {
        shown.to(visibleNow() ? 1f : 0f);
        shown.update(dt);
        wave.update(dt);
    }

    public float left() {
        return rx;
    }

    public float width() {
        return rw;
    }

    public float labelY() {
        return ry + 10f;
    }

    public final void paintRow(Canvas c, float x, float y, float w, float mx, float my, float dt) {
        float full = height(w);
        float h = animatedHeight(w);
        rx = x;
        ry = y;
        rw = w;
        rh = h;
        hover.to(Ui.inside(mx, my, x, y, w, h) && h > 2f ? 1f : 0f);
        hover.update(dt);
        if (h < 0.5f) {
            return;
        }
        float wv = wave.get();
        float a = Math.max(0f, Math.min(1f, shown.get())) * Math.max(0f, Math.min(1f, wv * 1.3f));
        if (a < 0.003f) {
            return;
        }
        boolean partial = h < full - 0.25f;
        if (partial) {
            c.pushClip(x - 6f, y, w + 12f, h, 0f);
        }
        c.pushAlpha(a);
        c.push();
        c.translate((1f - wv) * -8f, (Math.min(1f, shown.get()) - 1f) * 6f + (1f - wv) * 9f);
        if (hover.get() > 0.01f) {
            c.shape(x - 6f, y, w + 12f, full).radius(8f).fill(Colors.withAlpha(0xFFFFFFFF, 0.025f * hover.get())).draw();
        }
        paint(c, x, y, w, mx, my, dt);
        c.pop();
        c.popAlpha();
        if (partial) {
            c.popClip();
        }
    }

    protected abstract void paint(Canvas c, float x, float y, float w, float mx, float my, float dt);

    public boolean mouseDown(float mx, float my, int button) {
        return false;
    }

    public void mouseUp(float mx, float my) {
    }

    public String tip() {
        return null;
    }

    public boolean hit(float mx, float my) {
        return rh > 2f && wave.get() > 0.5f && Ui.inside(mx, my, rx, ry, rw, rh);
    }

    protected static void label(Canvas c, String text, float x, float cy, float hover) {
        c.text(Font.medium(), 9.5f).color(Colors.mix(Theme.TEXT_2, Theme.TEXT, hover)).drawMid(text, x, cy);
    }
}
