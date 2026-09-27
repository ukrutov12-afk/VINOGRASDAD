package dev.fashion.ui.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;

import java.time.LocalTime;

import dev.fashion.anim.Clock;
import dev.fashion.core.Modules;
import dev.fashion.gfx.Canvas;
import dev.fashion.gfx.Colors;
import dev.fashion.gfx.Font;
import dev.fashion.ui.Logo;
import dev.fashion.ui.Rolling;
import dev.fashion.ui.Theme;

public final class Watermark extends HudElement {
    private static final float SIZE = 8.5f;
    private final Rolling fps = new Rolling();
    private final Rolling ping = new Rolling();
    private final Rolling time = new Rolling();
    private float refresh;
    private float smoothFps = 60f;

    public Watermark() {
        super("watermark", "Вотермарка", 0.008f, 0.012f);
    }

    @Override
    public boolean shown(boolean editing) {
        return Modules.hud.enabled() && Modules.hud.watermark.on();
    }

    private float digitW() {
        return Font.semibold().width("0", SIZE, 0f);
    }

    @Override
    public float contentWidth() {
        float name = Font.display().width("FASHION", 8.5f, 0.07f);
        float units = Font.regular().width(" fps", 8f, 0f) + Font.regular().width(" ms", 8f, 0f);
        float digits = digitW() * (String.valueOf(Math.round(smoothFps)).length() + pingLength() + 5);
        return 30f + name + 3 * 15f + units + digits + 10f;
    }

    private int pingLength() {
        return Math.max(1, currentPing().length());
    }

    private String currentPing() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() != null && mc.player != null) {
            PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (e != null) {
                return String.valueOf(e.getLatency());
            }
        }
        return "0";
    }

    @Override
    public float contentHeight() {
        return 24f;
    }

    @Override
    public void paintContent(Canvas c, float x, float y, float w, float h, float dt, boolean editing) {
        MinecraftClient mc = MinecraftClient.getInstance();
        smoothFps += (mc.getCurrentFps() - smoothFps) * Math.min(1f, dt * 3f);
        refresh -= dt;
        if (refresh <= 0f) {
            refresh = 0.3f;
            fps.set(String.valueOf(Math.round(smoothFps)));
            ping.set(currentPing());
            LocalTime t = LocalTime.now();
            time.set(String.format("%02d:%02d", t.getHour(), t.getMinute()));
        }
        float lf = lift.get();
        panel(c, x, y, w, h, 9f, lf);
        c.shape(x, y, w, h).radius(9f).fill(0).glow(9f, Colors.withAlpha(Theme.GLOW, 0.12f + 0.05f * (float) Math.sin(Clock.time() * 1.2f))).draw();
        c.pushClip(x, y, w, h, 9f);
        float cy = y + h * 0.5f;
        Logo.draw(c, x + 14f, cy, 18f, (float) Math.sin(Clock.time() * 0.6f) * 0.08f, 0f);
        float px = x + 27f;
        px += c.text(Font.display(), 8.5f).color(0xFFFFFFFF).chrome(1f).tracking(0.07f).drawMid("FASHION", px, cy);
        px = separator(c, px, cy);
        px += fps.draw(c, Font.semibold(), SIZE, Theme.TEXT, px, cy, dt);
        px += c.text(Font.regular(), 8f).color(Theme.TEXT_3).drawMid(" fps", px, cy);
        px = separator(c, px, cy);
        px += ping.draw(c, Font.semibold(), SIZE, Theme.TEXT, px, cy, dt);
        px += c.text(Font.regular(), 8f).color(Theme.TEXT_3).drawMid(" ms", px, cy);
        px = separator(c, px, cy);
        time.draw(c, Font.semibold(), SIZE, Theme.TEXT_2, px, cy, dt);
        c.popClip();
    }

    private static float separator(Canvas c, float px, float cy) {
        c.shape(px + 6.5f, cy - 5f, c.px(), 10f).fill(Colors.withAlpha(Theme.ACCENT_HI, 0.35f)).draw();
        return px + 14f;
    }
}
