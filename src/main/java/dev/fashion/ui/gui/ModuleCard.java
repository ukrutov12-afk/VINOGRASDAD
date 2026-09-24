package dev.fashion.ui.gui;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonObject;

import dev.fashion.anim.Motion;
import dev.fashion.anim.Spring;
import dev.fashion.core.Config;
import dev.fashion.core.Module;
import dev.fashion.core.setting.Setting;
import dev.fashion.gfx.Canvas;
import dev.fashion.gfx.Colors;
import dev.fashion.gfx.Font;
import dev.fashion.ui.Draw;
import dev.fashion.ui.Theme;
import dev.fashion.ui.Ui;

public final class ModuleCard {
    private static final float PAD = 13f;
    private static final float RADIUS = 12f;

    public final Module module;
    public final Spring x = new Spring(0f, Motion.FLOW);
    public final Spring y = new Spring(0f, Motion.FOLLOW);
    public final Spring h = new Spring(0f, Motion.FLOW);
    public final Spring presence = new Spring(0f, Motion.ARRIVAL);
    public final Spring scrollLag = new Spring(0f, Motion.FOLLOW);
    private final Spring hover = new Spring(0f, Motion.HOVER);
    private final Spring press = new Spring(0f, Motion.PRESS);
    private final Spring on;
    private final Spring expand = new Spring(0f, Motion.FLOW);
    private final Switch toggle = new Switch();
    private final List<Row> rows = new ArrayList<>();
    private final BindRow bind;
    private boolean expanded;
    public boolean listed;
    public boolean placed;
    private List<String> lines;
    private float linesWidth = -1f;
    private float headerH = 52f;
    private float px;
    private float py;
    private float pw;
    private float ph;

    public ModuleCard(Module module) {
        this.module = module;
        this.on = new Spring(module.enabled() ? 1f : 0f, Motion.TOGGLE);
        for (Setting<?> s : module.settings()) {
            Row r = Row.of(s);
            if (r != null) {
                r.init();
                rows.add(r);
            }
        }
        bind = new BindRow(module);
        bind.init();
        rows.add(bind);
        JsonObject saved = Config.ui().has("expanded") ? Config.ui().getAsJsonObject("expanded") : null;
        expanded = saved == null || !saved.has(module.name()) || saved.get(module.name()).getAsBoolean();
        expand.snap(expanded ? 1f : 0f);
    }

    public BindRow bind() {
        return bind;
    }

    public boolean expanded() {
        return expanded;
    }

    public void setExpanded(boolean e) {
        expanded = e;
        if (!Config.ui().has("expanded")) {
            Config.ui().add("expanded", new JsonObject());
        }
        Config.ui().getAsJsonObject("expanded").addProperty(module.name(), e);
        Config.markDirty();
    }

    private void measure(float w) {
        if (linesWidth == w) {
            return;
        }
        linesWidth = w;
        lines = Ui.wrap(Font.regular(), 9f, module.description(), w - PAD * 2f - 44f, 2);
        headerH = 34f + lines.size() * 11.5f + 8f;
    }

    public float targetHeight(float w) {
        measure(w);
        if (!expanded) {
            return headerH;
        }
        return headerH + rowsHeight(w) + 14f;
    }

    private float rowsHeight(float w) {
        float inner = w - PAD * 2f;
        float sum = 0f;
        for (Row r : rows) {
            sum += r.animatedHeight(inner);
        }
        return sum;
    }

    public void update(float dt, float width) {
        for (Row r : rows) {
            r.update(dt);
        }
        h.to(targetHeight(width));
        if (!placed) {
            h.snap(h.target);
        }
        x.update(dt);
        y.update(dt);
        h.update(dt);
        presence.update(dt);
        scrollLag.update(dt);
        expand.to(expanded ? 1f : 0f);
        expand.update(dt);
        on.to(module.enabled() ? 1f : 0f);
        on.update(dt);
        press.update(dt);
    }

    public boolean alive() {
        return listed || presence.get() > 0.004f || Math.abs(presence.velocity) > 0.01f;
    }

    public float visualVelocity() {
        return y.velocity - scrollLag.velocity;
    }

    public void paint(Canvas c, float ox, float oy, float w, float mx, float my, float dt, boolean interactive) {
        measure(w);
        float p = presence.get();
        float a = Math.max(0f, Math.min(1f, p * 1.25f));
        if (a <= 0.003f) {
            px = py = pw = ph = 0f;
            return;
        }
        float cx = ox + x.get();
        float cy = oy + y.get() - scrollLag.get() + (1f - p) * 18f;
        float ch = Math.max(h.get(), 8f);
        px = cx;
        py = cy;
        pw = w;
        ph = ch;
        boolean over = interactive && Ui.inside(mx, my, cx, cy, w, ch);
        hover.to(over ? 1f : 0f);
        hover.update(dt);
        float hv = hover.get();
        float k = Math.max(0f, Math.min(1f, on.get()));
        float vel = visualVelocity();
        float sy = Motion.stretch(vel, 0.00005f, 0.03f);
        float sx = 1f - (sy - 1f) * 0.55f;
        float s = (0.955f + 0.045f * p) * (1f + 0.012f * hv - 0.02f * press.get());
        c.push();
        c.scaleAround(cx + w * 0.5f, cy + ch * 0.5f, s * sx, s * sy);
        c.translate(0f, -2.2f * hv);
        c.pushAlpha(a);

        int lineCol = Colors.mix(Colors.mix(0x2AFFFFFF, 0x45FFFFFF, hv), Colors.withAlpha(Theme.ACCENT_HI, 0.65f), k);
        c.shape(cx, cy, w, ch).radius(RADIUS)
                .vertical(Colors.mix(Theme.SURFACE_TOP, 0xF01E1735, k * 0.8f + hv * 0.2f), Colors.mix(Theme.SURFACE, 0xF0120D24, k))
                .border(c.px() * 1.1f, lineCol)
                .chrome(0.55f)
                .shadow(0f, 3f + 5f * hv, 9f + 9f * hv, 0.42f + 0.18f * hv)
                .glow(10f + 4f * hv, Colors.withAlpha(Theme.GLOW, 0.26f * k + 0.1f * hv))
                .sheen(0.045f + 0.035f * hv)
                .draw();

        float nameY = cy + 19f;
        c.text(Font.semibold(), 11.5f).color(Colors.mix(Theme.TEXT, 0xFFFFFFFF, k)).drawMid(module.name(), cx + PAD, nameY);
        for (int i = 0; i < lines.size(); i++) {
            c.text(Font.regular(), 9f).color(Colors.mix(Theme.TEXT_3, Theme.TEXT_2, 0.55f + 0.45f * hv))
                    .drawMid(lines.get(i), cx + PAD, cy + 35f + i * 11.5f);
        }
        float swW = 27f;
        float swH = 15f;
        float swX = cx + w - PAD - swW;
        float swY = nameY - swH * 0.5f;
        toggle.paint(c, swX, swY, swW, swH, module.enabled(), interactive && Ui.inside(mx, my, swX - 3f, swY - 3f, swW + 6f, swH + 6f), dt);

        float chevX = swX - 13f;
        c.push();
        c.rotateAround(chevX, nameY, (float) Math.PI * expand.get());
        Draw.icon(c, '\uE008', chevX, nameY, 9f, Colors.mix(Theme.TEXT_3, Theme.TEXT, Math.max(hv * 0.6f, expand.get())));
        c.pop();

        float open = ch - headerH;
        if (open > 0.5f) {
            float inner = w - PAD * 2f;
            float full = rowsHeight(w) + 14f;
            boolean partial = open < full - 0.5f;
            if (partial) {
                c.pushClip(cx + 1f, cy + headerH - 2f, w - 2f, open + 1f, 0f);
            }
            float ra = Math.max(0f, Math.min(1f, open / Math.max(1f, full)));
            c.pushAlpha(ra);
            Draw.hairline(c, cx + PAD, cy + headerH - 1f, inner, 0x30FFFFFF);
            float ry = cy + headerH + 6f;
            for (Row r : rows) {
                r.paintRow(c, cx + PAD, ry, inner, mx, my, dt);
                ry += r.animatedHeight(inner);
            }
            c.popAlpha();
            if (partial) {
                c.popClip();
            }
        }
        c.popAlpha();
        c.pop();
    }

    public String tipAt(float mx, float my) {
        if (!contains(mx, my) || !expanded) {
            return null;
        }
        for (Row r : rows) {
            if (r.hit(mx, my)) {
                return r.tip();
            }
        }
        return null;
    }

    public boolean contains(float mx, float my) {
        return ph > 0f && Ui.inside(mx, my, px, py, pw, ph);
    }

    public boolean mouseDown(float mx, float my, int button) {
        if (!contains(mx, my)) {
            return false;
        }
        float swX = px + pw - PAD - 27f;
        float swY = py + 19f - 7.5f;
        if (button == 0 && Ui.inside(mx, my, swX - 4f, swY - 4f, 35f, 23f)) {
            module.toggle();
            toggle.kick();
            return true;
        }
        if (my < py + headerH) {
            setExpanded(!expanded);
            press.impulse(-3.5f);
            return true;
        }
        if (expanded) {
            for (Row r : rows) {
                if (r.mouseDown(mx, my, button)) {
                    return true;
                }
            }
        }
        return true;
    }

    public void mouseUp(float mx, float my) {
        for (Row r : rows) {
            r.mouseUp(mx, my);
        }
    }
}
