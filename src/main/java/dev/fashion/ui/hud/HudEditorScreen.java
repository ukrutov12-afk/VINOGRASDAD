package dev.fashion.ui.hud;

import dev.fashion.anim.Motion;
import dev.fashion.anim.Spring;
import dev.fashion.gfx.Canvas;
import dev.fashion.gfx.Colors;
import dev.fashion.gfx.Font;
import dev.fashion.ui.Draw;
import dev.fashion.ui.FashionScreen;
import dev.fashion.ui.Theme;
import dev.fashion.ui.Ui;

public final class HudEditorScreen extends FashionScreen {
    private final Spring resetHover = new Spring(0f, Motion.HOVER);
    private final Spring resetPress = new Spring(0f, Motion.PRESS);
    private float px;
    private float py;
    private float pw;
    private float ph;

    public HudEditorScreen() {
        super("HUD");
    }

    @Override
    protected float layoutScale(int fbW, int fbH) {
        return HudLayer.scale();
    }

    @Override
    protected void paint(Canvas c, float dt, float sw, float sh) {
        HudLayer.drag(mouseX, mouseY);
        HudLayer.hover(mouseX, mouseY);
        float p = open.get();
        String title = "Редактор HUD";
        String hint = "Перетаскивайте элементы · они прилипают к краям и друг к другу · Esc — выход";
        float tw = Font.semibold().width(title, 10f, 0f);
        float hw = Font.regular().width(hint, 8.5f, 0f);
        pw = 30f + tw + 14f + hw + 16f + 56f;
        ph = 28f;
        px = (sw - pw) * 0.5f;
        py = 10f + (1f - p) * -30f;
        c.pushAlpha(Math.max(0f, Math.min(1f, p * 1.4f)));
        c.shape(px, py, pw, ph).radius(10f).fill(0xD00B0914).glass().border(c.px() * 1.1f, 0x4DFFFFFF).chrome(1f)
                .shadow(0f, 3f, 10f, 0.5f).glow(10f, Colors.withAlpha(Theme.GLOW, 0.12f)).draw();
        Draw.icon(c, '\uE007', px + 16f, py + ph * 0.5f, 11f, Theme.ACCENT_HI, 3f, Colors.withAlpha(Theme.GLOW, 0.6f));
        float x = px + 29f;
        x += c.text(Font.semibold(), 10f).color(Theme.TEXT).drawMid(title, x, py + ph * 0.5f) + 14f;
        c.text(Font.regular(), 8.5f).color(Theme.TEXT_3).drawMid(hint, x, py + ph * 0.5f);
        float bx = px + pw - 60f;
        float by = py + 5f;
        boolean over = Ui.inside(mouseX, mouseY, bx, by, 54f, 18f);
        resetHover.to(over ? 1f : 0f);
        resetHover.update(dt);
        resetPress.update(dt);
        float hv = resetHover.get();
        c.push();
        float s = 1f + 0.03f * hv - 0.05f * resetPress.get();
        c.scaleAround(bx + 27f, by + 9f, s, s);
        c.shape(bx, by, 54f, 18f).radius(6f).fill(Colors.withAlpha(0xFFFFFFFF, 0.06f + 0.06f * hv))
                .border(c.px(), Colors.mix(0x26FFFFFF, Colors.withAlpha(Theme.ACCENT_HI, 0.6f), hv))
                .glow(6f, Colors.withAlpha(Theme.GLOW, 0.3f * hv)).draw();
        Draw.icon(c, '\uE012', bx + 11f, by + 9f, 8.5f, Colors.mix(Theme.TEXT_2, Theme.ACCENT_HI, hv));
        c.text(Font.medium(), 8.5f).color(Colors.mix(Theme.TEXT_2, Theme.TEXT, hv)).drawMid("Сброс", bx + 19f, by + 9f);
        c.pop();
        c.popAlpha();
    }

    @Override
    protected boolean onMouseDown(float mx, float my, int button) {
        if (Ui.inside(mx, my, px + pw - 60f, py + 5f, 54f, 18f)) {
            resetPress.impulse(-5f);
            HudLayer.resetPositions();
            return true;
        }
        HudElement e = HudLayer.pick(mx, my);
        if (e != null && button == 0) {
            HudLayer.beginDrag(e, mx, my);
            return true;
        }
        return false;
    }

    @Override
    protected boolean onMouseUp(float mx, float my, int button) {
        HudLayer.endDrag();
        return true;
    }

    @Override
    protected void onClosed() {
        HudLayer.endDrag();
    }
}
