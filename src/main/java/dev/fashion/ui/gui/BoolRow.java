package dev.fashion.ui.gui;

import dev.fashion.core.setting.BoolSetting;
import dev.fashion.gfx.Canvas;
import dev.fashion.ui.Ui;

public final class BoolRow extends Row {
    private final BoolSetting setting;
    private final Switch sw = new Switch();

    public BoolRow(BoolSetting setting) {
        this.setting = setting;
    }

    @Override
    protected boolean visibleNow() {
        return setting.visible();
    }

    @Override
    public float height(float width) {
        return 26f;
    }

    @Override
    protected void paint(Canvas c, float x, float y, float w, float mx, float my, float dt) {
        label(c, setting.name(), x, y + 13f, hover.get());
        float sw2 = 24f;
        float sh = 13f;
        sw.paint(c, x + w - sw2, y + 13f - sh * 0.5f, sw2, sh, setting.on(), Ui.inside(mx, my, rx, ry, rw, rh), dt);
    }

    @Override
    public boolean mouseDown(float mx, float my, int button) {
        if (button == 0 && hit(mx, my)) {
            setting.toggle();
            sw.kick();
            return true;
        }
        return false;
    }
}
