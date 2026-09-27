package dev.fashion.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import dev.fashion.FashionClient;
import dev.fashion.core.setting.Setting;
import dev.fashion.ui.hud.HudLayer;

public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static boolean dirty;
    private static boolean loading;
    private static long dirtySince;
    private static JsonObject ui = new JsonObject();

    private Config() {
    }

    public static Path file() {
        return MinecraftClient.getInstance().runDirectory.toPath().resolve("config").resolve("fashion.json");
    }

    public static void markDirty() {
        if (loading) {
            return;
        }
        if (!dirty) {
            dirtySince = System.currentTimeMillis();
        }
        dirty = true;
    }

    public static boolean loading() {
        return loading;
    }

    public static JsonObject ui() {
        return ui;
    }

    public static void tick() {
        if (dirty && System.currentTimeMillis() - dirtySince > 800L) {
            save();
        }
    }

    public static void load() {
        Path f = file();
        if (!Files.exists(f)) {
            return;
        }
        loading = true;
        try (Reader r = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
            JsonObject mods = root.has("modules") ? root.getAsJsonObject("modules") : new JsonObject();
            for (Module m : Modules.all()) {
                if (!mods.has(m.name())) {
                    continue;
                }
                JsonObject o = mods.getAsJsonObject(m.name());
                if (o.has("key")) {
                    m.setKey(o.get("key").getAsInt());
                }
                if (o.has("settings")) {
                    JsonObject s = o.getAsJsonObject("settings");
                    for (Setting<?> setting : m.settings()) {
                        JsonElement e = s.get(setting.name());
                        if (e != null) {
                            setting.load(e);
                        }
                    }
                }
                if (o.has("enabled")) {
                    m.setEnabled(o.get("enabled").getAsBoolean());
                }
            }
            if (root.has("hud")) {
                HudLayer.load(root.getAsJsonObject("hud"));
            }
            if (root.has("ui")) {
                ui = root.getAsJsonObject("ui");
            }
            Events.config(true, true);
        } catch (Exception e) {
            FashionClient.LOGGER.error("Config load failed", e);
            Events.error("Конфиг не загружен", e.getClass().getSimpleName());
        } finally {
            loading = false;
            dirty = false;
        }
    }

    public static void saveNow() {
        save();
        Events.config(false, true);
    }

    public static void save() {
        dirty = false;
        JsonObject root = new JsonObject();
        JsonObject mods = new JsonObject();
        for (Module m : Modules.all()) {
            JsonObject o = new JsonObject();
            o.addProperty("enabled", m.enabled());
            o.addProperty("key", m.key());
            JsonObject s = new JsonObject();
            for (Setting<?> setting : m.settings()) {
                s.add(setting.name(), setting.save());
            }
            o.add("settings", s);
            mods.add(m.name(), o);
        }
        root.add("modules", mods);
        root.add("hud", HudLayer.save());
        root.add("ui", ui);
        Path f = file();
        try {
            Files.createDirectories(f.getParent());
            Path tmp = f.resolveSibling("fashion.json.tmp");
            try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(root, w);
            }
            Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            FashionClient.LOGGER.error("Config save failed", e);
            Events.error("Конфиг не сохранён", e.getClass().getSimpleName());
        }
    }
}
