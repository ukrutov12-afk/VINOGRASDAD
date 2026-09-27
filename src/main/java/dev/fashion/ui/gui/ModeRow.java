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
        this.left = new Spring(0f, Motion.PILL);
        this.right = new Spring(0f, Motion.PILL);
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

    private boolean fits(float w) {
        float need = 4f;
        for (String o : setting.options()) {
            need += Font.medium().width(o, 9f, 0f) + 14f;
        }
        return need <= w;
    }

    private int perRow(float w) {
        return fits(w) ? setting.options().size() : 2;
    }

    private void cell(int i, float x, float y, float w, float[] out) {
        int n = setting.options().size();
        if (fits(w)) {
            float inner = w - 4f;
            float cw = inner / n;
            out[0] = x + 2f + cw * i;
            out[1] = y + 19f + 2f;
            out[2] = cw;
            out[3] = SEG_H - 4f;
            return;
        }
        int cols = perRow(w);
        float cw = (w - 5f * (cols - 1)) / cols;
        out[0] = x + (i % cols) * (cw + 5f);
        out[1] = y + 19f + (i / cols) * (SEG_H + 5f);
        out[2] = cw;
        out[3] = SEG_H;
    }

    @Override
    public float height(float width) {
        if (fits(width)) {
            return 45f;
        }
        int rows = (setting.options().size() + 1) / 2;
        return 19f + rows * (SEG_H + 5f) + 2f;
    }

    private final float[] tmp = new float[4];
    private final Spring top = new Spring(-1f, Motion.PILL);
    private float lastW = -1f;

    @Override
    protected void paint(Canvas c, float x, float y, float w, float mx, float my, float dt) {
        int n = setting.options().size();
        int idx = setting.index();
        boolean grid = !fits(w);
        cell(idx, x, y, w, tmp);
        float tl = tmp[0] - x;
        float tr = tmp[0] + tmp[2] - x;
        float tt = tmp[1] - y;
        float th = tmp[3];
        if (idx != last) {
            boolean forward = tl > left.target;
            left.motion(forward ? 13f : 24f, forward ? 0.78f : 0.62f);
            right.motion(forward ? 24f : 13f, forward ? 0.62f : 0.78f);
            last = idx;
        }
        if (top.target < 0f || lastW != w) {
            left.snap(tl);
            right.snap(tr);
            top.snap(tt);
            lastW = w;
        }
        left.to(tl);
        right.to(tr);
        top.to(tt);
        left.update(dt);
        right.update(dt);
        top.update(dt);
        label(c, setting.name(), x, y + 10f, hover.get());
        segX = x;
        segY = y + 19f;
        segW = w;
        if (!grid) {
            c.shape(x, y + 19f, w, SEG_H).radius(7f).vertical(Theme.INSET, Theme.INSET_TOP).border(c.px(), 0x16FFFFFF).draw();
        } else {
            for (int i = 0; i < n; i++) {
                cell(i, x, y, w, tmp);
                c.shape(tmp[0], tmp[1], tmp[2], tmp[3]).radius(7f).vertical(Theme.INSET, Theme.INSET_TOP).border(c.px(), 0x16FFFFFF).draw();
            }
        }
        float pl = x + left.get();
        float pr = x + right.get();
        float pt = y + top.get();
        if (pr - pl > 2f) {
            c.shape(pl, pt, pr - pl, th).radius(grid ? 7f : 5.5f)
                    .vertical(0xFF9A7DFF, Theme.ACCENT_DEEP)
                    .border(c.px(), Colors.withAlpha(Theme.ACCENT_HI, 0.6f))
                    .chrome(0.4f)
                    .glow(7f, Colors.withAlpha(Theme.GLOW, 0.38f))
                    .sheen(0.12f)
                    .draw();
        }
        for (int i = 0; i < n; i++) {
            cell(i, x, y, w, tmp);
            boolean over = Ui.inside(mx, my, tmp[0], tmp[1], tmp[2], tmp[3]);
            hovers[i].to(over ? 1f : 0f);
            hovers[i].update(dt);
            float centre = tmp[0] + tmp[2] * 0.5f;
            float middle = tmp[1] + tmp[3] * 0.5f;
            float coverX = Math.max(0f, Math.min(1f, Math.min(centre - pl, pr - centre) / (tmp[2] * 0.35f) + 0.5f));
            float coverY = Math.max(0f, 1f - Math.abs(middle - (pt + th * 0.5f)) / (th * 0.8f));
            float cover = coverX * coverY;
            int col = Colors.mix(Colors.mix(Theme.TEXT_3, Theme.TEXT_2, hovers[i].get()), 0xFFFFFFFF, cover);
            c.text(Font.medium(), 9f).color(col).drawMidCenter(setting.options().get(i), centre, middle);
        }
    }

    @Override
    public boolean mouseDown(float mx, float my, int button) {
        if (button != 0 || rh < 2f) {
            return false;
        }
        for (int i = 0; i < setting.options().size(); i++) {
            cell(i, rx, ry, rw, tmp);
            if (Ui.inside(mx, my, tmp[0], tmp[1], tmp[2], tmp[3])) {
                setting.set(i);
                return true;
            }
        }
        return false;
    }

    @Override
    public String tip() {
        return setting.description();
    }
}
