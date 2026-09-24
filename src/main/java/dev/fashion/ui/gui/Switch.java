package dev.fashion.ui.gui;

import dev.fashion.anim.Motion;
import dev.fashion.anim.Spring;
import dev.fashion.gfx.Canvas;
import dev.fashion.gfx.Colors;
import dev.fashion.ui.Theme;

public final class Switch {
    private final Spring knob = new Spring(0f, Motion.TOGGLE);
    private final Spring hover = new Spring(0f, Motion.HOVER);
    private final Spring press = new Spring(0f, Motion.PRESS);
    private boolean init;

    public void kick() {
        press.impulse(-4f);
    }

    public void paint(Canvas c, float x, float y, float w, float h, boolean on, boolean hovered, float dt) {
        if (!init) {
            knob.snap(on ? 1f : 0f);
            init = true;
        }
        knob.to(on ? 1f : 0f);
        hover.to(hovered ? 1f : 0f);
        knob.update(dt);
        hover.update(dt);
        press.update(dt);
        float k = knob.get();
        float kc = Math.max(0f, Math.min(1f, k));
        float hv = hover.get();
        c.shape(x, y, w, h).radius(h * 0.5f)
                .horizontal(Colors.mix(0xFF0C0A14, Theme.ACCENT_DEEP, kc), Colors.mix(0xFF120F1C, Theme.ACCENT, kc))
                .border(c.px(), Colors.mix(Theme.LINE, Colors.withAlpha(Theme.ACCENT_HI, 0.7f), kc))
                .glow(7f, Colors.withAlpha(Theme.GLOW, 0.42f * kc + 0.12f * hv))
                .draw();
        float d = h - 4f;
        float travel = w - h;
        float stretch = Math.min(Math.abs(knob.velocity) * travel * 0.035f, d * 0.55f);
        float grow = 1f + 0.08f * hv + 0.12f * press.get();
        float kw = (d + stretch) * grow;
        float kh = d * grow / (1f + stretch / (d * 3f));
        float cx = x + h * 0.5f + travel * k;
        float cy = y + h * 0.5f;
        c.shape(cx - kw * 0.5f, cy - kh * 0.5f, kw, kh).radius(kh * 0.5f)
                .vertical(0xFFFFFFFF, Colors.mix(0xFFCFCBE0, 0xFFE9E3FF, kc))
                .shadow(0f, 1f, 3f, 0.45f)
                .glow(5f, Colors.withAlpha(Theme.ACCENT_HI, 0.55f * kc))
                .draw();
    }
}
