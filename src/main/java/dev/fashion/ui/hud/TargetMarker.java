package dev.fashion.ui.hud;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;

import dev.fashion.anim.Clock;
import dev.fashion.anim.Motion;
import dev.fashion.anim.Spring;
import dev.fashion.core.Modules;
import dev.fashion.gfx.Canvas;
import dev.fashion.gfx.Colors;
import dev.fashion.gfx.Projection;
import dev.fashion.ui.Draw;
import dev.fashion.ui.Theme;

public final class TargetMarker {
    private final Spring cx = new Spring(0f, 30f, 0.85f);
    private final Spring cy = new Spring(0f, 30f, 0.85f);
    private final Spring size = new Spring(0f, 30f, 0.85f);
    private final Spring top = new Spring(0f, 30f, 0.85f);
    private final Spring presence = new Spring(0f, Motion.ARRIVAL);
    private final Spring flash = new Spring(0f, 9f, 1f);
    private final float[] tmp = new float[3];
    private boolean placed;
    private LivingEntity last;
    private float lastHp;

    public void paint(Canvas c, float dt, float scale) {
        LivingEntity t = Modules.targetEsp.enabled() ? Modules.attackAura.target() : null;
        boolean visible = false;
        if (t != null) {
            float pt = Projection.tickProgress();
            double ix = t.lastRenderX + (t.getX() - t.lastRenderX) * pt;
            double iy = t.lastRenderY + (t.getY() - t.lastRenderY) * pt;
            double iz = t.lastRenderZ + (t.getZ() - t.lastRenderZ) * pt;
            Box b = t.getBoundingBox().offset(ix - t.getX(), iy - t.getY(), iz - t.getZ());
            float minX = Float.MAX_VALUE;
            float minY = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE;
            float maxY = -Float.MAX_VALUE;
            int ok = 0;
            for (int i = 0; i < 8; i++) {
                double px = (i & 1) == 0 ? b.minX : b.maxX;
                double py = (i & 2) == 0 ? b.minY : b.maxY;
                double pz = (i & 4) == 0 ? b.minZ : b.maxZ;
                if (Projection.project(px, py, pz, c.framebufferWidth(), c.framebufferHeight(), tmp)) {
                    minX = Math.min(minX, tmp[0]);
                    maxX = Math.max(maxX, tmp[0]);
                    minY = Math.min(minY, tmp[1]);
                    maxY = Math.max(maxY, tmp[1]);
                    ok++;
                }
            }
            if (ok == 8) {
                visible = true;
                float mx = (minX + maxX) * 0.5f / scale;
                float my = (minY + maxY) * 0.5f / scale;
                float sz = Math.max(maxX - minX, maxY - minY) / scale;
                cx.to(mx);
                cy.to(my);
                size.to(sz);
                top.to(minY / scale);
                if (!placed || t != last && presence.get() < 0.05f) {
                    cx.snap(mx);
                    cy.snap(my);
                    size.snap(sz);
                    top.snap(minY / scale);
                    placed = true;
                }
                if (t == last && t.getHealth() < lastHp - 0.01f) {
                    flash.snap(1f);
                    flash.to(0f);
                    size.impulse(-sz * 2.2f);
                }
                last = t;
                lastHp = t.getHealth();
            }
        }
        presence.to(visible ? 1f : 0f);
        presence.update(dt);
        cx.update(dt);
        cy.update(dt);
        size.update(dt);
        top.update(dt);
        flash.update(dt);
        float p = presence.get();
        if (p <= 0.004f) {
            placed = false;
            return;
        }
        float time = Clock.time();
        float spin = Modules.targetEsp.spin.on() ? time * 0.9f : 0f;
        float fl = flash.get();
        int col = Colors.mix(Theme.ACCENT_HI, 0xFFFF6B7D, fl);
        int glow = Colors.mix(Colors.withAlpha(Theme.GLOW, 0.9f), Colors.withAlpha(0xFFFF4D63, 0.9f), fl);
        float s = size.get() * (1.18f + (1f - p) * 0.5f);
        float x = cx.get();
        float y = cy.get();
        c.pushAlpha(Math.max(0f, Math.min(1f, p)));
        switch (Modules.targetEsp.style.index()) {
            case 0 -> corners(c, x, y, s, col, glow, time);
            case 1 -> ring(c, x, y, s, col, glow, spin);
            default -> star(c, x, top.get() - 10f, col, glow, spin, p);
        }
        c.popAlpha();
    }

    private static void corners(Canvas c, float x, float y, float s, int col, int glow, float time) {
        float breathe = 1.5f * (float) Math.sin(time * 2.6f);
        float half = s * 0.5f + breathe;
        float len = Math.max(5f, s * 0.2f);
        float t = 1.8f;
        for (int i = 0; i < 4; i++) {
            float sx = (i & 1) == 0 ? -1f : 1f;
            float sy = (i & 2) == 0 ? -1f : 1f;
            float ex = x + sx * half;
            float ey = y + sy * half;
            float hx = sx < 0 ? ex : ex - len;
            float vy = sy < 0 ? ey : ey - len;
            c.shape(hx, ey - t * 0.5f, len, t).radius(t * 0.5f).fill(col).glow(5f, glow).draw();
            c.shape(ex - t * 0.5f, vy, t, len).radius(t * 0.5f).fill(col).glow(5f, glow).draw();
        }
    }

    private static void ring(Canvas c, float x, float y, float s, int col, int glow, float spin) {
        float r = s * 0.55f;
        c.shape(x - r, y - r, r * 2f, r * 2f).radius(r).fill(0).border(1.6f, col).glow(7f, glow).draw();
        for (int i = 0; i < 3; i++) {
            double a = spin + i * Math.PI * 2.0 / 3.0;
            float dx = x + (float) Math.cos(a) * r;
            float dy = y + (float) Math.sin(a) * r;
            c.shape(dx - 2.2f, dy - 2.2f, 4.4f, 4.4f).radius(2.2f).fill(0xFFFFFFFF).glow(6f, glow).draw();
        }
    }

    private static void star(Canvas c, float x, float y, int col, int glow, float spin, float p) {
        c.push();
        c.rotateAround(x, y, spin * 0.6f);
        float sz = 14f * (0.6f + 0.4f * p);
        c.shape(x - 3f, y - 3f, 6f, 6f).radius(3f).fill(0).glow(10f, glow).draw();
        Draw.chromeIcon(c, '\uE006', x, y, sz, 3f, glow);
        c.pop();
    }
}
