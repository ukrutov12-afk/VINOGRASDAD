package dev.fashion.ui.hud;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;

import java.util.List;

import dev.fashion.anim.Motion;
import dev.fashion.anim.Spring;
import dev.fashion.core.Config;
import dev.fashion.gfx.Canvas;
import dev.fashion.gfx.Colors;
import dev.fashion.ui.Theme;

public final class HudLayer {
    private static final float MARGIN = 6f;
    private static final float SNAP = 6f;

    private static final List<HudElement> ELEMENTS = List.of(new Watermark(), new ModuleList(), new Coordinates(), new TargetCard());
    private static final TargetMarker MARKER = new TargetMarker();
    private static final Spring edit = new Spring(0f, Motion.FADE);
    private static final Spring vGuideA = new Spring(0f, Motion.HOVER);
    private static final Spring hGuideA = new Spring(0f, Motion.HOVER);
    private static final Spring vGuide = new Spring(0f, Motion.FOLLOW);
    private static final Spring hGuide = new Spring(0f, Motion.FOLLOW);
    private static float scale = 2f;
    private static float sw = 960f;
    private static float sh = 540f;
    private static HudElement dragging;
    private static float grabX;
    private static float grabY;
    private static boolean vSnapped;
    private static boolean hSnapped;

    private HudLayer() {
    }

    public static float scale() {
        return scale;
    }

    public static float screenWidth() {
        return sw;
    }

    public static float screenHeight() {
        return sh;
    }

    public static List<HudElement> elements() {
        return ELEMENTS;
    }

    public static void tick() {
        for (HudElement e : ELEMENTS) {
            e.tick();
        }
    }

    public static void paint(Canvas c, float dt) {
        MinecraftClient mc = MinecraftClient.getInstance();
        scale = Math.max(1f, c.framebufferHeight() / 540f);
        sw = c.framebufferWidth() / scale;
        sh = c.framebufferHeight() / scale;
        boolean editing = mc.currentScreen instanceof HudEditorScreen;
        edit.to(editing ? 1f : 0f);
        edit.update(dt);
        c.push();
        c.scale(scale, scale);
        MARKER.paint(c, dt, scale);
        float ea = edit.get();
        if (ea > 0.004f) {
            backdrop(c, ea);
        }
        for (HudElement e : ELEMENTS) {
            e.update(dt, sw, sh, editing);
            e.paint(c, dt, editing || ea > 0.5f);
        }
        vGuideA.to(dragging != null && vSnapped ? 1f : 0f);
        hGuideA.to(dragging != null && hSnapped ? 1f : 0f);
        vGuideA.update(dt);
        hGuideA.update(dt);
        vGuide.update(dt);
        hGuide.update(dt);
        if (vGuideA.get() > 0.01f) {
            c.shape(vGuide.get() - 0.5f, 0f, 1f, sh).vertical(Colors.withAlpha(Theme.ACCENT_HI, 0f), Colors.withAlpha(Theme.ACCENT_HI, 0.7f * vGuideA.get()))
                    .glow(4f, Colors.withAlpha(Theme.GLOW, 0.6f * vGuideA.get())).draw();
        }
        if (hGuideA.get() > 0.01f) {
            c.shape(0f, hGuide.get() - 0.5f, sw, 1f).horizontal(Colors.withAlpha(Theme.ACCENT_HI, 0.7f * hGuideA.get()), Colors.withAlpha(Theme.ACCENT_HI, 0.1f * hGuideA.get()))
                    .glow(4f, Colors.withAlpha(Theme.GLOW, 0.6f * hGuideA.get())).draw();
        }
        c.pop();
    }

    private static void backdrop(Canvas c, float ea) {
        c.shape(0f, 0f, sw, sh).fill(Colors.withAlpha(0xFF05040A, 0.3f * ea)).draw();
        int line = Colors.withAlpha(0xFFFFFFFF, 0.028f * ea);
        float step = 30f;
        for (float gx = step; gx < sw; gx += step) {
            c.shape(gx, 0f, c.px(), sh).fill(line).draw();
        }
        for (float gy = step; gy < sh; gy += step) {
            c.shape(0f, gy, sw, c.px()).fill(line).draw();
        }
        int mid = Colors.withAlpha(Theme.ACCENT_HI, 0.12f * ea);
        c.shape(sw * 0.5f, 0f, c.px(), sh).fill(mid).draw();
        c.shape(0f, sh * 0.5f, sw, c.px()).fill(mid).draw();
    }

    public static void hover(float mx, float my) {
        for (HudElement e : ELEMENTS) {
            e.hover.to(dragging == null && e.contains(mx, my) ? 1f : 0f);
        }
    }

    public static HudElement pick(float mx, float my) {
        for (int i = ELEMENTS.size() - 1; i >= 0; i--) {
            HudElement e = ELEMENTS.get(i);
            if (e.contains(mx, my)) {
                return e;
            }
        }
        return null;
    }

    public static void beginDrag(HudElement e, float mx, float my) {
        dragging = e;
        e.dragging = true;
        grabX = mx - e.x.target;
        grabY = my - e.y.target;
    }

    public static boolean isDragging() {
        return dragging != null;
    }

    public static void drag(float mx, float my) {
        if (dragging == null) {
            return;
        }
        HudElement e = dragging;
        float w = e.w.target;
        float h = e.h.target;
        float nx = Math.max(0f, Math.min(sw - w, mx - grabX));
        float ny = Math.max(0f, Math.min(sh - h, my - grabY));
        float bestX = Float.MAX_VALUE;
        float snapX = nx;
        float guideX = 0f;
        float bestY = Float.MAX_VALUE;
        float snapY = ny;
        float guideY = 0f;
        float[][] xs = {{MARGIN, MARGIN}, {sw - MARGIN - w, sw - MARGIN}, {(sw - w) * 0.5f, sw * 0.5f}};
        float[][] ys = {{MARGIN, MARGIN}, {sh - MARGIN - h, sh - MARGIN}, {(sh - h) * 0.5f, sh * 0.5f}};
        for (float[] cand : xs) {
            float d = Math.abs(nx - cand[0]);
            if (d < SNAP && d < bestX) {
                bestX = d;
                snapX = cand[0];
                guideX = cand[1];
            }
        }
        for (float[] cand : ys) {
            float d = Math.abs(ny - cand[0]);
            if (d < SNAP && d < bestY) {
                bestY = d;
                snapY = cand[0];
                guideY = cand[1];
            }
        }
        for (HudElement o : ELEMENTS) {
            if (o == e || o.presence.get() < 0.5f) {
                continue;
            }
            float ox = o.x.target;
            float oy = o.y.target;
            float ow = o.w.target;
            float oh = o.h.target;
            float[][] ox2 = {{ox, ox}, {ox + ow - w, ox + ow}, {ox + ow + 4f, ox + ow + 2f}, {ox - w - 4f, ox - 2f}};
            float[][] oy2 = {{oy, oy}, {oy + oh - h, oy + oh}, {oy + oh + 4f, oy + oh + 2f}, {oy - h - 4f, oy - 2f}};
            for (float[] cand : ox2) {
                float d = Math.abs(nx - cand[0]);
                if (d < SNAP && d < bestX) {
                    bestX = d;
                    snapX = cand[0];
                    guideX = cand[1];
                }
            }
            for (float[] cand : oy2) {
                float d = Math.abs(ny - cand[0]);
                if (d < SNAP && d < bestY) {
                    bestY = d;
                    snapY = cand[0];
                    guideY = cand[1];
                }
            }
        }
        boolean wasV = vSnapped;
        boolean wasH = hSnapped;
        vSnapped = bestX < Float.MAX_VALUE;
        hSnapped = bestY < Float.MAX_VALUE;
        if (vSnapped) {
            vGuide.to(guideX);
            if (!wasV) {
                vGuide.snap(guideX);
            }
        }
        if (hSnapped) {
            hGuide.to(guideY);
            if (!wasH) {
                hGuide.snap(guideY);
            }
        }
        e.x.to(snapX);
        e.y.to(snapY);
    }

    public static void endDrag() {
        if (dragging == null) {
            return;
        }
        HudElement e = dragging;
        float w = e.w.target;
        float h = e.h.target;
        e.fx = sw - w > 0f ? Math.max(0f, Math.min(1f, e.x.target / (sw - w))) : 0f;
        e.fy = sh - h > 0f ? Math.max(0f, Math.min(1f, e.y.target / (sh - h))) : 0f;
        e.dragging = false;
        dragging = null;
        vSnapped = false;
        hSnapped = false;
        Config.markDirty();
    }

    public static void resetPositions() {
        float[][] defaults = {{0.008f, 0.012f}, {0.992f, 0.012f}, {0.008f, 0.985f}, {0.5f, 0.74f}};
        for (int i = 0; i < ELEMENTS.size(); i++) {
            ELEMENTS.get(i).fx = defaults[i][0];
            ELEMENTS.get(i).fy = defaults[i][1];
        }
        Config.markDirty();
    }

    public static void load(JsonObject o) {
        for (HudElement e : ELEMENTS) {
            if (o.has(e.id)) {
                JsonArray a = o.getAsJsonArray(e.id);
                e.fx = a.get(0).getAsFloat();
                e.fy = a.get(1).getAsFloat();
            }
        }
    }

    public static JsonObject save() {
        JsonObject o = new JsonObject();
        for (HudElement e : ELEMENTS) {
            JsonArray a = new JsonArray();
            a.add(e.fx);
            a.add(e.fy);
            o.add(e.id, a);
        }
        return o;
    }
}
