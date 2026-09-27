package dev.fashion.ui.hud;

import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.entity.LivingEntity;

import dev.fashion.anim.Motion;
import dev.fashion.anim.Spring;
import dev.fashion.core.Modules;
import dev.fashion.gfx.Canvas;
import dev.fashion.gfx.Colors;
import dev.fashion.gfx.Projection;
import dev.fashion.gfx.Textures;
import dev.fashion.ui.Theme;

public final class TargetMarker {
    private static final String[] TEXTURES = {"textures/esp/diamond.png", "textures/esp/marker.png"};

    private final Spring x = new Spring(0f, 32f, 0.9f);
    private final Spring y = new Spring(0f, 32f, 0.9f);
    private final Spring presence = new Spring(0f, Motion.ARRIVAL);
    private final Spring flash = new Spring(0f, 9f, 1f);
    private final Spring kick = new Spring(0f, 24f, 0.45f);
    private final float[] tmp = new float[3];
    private boolean placed;
    private LivingEntity last;
    private float lastHp;

    public void paint(Canvas c, float dt, float scale) {
        LivingEntity t = Modules.targetEsp.target();
        boolean visible = false;
        if (t != null) {
            float pt = Projection.tickProgress();
            double ix = t.lastRenderX + (t.getX() - t.lastRenderX) * pt;
            double iy = t.lastRenderY + (t.getY() - t.lastRenderY) * pt + t.getHeight() / 2.0;
            double iz = t.lastRenderZ + (t.getZ() - t.lastRenderZ) * pt;
            if (Projection.project(ix, iy, iz, c.framebufferWidth(), c.framebufferHeight(), tmp)) {
                visible = true;
                float px = tmp[0] / scale;
                float py = tmp[1] / scale;
                x.to(px);
                y.to(py);
                if (!placed || t != last && presence.get() < 0.05f) {
                    x.snap(px);
                    y.snap(py);
                    placed = true;
                }
                if (t == last && t.getHealth() < lastHp - 0.01f) {
                    flash.snap(1f);
                    flash.to(0f);
                    kick.impulse(-4f);
                }
                last = t;
                lastHp = t.getHealth();
            }
        }
        presence.to(visible ? 1f : 0f);
        presence.update(dt);
        x.update(dt);
        y.update(dt);
        flash.update(dt);
        kick.update(dt);
        float p = presence.get();
        if (p <= 0.004f) {
            placed = false;
            return;
        }
        GpuTextureView tex = Textures.get(TEXTURES[Modules.targetEsp.texture.index()]);
        if (tex == null) {
            return;
        }
        double sin = Math.sin(System.currentTimeMillis() / 1000.0);
        float angle = (float) Math.toRadians(sin * 360.0);
        float size = Modules.targetEsp.size.asFloat() * (0.75f + 0.25f * p) * (1f + 0.08f * kick.get());
        float cx = x.get();
        float cy = y.get();
        float fl = flash.get();
        int a = Math.round(220 * Math.max(0f, Math.min(1f, p)));
        int c0 = Colors.withAlpha(Colors.mix(Theme.ACCENT_HI, Theme.DANGER, fl), a / 255f);
        int c1 = Colors.withAlpha(Colors.mix(Theme.ACCENT, Theme.DANGER, fl), a / 255f);
        int c2 = Colors.withAlpha(Colors.mix(Theme.GLOW_SILVER, Theme.DANGER, fl), a / 255f);
        int c3 = Colors.withAlpha(Colors.mix(0xFF6A48E8, Theme.DANGER, fl), a / 255f);
        c.push();
        c.rotateAround(cx, cy, angle);
        c.shape(cx - size / 2f, cy - size / 2f, size, size).corners(c0, c1, c2, c3)
                .image(tex, Textures.sampler(), 0f, 0f, 1f, 1f).draw();
        c.pop();
    }
}
