package dev.fashion.dev;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.WorldPresets;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.rule.GameRules;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;

import dev.fashion.FashionClient;
import dev.fashion.anim.Clock;
import dev.fashion.core.Category;
import dev.fashion.core.Modules;
import dev.fashion.gfx.Blur;
import dev.fashion.ui.ClickGui;
import dev.fashion.ui.FashionScreen;
import dev.fashion.ui.hud.HudEditorScreen;
import dev.fashion.ui.hud.HudElement;
import dev.fashion.ui.hud.HudLayer;

public final class AutoTest {
    private static final Deque<Step> STEPS = new ArrayDeque<>();
    private static int wait;
    private static boolean worldRequested;
    private static int worldTicks;
    private static boolean running;
    private static String recording;
    private static int recordLeft;
    private static int recordIndex;
    private static Path out;

    private interface Step {
        void run(MinecraftClient mc);
    }

    private AutoTest() {
    }

    public static boolean enabled() {
        return Boolean.getBoolean("fashion.autotest");
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(AutoTest::tick);
    }

    private static void tick(MinecraftClient mc) {
        if (!worldRequested && mc.currentScreen instanceof TitleScreen) {
            worldRequested = true;
            out = mc.runDirectory.toPath().resolve("autotest");
            try {
                Files.createDirectories(out);
            } catch (Exception e) {
                FashionClient.LOGGER.error("autotest dir", e);
            }
            LevelInfo info = new LevelInfo("fashion_autotest", GameMode.CREATIVE, false, Difficulty.EASY, true,
                    new GameRules(FeatureFlags.DEFAULT_ENABLED_FEATURES), DataConfiguration.SAFE_MODE);
            mc.createIntegratedServerLoader().createAndStart("fashion_autotest_" + System.currentTimeMillis(), info,
                    new GeneratorOptions(20240613L, true, false), WorldPresets::createDemoOptions, null);
            return;
        }
        if (mc.player != null && mc.world != null && !running) {
            worldTicks++;
            if (worldTicks == 100) {
                setupWorld(mc);
            }
            if (worldTicks == 190) {
                mc.inGameHud.getChatHud().clear(false);
            }
            if (worldTicks == 200) {
                running = true;
                Clock.fixed(1f / 60f);
                script();
            }
        }
    }

    private static void command(MinecraftClient mc, String cmd) {
        MinecraftServer server = mc.getServer();
        if (server != null) {
            server.execute(() -> server.getCommandManager().parseAndExecute(server.getCommandSource(), cmd));
        }
    }

    private static void setupWorld(MinecraftClient mc) {
        String name = mc.player.getName().getString();
        command(mc, "gamerule send_command_feedback false");
        command(mc, "gamerule spawn_mobs false");
        command(mc, "gamerule advance_time false");
        command(mc, "gamerule advance_weather false");
        command(mc, "kill @e[type=!minecraft:player]");
        command(mc, "time set 12200");
        command(mc, "weather clear");
        command(mc, "tp " + name + " ~ ~ ~ 90 4");
        command(mc, "item replace entity " + name + " hotbar.0 with minecraft:iron_sword");
        command(mc, "execute at " + name + " rotated 90 0 run summon minecraft:zombie ^0.3 ^ ^2.6 {NoAI:1b,PersistenceRequired:1b,equipment:{head:{id:\"minecraft:leather_helmet\",count:1}}}");
        command(mc, "execute at " + name + " rotated 90 0 run summon minecraft:mannequin ^-1.2 ^ ^3.6 {NoAI:1b}");
        command(mc, "execute as @e[type=!minecraft:player] at @s run tp @s ~ ~ ~ facing entity " + name + " feet");
    }

    private static void script() {
        MinecraftClient mc = MinecraftClient.getInstance();
        add(m -> shot("00_world_hud"));
        add(m -> {
            m.setScreen(new ClickGui());
            record("open", 70);
        });
        waitFrames(72);
        add(m -> shot("01_gui_combat"));
        add(m -> {
            hoverCard(m, 0, 20f);
            record("hover", 40);
        });
        waitFrames(42);
        add(m -> shot("02_gui_hover"));
        add(m -> {
            hoverCard(m, 1, 20f);
            Modules.attackAura.settings().stream().filter(s -> s.name().equals("Наведение")).findFirst()
                    .ifPresent(s -> ((dev.fashion.core.setting.ModeSetting) s).set(2));
            record("mode", 50);
        });
        waitFrames(52);
        add(m -> hoverCard(m, 0, 128f));
        waitFrames(70);
        add(m -> shot("02b_tooltip"));
        add(m -> gui().expandForTest("TargetESP", false));
        add(m -> record("collapse", 50));
        waitFrames(52);
        add(m -> shot("03_gui_collapse"));
        add(m -> gui().expandForTest("TargetESP", true));
        waitFrames(30);
        add(m -> {
            gui().selectForTest(Category.MOVEMENT);
            record("switch", 60);
        });
        waitFrames(40);
        add(m -> {
            Modules.byName("AutoSprint").setEnabled(true);
            record("toggle", 40);
        });
        waitFrames(42);
        add(m -> shot("04_gui_toggle"));
        add(m -> {
            gui().selectForTest(Category.RENDER);
        });
        waitFrames(60);
        add(m -> shot("05_gui_render"));
        add(m -> {
            gui().typeForTest("a");
            record("search", 50);
        });
        waitFrames(52);
        add(m -> shot("06_gui_search"));
        add(m -> gui().selectForTest(Category.COMBAT));
        waitFrames(50);
        add(m -> {
            FashionScreen.testMouseX = m.getFramebuffer().textureWidth * 0.6f;
            FashionScreen.testMouseY = m.getFramebuffer().textureHeight * 0.5f;
            gui().scrollForTest(-5f);
            record("scroll", 90);
        });
        waitFrames(92);
        add(m -> shot("07_gui_scrolled"));
        add(m -> {
            gui().scrollForTest(8f);
            gui().dragForTest(-160f, 26f);
            record("drag", 60);
        });
        waitFrames(62);
        add(m -> shot("08_gui_dragged"));
        add(m -> {
            gui().close();
            record("close", 60);
        });
        waitFrames(62);
        add(m -> {
            FashionScreen.testMouseX = -1f;
            Modules.attackAura.setEnabled(true);
            Modules.targetEsp.setEnabled(true);
            record("target", 170);
        });
        waitFrames(60);
        add(m -> shot("09_target_hud"));
        waitFrames(112);
        add(m -> shot("10_target_switch"));
        add(m -> m.setScreen(new HudEditorScreen()));
        waitFrames(30);
        add(m -> {
            HudElement wm = HudLayer.elements().get(0);
            HudLayer.beginDrag(wm, wm.x.get() + 10f, wm.y.get() + 10f);
            record("hudedit", 80);
        });
        for (int i = 0; i < 40; i++) {
            final int k = i;
            add(m -> {
                HudElement wm = HudLayer.elements().get(0);
                float tx = 10f + (HudLayer.screenWidth() * 0.5f - wm.w.target * 0.5f + 4f) * (k / 39f);
                HudLayer.drag(tx + 10f, 10f + 60f * (float) Math.sin(k / 39f * Math.PI) + 10f);
            });
        }
        waitFrames(20);
        add(m -> shot("11_hud_editor_snap"));
        add(m -> HudLayer.endDrag());
        waitFrames(25);
        add(m -> {
            HudLayer.resetPositions();
            m.currentScreen.close();
        });
        waitFrames(50);
        add(m -> m.setScreen(new ClickGui()));
        waitFrames(90);
        add(m -> shot("12_gui_reopen"));
        waitFrames(40);
        add(m -> FashionClient.LOGGER.info("autotest: blur recomputes={} reuses={}", Blur.recomputes(), Blur.reuses()));
        add(m -> {
            FashionClient.LOGGER.info("autotest: done");
            m.scheduleStop();
        });
    }

    private static ClickGui gui() {
        return (ClickGui) MinecraftClient.getInstance().currentScreen;
    }

    private static void hoverCard(MinecraftClient mc, int column, float dy) {
        ClickGui g = gui();
        float s = g.uiScale();
        float lx = g.windowX() + ClickGui.SIDE + 14f + (column == 0 ? 70f : 290f);
        float ly = g.windowY() + 70f + dy;
        FashionScreen.testMouseX = lx * s;
        FashionScreen.testMouseY = ly * s;
    }

    private static void add(Step s) {
        STEPS.add(s);
    }

    private static void waitFrames(int n) {
        for (int i = 0; i < n; i++) {
            STEPS.add(m -> {
            });
        }
    }

    private static void record(String name, int frames) {
        recording = name;
        recordLeft = frames;
        recordIndex = 0;
    }

    private static void shot(String name) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Path target = out.resolve(name + ".png");
        ScreenshotRecorder.takeScreenshot(mc.getFramebuffer(), img -> {
            try (img) {
                img.writeTo(target);
            } catch (Exception e) {
                FashionClient.LOGGER.error("autotest shot", e);
            }
        });
        FashionClient.LOGGER.info("autotest: shot {}", name);
    }

    public static void frame() {
        if (!running) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (recording != null && recordLeft > 0) {
            Path dir = out.resolve("seq_" + recording);
            String file = String.format("%03d.png", recordIndex++);
            try {
                Files.createDirectories(dir);
            } catch (Exception ignored) {
            }
            ScreenshotRecorder.takeScreenshot(mc.getFramebuffer(), 2, img -> {
                try (img) {
                    img.writeTo(dir.resolve(file));
                } catch (Exception e) {
                    FashionClient.LOGGER.error("autotest seq", e);
                }
            });
            recordLeft--;
            if (recordLeft == 0) {
                recording = null;
            }
        }
        if (wait > 0) {
            wait--;
            return;
        }
        Step s = STEPS.poll();
        if (s != null) {
            s.run(mc);
        }
    }
}
