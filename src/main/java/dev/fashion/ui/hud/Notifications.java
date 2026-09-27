package dev.fashion.ui.hud;

import java.util.ArrayList;
import java.util.List;

import dev.fashion.anim.Motion;
import dev.fashion.anim.Spring;
import dev.fashion.core.Modules;
import dev.fashion.gfx.Canvas;
import dev.fashion.gfx.Colors;
import dev.fashion.gfx.Font;
import dev.fashion.ui.Draw;
import dev.fashion.ui.Theme;
import dev.fashion.ui.Ui;

public final class Notifications extends HudElement {
    private static final float CW = 200f;
    private static final float CH = 42f;
    private static final float GAP = 6f;
    private static final float[][] CORNERS = {{0.992f, 0.975f}, {0.992f, 0.2f}, {0.008f, 0.86f}, {0.008f, 0.1f}};
    private static Notifications instance;

    private static final class Note {
        final String key;
        String title;
        String sub;
        char icon;
        int color;
        int count = 1;
        float time;
        boolean leaving;
        boolean placed;
        final Spring in = new Spring(0f, 13f, 0.62f);
        final Spring y = new Spring(0f, Motion.FOLLOW);
        final Spring kick = new Spring(0f, 26f, 0.4f);
        final Spring hover = new Spring(0f, Motion.HOVER);
        final Spring badge = new Spring(0f, Motion.TOGGLE);
        float px;
        float py;

        Note(String key, String title, String sub, char icon, int color) {
            this.key = key;
            this.title = title;
            this.sub = sub;
            this.icon = icon;
            this.color = color;
            in.to(1f);
        }
    }

    private final List<Note> notes = new ArrayList<>();

    public Notifications() {
        super("notifications", "Уведомления", CORNERS[0][0], CORNERS[0][1]);
        instance = this;
    }

    public static void snapToCorner(int corner) {
        if (instance != null) {
            instance.fx = CORNERS[corner][0];
            instance.fy = CORNERS[corner][1];
        }
    }

    public static void post(String key, String title, String sub, char icon, int color) {
        if (instance != null) {
            instance.add(key, title, sub, icon, color);
        }
    }

    private void add(String key, String title, String sub, char icon, int color) {
        for (Note n : notes) {
            if (!n.leaving && n.key.equals(key)) {
                n.count++;
                n.time = 0f;
                n.title = title;
                n.sub = sub;
                n.icon = icon;
                n.color = color;
                n.kick.impulse(-5f);
                n.badge.to(1f);
                return;
            }
        }
        notes.add(new Note(key, title, sub, icon, color));
        int max = (int) Math.round(Modules.notifications.max.asDouble());
        int active = 0;
        for (int i = notes.size() - 1; i >= 0; i--) {
            Note n = notes.get(i);
            if (n.leaving) {
                continue;
            }
            active++;
            if (active > max) {
                leave(n);
            }
        }
    }

    private static void leave(Note n) {
        n.leaving = true;
        n.in.motion(Motion.DEPART);
        n.in.to(0f);
    }

    private boolean right() {
        return fx > 0.5f;
    }

    private boolean bottom() {
        return fy > 0.5f;
    }

    private int active() {
        int k = 0;
        for (Note n : notes) {
            if (!n.leaving) {
                k++;
            }
        }
        return k;
    }

    @Override
    public boolean shown(boolean editing) {
        return Modules.notifications.enabled() && (editing || !notes.isEmpty());
    }

    @Override
    public float contentWidth() {
        return CW;
    }

    @Override
    public float contentHeight() {
        int n = Math.max(1, active());
        return n * CH + (n - 1) * GAP;
    }

    @Override
    public void update(float dt, float sw, float sh, boolean editing) {
        super.update(dt, sw, sh, editing);
        float duration = (float) Modules.notifications.duration.asDouble();
        float[] mouse = HudLayer.mouse();
        for (Note n : notes) {
            boolean over = mouse != null && !n.leaving && Ui.inside(mouse[0], mouse[1], n.px, n.py, CW, CH);
            n.hover.to(over ? 1f : 0f);
            if (!n.leaving && !over && !editing) {
                n.time += dt;
                if (n.time >= duration) {
                    leave(n);
                }
            }
            n.in.update(dt);
            n.y.update(dt);
            n.kick.update(dt);
            n.hover.update(dt);
            n.badge.update(dt);
        }
        notes.removeIf(n -> n.leaving && n.in.get() < 0.01f && Math.abs(n.in.velocity) < 0.05f);
    }

    public boolean click(float mx, float my) {
        for (Note n : notes) {
            if (!n.leaving && Ui.inside(mx, my, n.px, n.py, CW, CH)) {
                leave(n);
                return true;
            }
        }
        return false;
    }

    @Override
    public void paintContent(Canvas c, float x, float y, float w, float h, float dt, boolean editing) {
        if (notes.isEmpty() && editing) {
            paintCard(c, x, y, "Уведомления", "пример карточки", '', Theme.ACCENT_HI, 1, 0.62f, 0f, 0f, 0f);
            return;
        }
        boolean fromRight = right();
        boolean fromBottom = bottom();
        int index = 0;
        for (int i = notes.size() - 1; i >= 0; i--) {
            Note n = notes.get(i);
            if (n.leaving) {
                continue;
            }
            float slot = index * (CH + GAP);
            float target = fromBottom ? h - CH - slot : slot;
            n.y.to(target);
            if (!n.placed) {
                n.y.snap(target);
                n.placed = true;
            }
            index++;
        }
        float duration = (float) Modules.notifications.duration.asDouble();
        for (Note n : notes) {
            float p = n.in.get();
            float side = fromRight ? 1f : -1f;
            float slide = (1f - p) * (CW + 28f) * side;
            float nx = x + slide;
            float ny = y + n.y.get();
            n.px = nx;
            n.py = ny;
            float remain = n.leaving ? Math.max(0f, 1f - n.time / duration) : Math.max(0f, 1f - n.time / duration);
            c.pushAlpha(Math.max(0f, Math.min(1f, p * 1.6f)));
            paintCard(c, nx, ny, n.title, n.sub, n.icon, n.color, n.count, remain, n.hover.get(), n.kick.get(), n.badge.get());
            c.popAlpha();
        }
    }

    private static void paintCard(Canvas c, float x, float y, String title, String sub, char icon, int color,
                                  int count, float remain, float hover, float kick, float badge) {
        float s = 1f + 0.05f * kick + 0.015f * hover;
        c.push();
        c.scaleAround(x + CW * 0.5f, y + CH * 0.5f, s, s);
        c.translate(0f, -2f * hover);
        c.shape(x, y, CW, CH).radius(10f).fill(Theme.PLAQUE).glass().clouds(0.55f)
                .border(c.px() * 1.1f, Colors.mix(0x33FFFFFF, Colors.withAlpha(color, 0.5f), 0.35f)).chrome(0.8f)
                .shadow(0f, 3f + 4f * hover, 9f + 7f * hover, 0.55f)
                .glow(9f + 3f * hover, Colors.withAlpha(color, 0.2f + 0.12f * hover - 0.3f * kick))
                .draw();
        float ix = x + 8f;
        float iy = y + (CH - 26f) * 0.5f;
        c.shape(ix, iy, 26f, 26f).radius(7f)
                .vertical(Colors.mix(Theme.INSET_TOP, color, 0.16f), Colors.mix(Theme.INSET, color, 0.06f))
                .border(c.px(), Colors.withAlpha(color, 0.35f))
                .draw();
        Draw.icon(c, icon, ix + 13f, iy + 13f, 13f, color, 3f, Colors.withAlpha(color, 0.55f));
        float tx = x + 42f;
        float maxW = CW - 52f - (count > 1 ? 24f : 0f);
        c.pushClip(tx - 10f, y - 8f, maxW + 10f, CH + 16f, 0f, 8f);
        c.text(Font.semibold(), 10f).color(Theme.TEXT).drawMid(title, tx, y + 15f);
        c.text(Font.regular(), 8.5f).color(Colors.mix(Theme.TEXT_2, color, 0.55f)).drawMid(sub, tx, y + 28f);
        c.popClip();
        if (count > 1 && badge > 0.01f) {
            String b = "×" + count;
            float bw = Font.semibold().width(b, 8.5f, 0f) + 10f;
            float bx = x + CW - 8f - bw;
            float by = y + 8f;
            float bs = Math.max(0f, badge);
            c.push();
            c.scaleAround(bx + bw * 0.5f, by + 7f, bs, bs);
            c.shape(bx, by, bw, 14f).radius(7f).fill(Colors.withAlpha(color, 0.16f)).border(c.px(), Colors.withAlpha(color, 0.45f)).draw();
            c.text(Font.semibold(), 8.5f).color(color).drawMidCenter(b, bx + bw * 0.5f, by + 7f);
            c.pop();
        }
        float bw = (CW - 20f) * Math.max(0f, Math.min(1f, remain));
        if (bw > 0.5f) {
            c.shape(x + 10f, y + CH - 4f, bw, 1.6f).radius(0.8f)
                    .horizontal(Colors.withAlpha(color, 0.25f), Colors.withAlpha(color, 0.95f))
                    .glow(3f, Colors.withAlpha(color, 0.5f))
                    .draw();
        }
        c.pop();
    }
}
