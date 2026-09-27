package dev.fashion.ui.gui;

import dev.fashion.anim.Clock;
import dev.fashion.anim.Motion;
import dev.fashion.anim.Spring;
import dev.fashion.core.Keys;
import dev.fashion.core.Module;
import dev.fashion.gfx.Canvas;
import dev.fashion.gfx.Colors;
import dev.fashion.gfx.Font;
import dev.fashion.ui.FadeText;
import dev.fashion.ui.Theme;
import dev.fashion.ui.Ui;

public final class BindChip {
    private static final float H = 16f;
    private final Module module;
    private final Spring listen = new Spring(0f, Motion.HOVER);
    private final Spring hover = new Spring(0f, Motion.HOVER);
    private final Spring press = new Spring(0f, Motion.PRESS);
    private final Spring width = new Spring(0f, Motion.PILL);
    private final FadeText text;
    private boolean listening;
    private float cx;
    private float cy;

    public BindChip(Module module) {
        this.module = module;
        this.text = new FadeText(label());
    }

    private String label() {
        return listening ? "···" : module.key() >= 0 ? Keys.name(module.key()) : "—";
    }

    public boolean listening() {
        return listening;
    }

    public void listen() {
        listening = true;
    }

    public void stop() {
        listening = false;
    }

    public void assign(int key) {
        listening = false;
        module.setKey(key);
        press.impulse(-5f);
    }

    public float targetWidth() {
        return Math.max(20f, Font.semibold().width(label(), 8f, 0.04f) + 12f);
    }

    public float width() {
        return width.get();
    }

    public void paint(Canvas c, float right, float centerY, float mx, float my, float dt) {
        text.set(label());
        text.update(dt);
        float tw = targetWidth();
        if (width.get() == 0f) {
            width.snap(tw);
        }
        width.to(tw);
        width.update(dt);
        listen.to(listening ? 1f : 0f);
        listen.update(dt);
        press.update(dt);
        float w = width.get();
        cx = right - w;
        cy = centerY - H * 0.5f;
        hover.to(Ui.inside(mx, my, cx - 2f, cy - 2f, w + 4f, H + 4f) ? 1f : 0f);
        hover.update(dt);
        float l = listen.get();
        float hv = hover.get();
        float pulse = l * (0.55f + 0.45f * (float) Math.sin(Clock.time() * 5.2));
        float s = 1f + 0.04f * hv + 0.06f * press.get() + 0.03f * pulse;
        c.push();
        c.scaleAround(cx + w * 0.5f, centerY, s, s);
        c.shape(cx, cy + 1.2f, w, H).radius(5f).fill(0xFF050409).draw();
        c.shape(cx, cy, w, H).radius(5f)
                .vertical(Colors.mix(0xFF1D1A28, 0xFF2F2260, l), Colors.mix(0xFF121019, 0xFF1C1440, l))
                .border(c.px(), Colors.mix(Colors.mix(0x2EFFFFFF, 0x55FFFFFF, hv), Colors.withAlpha(Theme.ACCENT_HI, 0.8f), l))
                .chrome(0.6f)
                .glow(7f, Colors.withAlpha(Theme.GLOW, 0.55f * pulse + 0.12f * hv))
                .sheen(0.06f)
                .draw();
        float p = text.progress();
        if (text.previous() != null) {
            c.pushAlpha(1f - Math.min(1f, p));
            c.text(Font.semibold(), 8f).tracking(0.04f).color(Theme.TEXT_2).drawMidCenter(text.previous(), cx + w * 0.5f, centerY - p * 5f);
            c.popAlpha();
        }
        c.pushAlpha(Math.max(0f, Math.min(1f, p)));
        int col = module.key() >= 0 || listening ? Colors.mix(Theme.TEXT, Theme.ACCENT_HI, l) : Theme.TEXT_3;
        c.text(Font.semibold(), 8f).tracking(0.04f).color(col).drawMidCenter(text.current(), cx + w * 0.5f, centerY + (1f - p) * 5f);
        c.popAlpha();
        c.pop();
    }

    public boolean mouseDown(float mx, float my, int button) {
        if (!Ui.inside(mx, my, cx - 2f, cy - 2f, width.get() + 4f, H + 4f)) {
            return false;
        }
        if (button == 1) {
            listening = false;
            module.setKey(-1);
            press.impulse(-5f);
        } else {
            listening = !listening;
            press.impulse(-3f);
        }
        return true;
    }
}
