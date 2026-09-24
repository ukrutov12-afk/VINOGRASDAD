package dev.fashion.ui.gui;

import dev.fashion.anim.Motion;
import dev.fashion.anim.Spring;
import dev.fashion.core.setting.MultiSetting;
import dev.fashion.gfx.Canvas;
import dev.fashion.gfx.Colors;
import dev.fashion.gfx.Font;
import dev.fashion.ui.Draw;
import dev.fashion.ui.Theme;
import dev.fashion.ui.Ui;

public final class MultiRow extends Row {
    private static final float CHIP_H = 20f;
    private static final float GAP = 5f;
    private final MultiSetting setting;
    private final Spring[] on;
    private final Spring[] hovers;
    private final Spring[] press;
    private final float[] cx;
    private final float[] cy;
    private final float[] cw;

    public MultiRow(MultiSetting setting) {
        this.setting = setting;
        int n = setting.options().size();
        on = new Spring[n];
        hovers = new Spring[n];
        press = new Spring[n];
        cx = new float[n];
        cy = new float[n];
        cw = new float[n];
        for (int i = 0; i < n; i++) {
            on[i] = new Spring(setting.has(i) ? 1f : 0f, Motion.TOGGLE);
            hovers[i] = new Spring(0f, Motion.HOVER);
            press[i] = new Spring(0f, Motion.PRESS);
        }
    }

    @Override
    protected boolean visibleNow() {
        return setting.visible();
    }

    private float chipWidth(int i) {
        return Font.medium().width(setting.options().get(i), 9f, 0f) + 30f;
    }

    private int layout(float width) {
        float px = 0f;
        int line = 0;
        for (int i = 0; i < setting.options().size(); i++) {
            float w = chipWidth(i);
            if (px > 0f && px + w > width) {
                px = 0f;
                line++;
            }
            cx[i] = px;
            cy[i] = line * (CHIP_H + GAP);
            cw[i] = w;
            px += w + GAP;
        }
        return line + 1;
    }

    @Override
    public float height(float width) {
        return 22f + layout(width) * (CHIP_H + GAP);
    }

    @Override
    protected void paint(Canvas c, float x, float y, float w, float mx, float my, float dt) {
        label(c, setting.name(), x, y + 10f, hover.get());
        layout(w);
        for (int i = 0; i < setting.options().size(); i++) {
            float bx = x + cx[i];
            float by = y + 20f + cy[i];
            boolean over = Ui.inside(mx, my, bx, by, cw[i], CHIP_H);
            on[i].to(setting.has(i) ? 1f : 0f);
            hovers[i].to(over ? 1f : 0f);
            on[i].update(dt);
            hovers[i].update(dt);
            press[i].update(dt);
            float k = on[i].get();
            float kc = Math.max(0f, Math.min(1f, k));
            float hv = hovers[i].get();
            float s = 1f + 0.03f * hv + 0.08f * press[i].get();
            c.push();
            c.scaleAround(bx + cw[i] * 0.5f, by + CHIP_H * 0.5f, s, s);
            c.translate(0f, -hv * 0.8f);
            c.shape(bx, by, cw[i], CHIP_H).radius(CHIP_H * 0.5f)
                    .vertical(Colors.mix(0xFF0E0C17, 0xFF3A2A7A, kc), Colors.mix(0xFF0A0912, 0xFF261A57, kc))
                    .border(c.px(), Colors.mix(0x22FFFFFF, Colors.withAlpha(Theme.ACCENT_HI, 0.6f), kc))
                    .glow(6f, Colors.withAlpha(Theme.GLOW, 0.3f * kc + 0.1f * hv))
                    .shadow(0f, 1f + hv, 3f + 2f * hv, 0.3f)
                    .draw();
            float iconA = kc;
            float ix = bx + 11f;
            if (iconA > 0.01f) {
                c.push();
                c.scaleAround(ix, by + CHIP_H * 0.5f, 0.6f + 0.4f * k, 0.6f + 0.4f * k);
                c.pushAlpha(iconA);
                Draw.icon(c, '\uE009', ix, by + CHIP_H * 0.5f, 9f, Theme.ACCENT_HI);
                c.popAlpha();
                c.pop();
            }
            if (1f - iconA > 0.01f) {
                c.pushAlpha(1f - iconA);
                c.shape(ix - 2f, by + CHIP_H * 0.5f - 2f, 4f, 4f).radius(2f).fill(Theme.TEXT_3).draw();
                c.popAlpha();
            }
            c.text(Font.medium(), 9f).color(Colors.mix(Colors.mix(Theme.TEXT_3, Theme.TEXT_2, hv), Theme.TEXT, kc))
                    .drawMid(setting.options().get(i), bx + 20f, by + CHIP_H * 0.5f);
            c.pop();
        }
    }

    @Override
    public boolean mouseDown(float mx, float my, int button) {
        if (button != 0 || rh < 2f) {
            return false;
        }
        for (int i = 0; i < setting.options().size(); i++) {
            float bx = rx + cx[i];
            float by = ry + 20f + cy[i];
            if (Ui.inside(mx, my, bx, by, cw[i], CHIP_H)) {
                setting.toggle(i);
                press[i].impulse(-5f);
                return true;
            }
        }
        return false;
    }
}
