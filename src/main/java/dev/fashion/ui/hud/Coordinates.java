package dev.fashion.ui.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import dev.fashion.anim.Spring;
import dev.fashion.core.Modules;
import dev.fashion.gfx.Canvas;
import dev.fashion.gfx.Colors;
import dev.fashion.gfx.Font;
import dev.fashion.ui.Theme;

public final class Coordinates extends HudElement {
    private static final String[] DIRS = {"S", "W", "N", "E"};
    private String text = "0 0 0";
    private String nether = "";
    private String dir = "N";
    private final Spring heading = new Spring(0f, 18f, 0.8f);
    private boolean headingPlaced;

    public Coordinates() {
        super("coords", "Координаты", 0.008f, 0.985f);
    }

    @Override
    public boolean shown(boolean editing) {
        return Modules.hud.enabled() && Modules.hud.coords.on();
    }

    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) {
            return;
        }
        int bx = mc.player.getBlockX();
        int by = mc.player.getBlockY();
        int bz = mc.player.getBlockZ();
        text = bx + " " + by + " " + bz;
        boolean inNether = mc.world.getRegistryKey() == World.NETHER;
        nether = inNether ? (bx * 8) + " " + (bz * 8) : (bx / 8) + " " + (bz / 8);
        float yaw = MathHelper.wrapDegrees(mc.player.getYaw());
        dir = DIRS[Math.floorMod(Math.round(yaw / 90f), 4)];
    }

    @Override
    public float contentWidth() {
        return 40f + Font.semibold().width(text, 8.5f, 0f) + 12f + Font.regular().width(nether, 7.5f, 0f) + 10f;
    }

    @Override
    public float contentHeight() {
        return 20f;
    }

    @Override
    public void paintContent(Canvas c, float x, float y, float w, float h, float dt, boolean editing) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            float yaw = mc.player.getYaw();
            if (!headingPlaced) {
                heading.snap(yaw);
                headingPlaced = true;
            }
            float cur = heading.target;
            heading.to(cur + MathHelper.wrapDegrees(yaw - cur));
        }
        heading.update(dt);
        panel(c, x, y, w, h, 7f, lift.get());
        c.pushClip(x, y, w, h, 7f);
        float cy = y + h * 0.5f;
        float cx = x + 11f;
        c.shape(cx - 7f, cy - 7f, 14f, 14f).radius(7f).fill(Theme.INSET).border(c.px(), Colors.withAlpha(Theme.ACCENT_HI, 0.35f)).draw();
        c.push();
        c.rotateAround(cx, cy, (float) Math.toRadians(180f - heading.get()));
        c.shape(cx - 1.1f, cy - 5.5f, 2.2f, 5.5f).radius(1.1f).vertical(Theme.ACCENT_HI, Colors.withAlpha(Theme.ACCENT, 0.5f))
                .glow(3f, Colors.withAlpha(Theme.GLOW, 0.8f)).draw();
        c.shape(cx - 0.8f, cy, 1.6f, 4.5f).radius(0.8f).fill(Colors.withAlpha(Theme.TEXT_3, 0.8f)).draw();
        c.pop();
        float px = x + 22f;
        px += c.text(Font.bold(), 8.5f).color(Theme.ACCENT_HI).drawMid(dir, px, cy) + 6f;
        px += c.text(Font.semibold(), 8.5f).color(Theme.TEXT).drawMid(text, px, cy);
        px += 6f;
        c.shape(px, cy - 4.5f, c.px(), 9f).fill(0x33FFFFFF).draw();
        px += 6f;
        c.text(Font.regular(), 7.5f).color(Theme.TEXT_3).drawMid(nether, px, cy);
        c.popClip();
    }
}
