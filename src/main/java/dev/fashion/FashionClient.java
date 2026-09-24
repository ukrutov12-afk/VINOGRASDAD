package dev.fashion;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.fashion.core.Config;
import dev.fashion.core.Module;
import dev.fashion.core.Modules;
import dev.fashion.dev.AutoTest;
import dev.fashion.ui.ClickGui;
import dev.fashion.ui.hud.HudLayer;

public final class FashionClient implements ClientModInitializer {
    public static final String MOD_ID = "fashion";
    public static final String NAME = "Fashion";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

    private static final int GUI_KEY = GLFW.GLFW_KEY_RIGHT_SHIFT;

    private boolean configLoaded;
    private final boolean[] pressed = new boolean[GLFW.GLFW_KEY_LAST + 1];

    @Override
    public void onInitializeClient() {
        Modules.init();
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> Config.save());
        if (AutoTest.enabled()) {
            AutoTest.register();
        }
        LOGGER.info("{} ready: {} modules", NAME, Modules.all().size());
    }

    private void tick(MinecraftClient mc) {
        if (!configLoaded) {
            Config.load();
            configLoaded = true;
        }
        pollKeys(mc);
        if (mc.player != null && mc.world != null) {
            Modules.tick();
            HudLayer.tick();
        }
        Config.tick();
    }

    private void pollKeys(MinecraftClient mc) {
        if (mc.currentScreen != null || mc.player == null) {
            java.util.Arrays.fill(pressed, false);
            return;
        }
        Window window = mc.getWindow();
        check(window, GUI_KEY, () -> mc.setScreen(new ClickGui()));
        for (Module m : Modules.all()) {
            int key = m.key();
            if (key > 0 && key <= GLFW.GLFW_KEY_LAST && key != GUI_KEY) {
                check(window, key, m::toggle);
            }
        }
    }

    private void check(Window window, int key, Runnable action) {
        boolean down = InputUtil.isKeyPressed(window, key);
        if (down && !pressed[key]) {
            action.run();
        }
        pressed[key] = down;
    }
}
