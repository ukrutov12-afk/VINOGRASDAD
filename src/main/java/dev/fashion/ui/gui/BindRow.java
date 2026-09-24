package dev.fashion.ui.gui;

import dev.fashion.anim.Clock;
import dev.fashion.anim.Motion;
import dev.fashion.anim.Spring;
import dev.fashion.core.Keys;
import dev.fashion.core.Module;
import dev.fashion.gfx.Canvas;
import dev.fashion.gfx.Colors;
import dev.fashion.gfx.Font;
import dev.fashion.ui.Draw;
import dev.fashion.ui.FadeText;
import dev.fashion.ui.Theme;
import dev.fashion.ui.Ui;

public final class BindRow extends Row {
    private final Module module;
    private final Spring listen = new Spring(0f, Motion.HOVER);
    private final Spring width = new Spring(0f, Motion.FLOW);
    private final FadeText text;
    private boolean listening;
    private float chipX;
    private float chipW;

    public BindRow(Module module) {
        this.module = module;
        this.text = new FadeText(Keys.name(module.key()));
    }

    public boolean listening() {
        return listening;
    }

    public void stopListening() {
        listening = false;
    }

    public void assign(int key) {
        module.setKey(key);
        listening = false;
    }

    @Override
    public float height(float width) {
        return 28f;
    }

    @Override
    protected void paint(Canvas c, float x, float y, float w, float mx, float my, float dt) {
        text.set(listening ? "…" : Keys.name(module.key()));
        text.update(dt);
        listen.to(listening ? 1f : 0f);
        listen.update(dt);
        Draw.icon(c, '\uE00B', x + 6f, y + 14f, 11f, Colors.mix(Theme.TEXT_3, Theme.TEXT_2, hover.get()));
        label(c, "Клавиша", x + 16f, y + 14f, hover.get());
        float target = Math.max(34f, Font.semibold().width(text.current(), 9f, 0.04f) + 20f);
        if (width.get() == 0f) {
            width.snap(target);
        }
        width.to(target);
        width.update(dt);
        chipW = width.get();
        chipX = x + w - chipW;
        float cy = y + 4f;
        float l = listen.get();
        float pulse = l * (0.55f + 0.45f * (float) Math.sin(Clock.time() * 5.0));
        c.shape(chipX, cy, chipW, 20f).radius(6f)
                .vertical(Colors.mix(0xFF15121F, 0xFF2B1F5E, l), Colors.mix(0xFF0C0A13, 0xFF1C1440, l))
                .border(c.px(), Colors.mix(0x26FFFFFF, Colors.withAlpha(Theme.ACCENT_HI, 0.7f), l))
                .glow(8f, Colors.withAlpha(Theme.GLOW, 0.45f * pulse))
                .shadow(0f, 1f, 3f, 0.35f)
                .draw();
        float p = text.progress();
        float mid = cy + 10f;
        if (text.previous() != null) {
            c.pushAlpha(1f - p);
            c.text(Font.semibold(), 9f).tracking(0.04f).color(Theme.TEXT).drawMidCenter(text.previous(), chipX + chipW * 0.5f, mid - p * 6f);
            c.popAlpha();
        }
        c.pushAlpha(p);
        c.text(Font.semibold(), 9f).tracking(0.04f).color(Colors.mix(Theme.TEXT, Theme.ACCENT_HI, l))
                .drawMidCenter(text.current(), chipX + chipW * 0.5f, mid + (1f - p) * 6f);
        c.popAlpha();
    }

    @Override
    public boolean mouseDown(float mx, float my, int button) {
        if (rh < 2f) {
            return false;
        }
        if (Ui.inside(mx, my, chipX, ry + 4f, chipW, 20f)) {
            if (button == 1) {
                module.setKey(-1);
                listening = false;
            } else {
                listening = !listening;
            }
            return true;
        }
        return false;
    }

    @Override
    public String tip() {
        return "ЛКМ — назначить клавишу, ПКМ — сбросить";
    }
}
