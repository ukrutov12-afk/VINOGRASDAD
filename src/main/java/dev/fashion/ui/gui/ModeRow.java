package dev.fashion.ui.gui;

import dev.fashion.anim.Motion;
import dev.fashion.anim.Spring;
import dev.fashion.core.setting.ModeSetting;
import dev.fashion.gfx.Canvas;
import dev.fashion.gfx.Colors;
import dev.fashion.gfx.Font;
import dev.fashion.ui.Theme;
import dev.fashion.ui.Ui;

public final class ModeRow extends Row {
    private final ModeSetting setting;
    private final Spring left;
    private final Spring right;
    private final Spring[] hovers;
    private int last;
    private float segX;
    private float segY;
    private float segW;
    private static final float SEG_H = 21f;

    public ModeRow(ModeSetting setting) {
        this.setting = setting;
        int n = setting.options().size();
        this.left = new Spring(setting.index() / (float) n, Motion.PILL);
        this.right = new Spring((setting.index() + 1) / (float) n, Motion.PILL);
        this.hovers = new Spring[n];
        for (int i = 0; i < n; i++) {
            hovers[i] = new Spring(0f, Motion.HOVER);
        }
        this.last = setting.index();
    }

    @Override
    protected boolean visibleNow() {
        return setting.visible();
    }

    @Override
    public float height(float width) {
        return 45f;
    }

    @Override
    protected void paint(Canvas c, float x, float y, float w, float mx, float my, float dt) {
        int n = setting.options().size();
        int idx = setting.index();
        if (idx != last) {
            boolean forward = idx > last;
            left.motion(forward ? 13f : 24f, forward ? 0.78f : 0.62f);
            right.motion(forward ? 24f : 13f, forward ? 0.62f : 0.78f);
            last = idx;
        }
        left.to(idx / (float) n);
        right.to((idx + 1) / (float) n);
        left.update(dt);
        right.update(dt);
        label(c, setting.name(), x, y + 10f, hover.get());
        segX = x;
        segY = y + 19f;
        segW = w;
        c.shape(segX, segY, segW, SEG_H).radius(7f).fill(Theme.INSET).border(c.px(), 0x16FFFFFF).draw();
        float pad = 2f;
        float inner = segW - pad * 2f;
        float pl = segX + pad + left.get() * inner;
        float pr = segX + pad + right.get() * inner;
        if (pr - pl > 2f) {
            c.shape(pl, segY + pad, pr - pl, SEG_H - pad * 2f).radius(5.5f)
                    .vertical(0xFF9A7DFF, Theme.ACCENT_DEEP)
                    .border(c.px(), Colors.withAlpha(Theme.ACCENT_HI, 0.55f))
                    .glow(7f, Colors.withAlpha(Theme.GLOW, 0.38f))
                    .sheen(0.12f)
                    .draw();
        }
        float cw = inner / n;
        for (int i = 0; i < n; i++) {
            float ox = segX + pad + cw * i;
            boolean over = Ui.inside(mx, my, ox, segY, cw, SEG_H);
            hovers[i].to(over ? 1f : 0f);
            hovers[i].update(dt);
            float centre = ox + cw * 0.5f;
            float cover = Math.max(0f, Math.min(1f, Math.min(centre - pl, pr - centre) / (cw * 0.35f) + 0.5f));
            int col = Colors.mix(Colors.mix(Theme.TEXT_3, Theme.TEXT_2, hovers[i].get()), 0xFFFFFFFF, cover);
            c.text(Font.medium(), 9f).color(col).drawMidCenter(setting.options().get(i), centre, segY + SEG_H * 0.5f);
        }
    }

    @Override
    public boolean mouseDown(float mx, float my, int button) {
        if (button != 0 || rh < 2f || !Ui.inside(mx, my, segX, segY, segW, SEG_H)) {
            return false;
        }
        int n = setting.options().size();
        int i = (int) ((mx - segX) / (segW / n));
        setting.set(Math.max(0, Math.min(n - 1, i)));
        return true;
    }

    @Override
    public String tip() {
        return setting.description();
    }
}
