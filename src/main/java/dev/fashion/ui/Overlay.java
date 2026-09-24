package dev.fashion.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;

import dev.fashion.anim.Clock;
import dev.fashion.dev.AutoTest;
import dev.fashion.gfx.Blur;
import dev.fashion.gfx.Canvas;
import dev.fashion.gfx.Font;
import dev.fashion.ui.hud.HudLayer;

public final class Overlay {
    private static boolean glassLastFrame = true;

    private Overlay() {
    }

    private static boolean active(MinecraftClient mc) {
        return mc.world != null || mc.currentScreen instanceof FashionScreen;
    }

    public static void beforeGui() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!active(mc) || !Font.get().ready()) {
            return;
        }
        if (glassLastFrame || mc.currentScreen instanceof FashionScreen) {
            Blur.update(mc.getFramebuffer());
        }
    }

    public static void afterGui() {
        MinecraftClient mc = MinecraftClient.getInstance();
        float dt = Clock.tick();
        if (!active(mc) || !Font.get().ready()) {
            return;
        }
        Framebuffer fb = mc.getFramebuffer();
        Canvas c = Canvas.get();
        c.begin(fb.textureWidth, fb.textureHeight, 1f);
        if (mc.world != null && mc.player != null && !mc.options.hudHidden) {
            HudLayer.paint(c, dt);
        }
        if (mc.currentScreen instanceof FashionScreen screen) {
            screen.paintFrame(c, dt);
        }
        glassLastFrame = c.usedGlass();
        c.flush(fb, Blur.texture(), Blur.ready(), Clock.time());
        if (AutoTest.enabled()) {
            AutoTest.frame();
        }
    }
}
