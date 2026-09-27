package dev.fashion.ui.hud;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.fashion.anim.Motion;
import dev.fashion.anim.Spring;
import dev.fashion.core.Module;
import dev.fashion.core.Modules;
import dev.fashion.gfx.Canvas;
import dev.fashion.gfx.Colors;
import dev.fashion.gfx.Font;
import dev.fashion.ui.Theme;

public final class ModuleList extends HudElement {
    private static final float ROW = 15f;
    private static final float SIZE = 8.5f;

    private static final class Entry {
        final Module module;
        final Spring in = new Spring(0f, Motion.ARRIVAL);
        final Spring y = new Spring(0f, Motion.FOLLOW);
        final Spring w = new Spring(0f, Motion.FLOW);
        boolean placed;

        Entry(Module m) {
            module = m;
        }
    }

    private final Map<Module, Entry> entries = new HashMap<>();
    private final List<Entry> order = new ArrayList<>();

    public ModuleList() {
        super("modules", "Список функций", 0.992f, 0.012f);
    }

    @Override
    public boolean shown(boolean editing) {
        return Modules.hud.enabled() && Modules.hud.arrayList.on();
    }

    private static String suffix(Module m) {
        String s = m.suffix();
        return s == null ? "" : " " + s;
    }

    private static float width(Module m) {
        return Font.medium().width(m.name(), SIZE, 0f) + Font.regular().width(suffix(m), SIZE - 0.5f, 0f) + 16f;
    }

    private List<Module> active(boolean editing) {
        List<Module> list = new ArrayList<>();
        for (Module m : Modules.all()) {
            if (m.enabled() && m != Modules.hud) {
                list.add(m);
            }
        }
        if (list.isEmpty() && editing) {
            list.add(Modules.hud);
        }
        list.sort((a, b) -> Float.compare(width(b), width(a)));
        return list;
    }

    @Override
    public float contentWidth() {
        float max = 60f;
        for (Module m : active(true)) {
            max = Math.max(max, width(m));
        }
        return max;
    }

    @Override
    public float contentHeight() {
        return Math.max(ROW, active(true).size() * ROW);
    }

    @Override
    public void paintContent(Canvas c, float x, float y, float w, float h, float dt, boolean editing) {
        List<Module> act = active(editing);
        for (Module m : act) {
            entries.computeIfAbsent(m, Entry::new);
        }
        order.clear();
        order.addAll(entries.values());
        boolean right = fx > 0.5f;
        int rank = 0;
        for (Module m : act) {
            Entry e = entries.get(m);
            e.in.to(1f, rank * 0.03f);
            e.y.to(rank * ROW);
            e.w.to(width(m));
            if (!e.placed) {
                e.y.snap(e.y.target);
                e.w.snap(e.w.target);
                e.placed = true;
            }
            rank++;
        }
        for (Entry e : order) {
            if (!act.contains(e.module)) {
                e.in.motion(Motion.DEPART);
                e.in.to(0f);
            } else {
                e.in.motion(Motion.ARRIVAL);
            }
            e.in.update(dt);
            e.y.update(dt);
            e.w.update(dt);
        }
        order.removeIf(e -> {
            boolean dead = !act.contains(e.module) && e.in.get() < 0.004f && Math.abs(e.in.velocity) < 0.01f;
            if (dead) {
                entries.remove(e.module);
            }
            return dead;
        });
        int n = Math.max(1, act.size());
        for (Entry e : order) {
            float p = e.in.get();
            float ew = e.w.get();
            float slide = (1f - p) * (ew + 14f);
            float ex = right ? x + w - ew + slide : x - slide;
            float ey = y + e.y.get();
            float t = Math.max(0f, Math.min(1f, e.y.get() / Math.max(1f, (n - 1) * ROW)));
            int accent = Colors.mix(Theme.ACCENT_HI, Theme.ACCENT, t);
            c.pushAlpha(Math.max(0f, Math.min(1f, p * 1.4f)));
            c.shape(ex, ey, ew, ROW).radii(right ? 4f : 0f, right ? 0f : 4f, right ? 0f : 4f, right ? 4f : 0f)
                    .horizontal(right ? Colors.mulAlpha(Theme.PLAQUE, 0.82f) : Theme.PLAQUE, right ? Theme.PLAQUE : Colors.mulAlpha(Theme.PLAQUE, 0.82f))
                    .glass().clouds(0.5f)
                    .draw();
            float bx = right ? ex + ew - 2f : ex;
            c.shape(bx, ey + 1.5f, 2f, ROW - 3f).radius(1f).vertical(accent, Colors.mix(accent, Theme.ACCENT_DEEP, 0.4f))
                    .glow(5f, Colors.withAlpha(accent, 0.85f)).draw();
            float tx = right ? ex + 7f : ex + 9f;
            float cy = ey + ROW * 0.5f;
            tx += c.text(Font.medium(), SIZE).color(Colors.mix(Theme.TEXT, accent, 0.18f)).drawMid(e.module.name(), tx, cy);
            c.text(Font.regular(), SIZE - 0.5f).color(Theme.TEXT_3).drawMid(suffix(e.module), tx, cy);
            c.popAlpha();
        }
    }
}
