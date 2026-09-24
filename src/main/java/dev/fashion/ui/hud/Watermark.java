package dev.fashion.ui.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;

import java.time.LocalTime;

import dev.fashion.anim.Clock;
import dev.fashion.anim.Motion;
import dev.fashion.anim.Spring;
import dev.fashion.core.Modules;
import dev.fashion.gfx.Canvas;
import dev.fashion.gfx.Colors;
import dev.fashion.gfx.Font;
import dev.fashion.ui.Draw;
import dev.fashion.ui.Theme;

public final class Watermark extends HudElement {
    private final Spring fps = new Spring(60f, Motion.SOFT);
    private String fpsText = "60";
    private String pingText = "0";
    private String timeText = "00:00";
    private float refresh;

    public Watermark() {
        super("watermark", "Вотермарка", 0.008f, 0.012f);
    }

    @Override
    public boolean shown(boolean editing) {
        return Modules.hud.enabled() && Modules.hud.watermark.on();
    }

    private float partsWidth() {
        float w = 0f;
        w += Font.display().width("FASHION", 8.5f, 0.07f);
        w += Font.semibold().width(fpsText, 8.5f, 0f) + Font.regular().width(" fps", 8f, 0f);
        w += Font.semibold().width(pingText, 8.5f, 0f) + Font.regular().width(" ms", 8f, 0f);
        w += Font.semibold().width(timeText, 8.5f, 0f);
        return w;
    }

    @Override
    public float contentWidth() {
        return 30f + partsWidth() + 3 * 15f + 10f;
    }

    @Override
    public float contentHeight() {
        return 22f;
    }

    @Override
    public void paintContent(Canvas c, float x, float y, float w, float h, float dt, boolean editing) {
        MinecraftClient mc = MinecraftClient.getInstance();
        fps.to(mc.getCurrentFps());
        fps.update(dt);
        refresh -= dt;
        if (refresh <= 0f) {
            refresh = 0.25f;
            fpsText = String.valueOf(Math.round(fps.get()));
            int ping = 0;
            if (mc.getNetworkHandler() != null && mc.player != null) {
                PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                if (e != null) {
                    ping = e.getLatency();
                }
            }
            pingText = String.valueOf(ping);
            LocalTime t = LocalTime.now();
            timeText = String.format("%02d:%02d", t.getHour(), t.getMinute());
        }
        panel(c, x, y, w, h, 8f, lift.get());
        c.pushClip(x, y, w, h, 8f);
        float cy = y + h * 0.5f;
        float tw = 0.5f + 0.5f * (float) Math.sin(Clock.time() * 1.3f);
        float sx = x + 13f;
        c.shape(sx - 3f, cy - 3f, 6f, 6f).radius(3f).fill(0).glow(8f + 2f * tw, Colors.withAlpha(0xFFBCB0FF, 0.45f + 0.15f * tw)).draw();
        Draw.chromeIcon(c, '\uE006', sx, cy, 11.5f + 0.4f * tw, 2.5f, Colors.withAlpha(0xFFFFFFFF, 0.7f));
        float px = x + 25f;
        px += c.text(Font.display(), 8.5f).color(0xFFFFFFFF).chrome(1f).tracking(0.07f).drawMid("FASHION", px, cy);
        px = separator(c, px, cy);
        px += c.text(Font.semibold(), 8.5f).color(Theme.TEXT).drawMid(fpsText, px, cy);
        px += c.text(Font.regular(), 8f).color(Theme.TEXT_3).drawMid(" fps", px, cy);
        px = separator(c, px, cy);
        px += c.text(Font.semibold(), 8.5f).color(Theme.TEXT).drawMid(pingText, px, cy);
        px += c.text(Font.regular(), 8f).color(Theme.TEXT_3).drawMid(" ms", px, cy);
        px = separator(c, px, cy);
        c.text(Font.semibold(), 8.5f).color(Theme.TEXT_2).drawMid(timeText, px, cy);
        c.popClip();
    }

    private static float separator(Canvas c, float px, float cy) {
        c.shape(px + 6f, cy - 1.25f, 2.5f, 2.5f).radius(1.25f).fill(Colors.withAlpha(Theme.ACCENT_HI, 0.8f))
                .glow(3f, Colors.withAlpha(Theme.GLOW, 0.8f)).draw();
        return px + 15f;
    }
}
