package dev.fashion.ui.gui;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

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
    public static final float HEAD = 50f;
    private static final float PAD = 12f;
    private static final float RADIUS = 11f;
    private static final float COL_GAP = 20f;

    public final Module module;
    public final Spring x = new Spring(0f, Motion.FLOW);
    public final Spring y = new Spring(0f, Motion.FOLLOW);
    public final Spring w = new Spring(0f, Motion.FLOW);
    public final Spring h = new Spring(HEAD, Motion.FLOW);
    public final Spring presence = new Spring(0f, Motion.ARRIVAL);
    public final Spring scrollLag = new Spring(0f, Motion.FOLLOW);
    private final Spring hover = new Spring(0f, Motion.HOVER);
    private final Spring press = new Spring(0f, Motion.PRESS);
    private final Spring on;
    private final Spring expand = new Spring(0f, Motion.FLOW);
    private final Switch toggle = new Switch();
    private final BindChip bind;
    private final List<Row> rows = new ArrayList<>();
    private boolean expanded;
    public boolean listed;
    public boolean placed;
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
        bind = new BindChip(module);
        JsonObject saved = Config.ui().has("expanded") ? Config.ui().getAsJsonObject("expanded") : null;
        expanded = saved != null && saved.has(module.name()) && saved.get(module.name()).getAsBoolean() && !rows.isEmpty();
        expand.snap(expanded ? 1f : 0f);
        for (Row r : rows) {
            r.wave.snap(expanded ? 1f : 0f);
        }
    }

    public BindChip bind() {
        return bind;
    }

    public boolean expanded() {
        return expanded;
    }

    public boolean expandable() {
        return !rows.isEmpty();
    }

    public void setExpanded(boolean e) {
        if (rows.isEmpty()) {
            press.impulse(-3f);
            return;
        }
        if (e == expanded) {
            return;
        }
        expanded = e;
        int n = rows.size();
        for (int i = 0; i < n; i++) {
            Row r = rows.get(i);
            if (e) {
                r.wave.motion(15f, 0.7f);
                r.wave.to(1f, 0.06f + i * 0.045f);
            } else {
                r.wave.motion(22f, 1f);
                r.wave.to(0f, (n - 1 - i) * 0.018f);
            }
        }
        if (!Config.ui().has("expanded")) {
            Config.ui().add("expanded", new JsonObject());
        }
        Config.ui().getAsJsonObject("expanded").addProperty(module.name(), e);
        Config.markDirty();
    }

    private boolean twoColumns(float width) {
        return width > 360f;
    }

    private int split(float inner) {
        int n = rows.size();
        float total = 0f;
        for (Row r : rows) {
            total += r.animatedHeight(inner);
        }
        float acc = 0f;
        int best = n;
        float bestCost = Float.MAX_VALUE;
        for (int k = 0; k <= n; k++) {
            float cost = Math.max(acc, total - acc);
            if (cost < bestCost) {
                bestCost = cost;
                best = k;
            }
            if (k < n) {
                acc += rows.get(k).animatedHeight(inner);
            }
        }
        return best;
    }

    private float rowWidth(float width) {
        float inner = width - PAD * 2f;
        return twoColumns(width) ? (inner - COL_GAP) * 0.5f : inner;
    }

    private float rowsHeight(float width) {
        float rw = rowWidth(width);
        if (!twoColumns(width)) {
            float sum = 0f;
            for (Row r : rows) {
                sum += r.animatedHeight(rw);
            }
            return sum;
        }
        int k = split(rw);
        float a = 0f;
        float b = 0f;
        for (int i = 0; i < rows.size(); i++) {
            if (i < k) {
                a += rows.get(i).animatedHeight(rw);
            } else {
                b += rows.get(i).animatedHeight(rw);
            }
        }
        return Math.max(a, b);
    }

    public float targetHeight(float fullWidth) {
        if (!expanded) {
            return HEAD;
        }
        return HEAD + rowsHeight(fullWidth) + 16f;
    }

    public void update(float dt, float fullWidth) {
        for (Row r : rows) {
            r.update(dt);
        }
        h.to(targetHeight(fullWidth));
        x.update(dt);
        y.update(dt);
        w.update(dt);
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

    public void paint(Canvas c, float ox, float oy, float mx, float my, float dt, boolean interactive) {
        float p = presence.get();
        float a = Math.max(0f, Math.min(1f, p * 1.25f));
        if (a <= 0.003f) {
            px = py = pw = ph = 0f;
            return;
        }
        float cw = Math.max(40f, w.get());
        float cx = ox + x.get();
        float cy = oy + y.get() - scrollLag.get() + (1f - p) * 16f;
        float ch = Math.max(h.get(), 12f);
        px = cx;
        py = cy;
        pw = cw;
        ph = ch;
        boolean over = interactive && Ui.inside(mx, my, cx, cy, cw, ch);
        hover.to(over ? 1f : 0f);
        hover.update(dt);
        float hv = hover.get();
        float k = Math.max(0f, Math.min(1f, on.get()));
        float sy = Motion.stretch(visualVelocity(), 0.00005f, 0.03f);
        float sx = 1f - (sy - 1f) * 0.55f;
        float s = (0.955f + 0.045f * p) * (1f + 0.01f * hv - 0.02f * press.get());
        c.push();
        c.scaleAround(cx + cw * 0.5f, cy + ch * 0.5f, s * sx, s * sy);
        c.translate(0f, -2f * hv);
        c.pushAlpha(a);

        int top = Colors.mix(Colors.mix(Theme.CARD_TOP, Theme.CARD_HOVER_TOP, hv), Theme.CARD_ON_TOP, k);
        int bottom = Colors.mix(Theme.CARD_BOTTOM, Theme.CARD_ON_BOTTOM, k);
        int edge = Colors.mix(Colors.mix(0x33FFFFFF, 0x4DFFFFFF, hv), Colors.withAlpha(Theme.GLOW_SILVER, 0.7f), k);
        c.shape(cx, cy, cw, ch).radius(RADIUS)
                .vertical(top, bottom)
                .border(c.px() * 1.15f, edge)
                .chrome(0.75f)
                .shadow(0f, 3f + 5f * hv, 8f + 9f * hv, 0.5f + 0.15f * hv)
                .glow(11f + 4f * hv, Colors.mix(Colors.withAlpha(Theme.GLOW, 0.08f * hv), Colors.withAlpha(0xFF9F86FF, 0.34f), k))
                .sheen(0.035f + 0.03f * hv + 0.03f * k)
                .draw();

        float tile = 28f;
        float tx = cx + 11f;
        float ty = cy + (HEAD - tile) * 0.5f;
        c.shape(tx, ty, tile, tile).radius(8f)
                .vertical(Colors.mix(Theme.INSET_TOP, 0xFF6B4FE0, k), Colors.mix(Theme.INSET, 0xFF2C1A6E, k))
                .border(c.px(), Colors.mix(0x1FFFFFFF, Colors.withAlpha(Theme.ACCENT_HI, 0.7f), k))
                .chrome(0.5f * k)
                .glow(7f, Colors.withAlpha(Theme.GLOW, 0.5f * k))
                .sheen(0.1f * k)
                .draw();
        int iconCol = Colors.mix(Colors.mix(Theme.TEXT_3, Theme.TEXT_2, hv), 0xFFFFFFFF, k);
        Draw.icon(c, module.icon(), tx + tile * 0.5f, ty + tile * 0.5f, 15f, iconCol, 3f * k, Colors.withAlpha(Theme.GLOW_SILVER, 0.7f * k));

        float right = cx + cw - PAD;
        float swW = 26f;
        float swH = 14f;
        float midY = cy + HEAD * 0.5f;
        toggle.paint(c, right - swW, midY - swH * 0.5f, swW, swH, module.enabled(),
                interactive && Ui.inside(mx, my, right - swW - 3f, midY - swH * 0.5f - 3f, swW + 6f, swH + 6f), dt);
        float bindRight = right - swW - 9f;
        bind.paint(c, bindRight, midY, interactive ? mx : -1e4f, interactive ? my : -1e4f, dt);
        float textRight = bindRight - bind.width() - 8f;
        if (!rows.isEmpty()) {
            float chevX = textRight - 5f;
            c.push();
            c.rotateAround(chevX, midY, (float) Math.PI * expand.get());
            Draw.icon(c, '', chevX, midY, 8f, Colors.mix(Theme.TEXT_3, Theme.TEXT, Math.max(hv * 0.6f, expand.get())));
            c.pop();
            textRight -= 14f;
        }
        float nameX = tx + tile + 10f;
        c.pushClip(nameX - 14f, cy - 12f, Math.max(10f, textRight - nameX + 14f), HEAD + 24f, 0f, 12f);
        c.text(Font.semibold(), 11.5f).color(Colors.mix(Theme.TEXT_OFF, 0xFFFFFFFF, Math.max(k, hv * 0.5f))).drawMid(module.name(), nameX, cy + 19f);
        c.text(Font.regular(), 8.5f).color(Colors.mix(Theme.TEXT_3, Theme.TEXT_2, Math.max(k * 0.8f, hv * 0.6f))).drawMid(module.description(), nameX, cy + 33f);
        c.popClip();

        float open = ch - HEAD;
        if (open > 0.5f) {
            float full = rowsHeight(cw) + 16f;
            boolean partial = open < full - 0.5f;
            if (partial) {
                c.pushClip(cx + 1f, cy + HEAD - 2f, cw - 2f, open + 1f, 0f, 6f);
            }
            float ea = Math.max(0f, Math.min(1f, open / 18f));
            c.pushAlpha(ea);
            Draw.hairline(c, cx + PAD, cy + HEAD - 1f, cw - PAD * 2f, 0x2EFFFFFF);
            float rw = rowWidth(cw);
            float rx0 = cx + PAD;
            float ry0 = cy + HEAD + 7f;
            if (twoColumns(cw)) {
                int split = split(rw);
                float ya = ry0;
                float yb = ry0;
                float rx1 = rx0 + rw + COL_GAP;
                if (split < rows.size() && split > 0) {
                    c.shape(rx1 - COL_GAP * 0.5f, ry0 + 4f, c.px(), Math.max(0f, rowsHeight(cw) - 8f)).fill(0x14FFFFFF).draw();
                }
                for (int i = 0; i < rows.size(); i++) {
                    Row r = rows.get(i);
                    if (i < split) {
                        r.paintRow(c, rx0, ya, rw, mx, my, dt);
                        ya += r.animatedHeight(rw);
                    } else {
                        r.paintRow(c, rx1, yb, rw, mx, my, dt);
                        yb += r.animatedHeight(rw);
                    }
                }
            } else {
                float ry = ry0;
                for (Row r : rows) {
                    r.paintRow(c, rx0, ry, rw, mx, my, dt);
                    ry += r.animatedHeight(rw);
                }
            }
            c.popAlpha();
            if (partial) {
                c.popClip();
            }
        }
        c.popAlpha();
        c.pop();
    }

    public Row rowAt(float mx, float my) {
        if (!contains(mx, my) || !expanded) {
            return null;
        }
        for (Row r : rows) {
            if (r.hit(mx, my)) {
                return r;
            }
        }
        return null;
    }

    public float right() {
        return px + pw;
    }

    public boolean contains(float mx, float my) {
        return ph > 0f && Ui.inside(mx, my, px, py, pw, ph);
    }

    public boolean mouseDown(float mx, float my, int button) {
        if (!contains(mx, my)) {
            return false;
        }
        if (my < py + HEAD) {
            if (bind.mouseDown(mx, my, button)) {
                return true;
            }
            float swX = px + pw - PAD - 26f;
            if (button == 0 && Ui.inside(mx, my, swX - 4f, py + HEAD * 0.5f - 11f, 34f, 22f)) {
                module.toggle();
                toggle.kick();
                return true;
            }
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
