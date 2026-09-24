package dev.fashion.ui.gui;

import dev.fashion.anim.Motion;
import dev.fashion.anim.Spring;
import dev.fashion.core.setting.NumberSetting;
import dev.fashion.gfx.Canvas;
import dev.fashion.gfx.Colors;
import dev.fashion.gfx.Font;
import dev.fashion.ui.Theme;
import dev.fashion.ui.Ui;

public final class SliderRow extends Row {
    private final NumberSetting setting;
    private final Spring shownValue;
    private final Spring grab = new Spring(0f, Motion.HOVER);
    private boolean dragging;
    private float trackX;
    private float trackW;

    public SliderRow(NumberSetting setting) {
        this.setting = setting;
        this.shownValue = new Spring((float) setting.fraction(), Motion.FOLLOW);
    }

    @Override
    protected boolean visibleNow() {
        return setting.visible();
    }

    @Override
    public float height(float width) {
        return 38f;
    }

    @Override
    protected void paint(Canvas c, float x, float y, float w, float mx, float my, float dt) {
        if (dragging) {
            setting.setFraction((mx - trackX) / Math.max(1f, trackW));
        }
        shownValue.to((float) setting.fraction());
        shownValue.update(dt);
        float trackY = y + 27f;
        trackX = x;
        trackW = w;
        boolean overKnob = Ui.inside(mx, my, x - 4f, y + 18f, w + 8f, 18f);
        grab.to(dragging ? 1f : overKnob ? 0.5f : 0f);
        grab.update(dt);
        float f = shownValue.get();
        double shownNumber = setting.min() + Math.max(0f, Math.min(1f, f)) * (setting.max() - setting.min());
        double snapped = Math.round(shownNumber / setting.step()) * setting.step();
        label(c, setting.name(), x, y + 10f, hover.get());
        c.text(Font.semibold(), 9.5f).color(Colors.mix(Theme.TEXT_2, Theme.ACCENT_HI, grab.get()))
                .drawMidRight(setting.format(snapped), x + w, y + 10f);
        float th = 4f;
        c.shape(x, trackY - th * 0.5f, w, th).radius(th * 0.5f).fill(Theme.INSET).border(c.px(), 0x16FFFFFF).draw();
        float fx = Math.max(0f, Math.min(1f, f)) * w;
        if (fx > 0.5f) {
            c.shape(x, trackY - th * 0.5f, fx, th).radius(th * 0.5f)
                    .horizontal(Theme.ACCENT_DEEP, Theme.ACCENT)
                    .glow(6f, Colors.withAlpha(Theme.GLOW, 0.35f + 0.25f * grab.get()))
                    .draw();
        }
        float stretch = Math.min(Math.abs(shownValue.velocity) * w * 0.004f, 6f);
        float kd = 9f + 3f * grab.get();
        float kx = x + f * w;
        c.shape(kx - (kd + stretch) * 0.5f, trackY - kd * 0.5f, kd + stretch, kd).radius(kd * 0.5f)
                .vertical(0xFFFFFFFF, 0xFFD8D2F0)
                .shadow(0f, 1f, 3f, 0.5f)
                .glow(6f + 4f * grab.get(), Colors.withAlpha(Theme.ACCENT_HI, 0.35f + 0.35f * grab.get()))
                .draw();
    }

    @Override
    public boolean mouseDown(float mx, float my, int button) {
        if (button == 0 && rh > 2f && Ui.inside(mx, my, rx - 4f, ry + 16f, rw + 8f, 20f)) {
            dragging = true;
            setting.setFraction((mx - trackX) / Math.max(1f, trackW));
            return true;
        }
        return false;
    }

    @Override
    public void mouseUp(float mx, float my) {
        dragging = false;
    }
}
