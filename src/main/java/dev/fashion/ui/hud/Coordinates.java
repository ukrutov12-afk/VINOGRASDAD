package dev.fashion.ui.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.world.World;

import dev.fashion.core.Modules;
import dev.fashion.gfx.Canvas;
import dev.fashion.gfx.Font;
import dev.fashion.ui.Draw;
import dev.fashion.ui.Theme;

public final class Coordinates extends HudElement {
    private String text = "0  0  0";
    private String nether = "";

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
        text = bx + "  " + by + "  " + bz;
        boolean inNether = mc.world.getRegistryKey() == World.NETHER;
        nether = inNether ? (bx * 8) + "  " + (bz * 8) : (bx / 8) + "  " + (bz / 8);
    }

    @Override
    public float contentWidth() {
        return 34f + Font.semibold().width(text, 8.5f, 0f) + 16f + Font.regular().width(nether, 8f, 0f) + 22f;
    }

    @Override
    public float contentHeight() {
        return 22f;
    }

    @Override
    public void paintContent(Canvas c, float x, float y, float w, float h, float dt, boolean editing) {
        panel(c, x, y, w, h, 8f, lift.get());
        c.pushClip(x, y, w, h, 8f);
        float cy = y + h * 0.5f;
        Draw.icon(c, '\uE013', x + 12f, cy, 10f, Theme.ACCENT_HI, 3f, 0x99805CFF);
        float px = x + 23f;
        px += c.text(Font.semibold(), 8.5f).color(Theme.TEXT).drawMid(text, px, cy);
        px += 9f;
        c.shape(px, cy - 5f, c.px(), 10f).fill(0x33FFFFFF).draw();
        px += 7f;
        c.text(Font.regular(), 8f).color(Theme.TEXT_3).drawMid(nether, px, cy);
        c.popClip();
    }
}
