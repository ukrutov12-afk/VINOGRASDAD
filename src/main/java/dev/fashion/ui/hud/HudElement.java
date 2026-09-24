package dev.fashion.ui.hud;

import dev.fashion.anim.Motion;
import dev.fashion.anim.Spring;
import dev.fashion.gfx.Canvas;
import dev.fashion.gfx.Colors;
import dev.fashion.ui.Theme;

public abstract class HudElement {
    public final String id;
    public final String title;
    public float fx;
    public float fy;
    public final Spring x = new Spring(0f, Motion.DRAG);
    public final Spring y = new Spring(0f, Motion.DRAG);
    public final Spring w = new Spring(0f, Motion.FLOW);
    public final Spring h = new Spring(0f, Motion.FLOW);
    public final Spring presence = new Spring(0f, Motion.ARRIVAL);
    public final Spring hover = new Spring(0f, Motion.HOVER);
    public final Spring lift = new Spring(0f, Motion.HOVER);
    protected boolean placed;
    public boolean dragging;

    protected HudElement(String id, String title, float fx, float fy) {
        this.id = id;
        this.title = title;
        this.fx = fx;
        this.fy = fy;
    }

    public abstract boolean shown(boolean editing);

    public abstract float contentWidth();

    public abstract float contentHeight();

    public abstract void paintContent(Canvas c, float x, float y, float w, float h, float dt, boolean editing);

    public void tick() {
    }

    public float anchorX(float sw) {
        return fx * Math.max(0f, sw - w.target);
    }

    public float anchorY(float sh) {
        return fy * Math.max(0f, sh - h.target);
    }

    public void update(float dt, float sw, float sh, boolean editing) {
        presence.to(shown(editing) ? 1f : 0f);
        w.to(contentWidth());
        h.to(contentHeight());
        if (!placed) {
            w.snap(w.target);
            h.snap(h.target);
        }
        if (!dragging) {
            x.to(anchorX(sw));
            y.to(anchorY(sh));
        }
        if (!placed) {
            x.snap(x.target);
            y.snap(y.target);
            placed = true;
        }
        x.update(dt);
        y.update(dt);
        w.update(dt);
        h.update(dt);
        presence.update(dt);
        hover.update(dt);
        lift.to(dragging ? 1f : 0f);
        lift.update(dt);
    }

    public boolean contains(float mx, float my) {
        return presence.get() > 0.3f && mx >= x.get() && mx <= x.get() + w.get() && my >= y.get() && my <= y.get() + h.get();
    }

    public void paint(Canvas c, float dt, boolean editing) {
        float p = presence.get();
        if (p <= 0.004f) {
            return;
        }
        float ex = x.get();
        float ey = y.get();
        float ew = w.get();
        float eh = h.get();
        float speed = (float) Math.hypot(x.velocity, y.velocity);
        float sx = 1f;
        float sy = 1f;
        if (speed > 1f) {
            float amount = Math.min(speed * 0.00008f, 0.06f);
            float ax = Math.abs(x.velocity) / speed;
            float ay = Math.abs(y.velocity) / speed;
            sx = 1f + amount * (ax - 0.55f * ay);
            sy = 1f + amount * (ay - 0.55f * ax);
        }
        float s = (0.9f + 0.1f * p) * (1f + 0.03f * lift.get());
        c.push();
        c.scaleAround(ex + ew * 0.5f, ey + eh * 0.5f, s * sx, s * sy);
        c.translate(0f, (1f - p) * 8f - lift.get() * 2f);
        c.pushAlpha(Math.max(0f, Math.min(1f, p * 1.3f)));
        if (editing) {
            float hv = Math.max(hover.get(), lift.get());
            c.shape(ex - 3f, ey - 3f, ew + 6f, eh + 6f).radius(10f).fill(0)
                    .border(c.px() * 1.2f, Colors.withAlpha(Theme.ACCENT_HI, 0.25f + 0.5f * hv))
                    .glow(8f, Colors.withAlpha(Theme.GLOW, 0.12f + 0.3f * hv))
                    .draw();
        }
        paintContent(c, ex, ey, ew, eh, dt, editing);
        c.popAlpha();
        c.pop();
    }

    public static void panel(Canvas c, float x, float y, float w, float h, float r, float lift) {
        c.shape(x, y, w, h).radius(r).fill(0xC20B0914).glass()
                .border(c.px() * 1.1f, 0x4DFFFFFF).chrome(1f)
                .shadow(0f, 2f + 3f * lift, 6f + 6f * lift, 0.45f)
                .glow(8f, Colors.withAlpha(Theme.GLOW, 0.07f + 0.1f * lift))
                .sheen(0.04f)
                .draw();
    }
}
