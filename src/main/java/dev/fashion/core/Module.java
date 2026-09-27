package dev.fashion.core;

import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

import dev.fashion.core.setting.BoolSetting;
import dev.fashion.core.setting.ModeSetting;
import dev.fashion.core.setting.MultiSetting;
import dev.fashion.core.setting.NumberSetting;
import dev.fashion.core.setting.Setting;

public abstract class Module {
    protected static final MinecraftClient mc = MinecraftClient.getInstance();

    private final String name;
    private final String description;
    private final Category category;
    private final char icon;
    private final List<Setting<?>> settings = new ArrayList<>();
    private boolean enabled;
    private int key = -1;

    protected Module(String name, String description, Category category, char icon) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.icon = icon;
    }

    public char icon() {
        return icon;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public Category category() {
        return category;
    }

    public List<Setting<?>> settings() {
        return settings;
    }

    public boolean enabled() {
        return enabled;
    }

    public int key() {
        return key;
    }

    public void setKey(int key) {
        if (this.key == key) {
            return;
        }
        this.key = key;
        Config.markDirty();
        if (!Config.loading()) {
            Events.bind(this);
        }
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public void setEnabled(boolean on) {
        if (on == enabled) {
            return;
        }
        enabled = on;
        if (on) {
            onEnable();
        } else {
            onDisable();
        }
        Config.markDirty();
        if (!Config.loading()) {
            Events.toggled(this);
        }
    }

    public String suffix() {
        return null;
    }

    protected void onEnable() {
    }

    protected void onDisable() {
    }

    public void onTick() {
    }

    protected <S extends Setting<?>> S add(S s) {
        settings.add(s);
        s.onChange(Config::markDirty);
        return s;
    }

    protected BoolSetting bool(String name, String description, boolean value) {
        return add(new BoolSetting(name, description, value));
    }

    protected NumberSetting number(String name, String description, double value, double min, double max, double step, String unit) {
        return add(new NumberSetting(name, description, value, min, max, step, unit));
    }

    protected ModeSetting mode(String name, String description, int value, String... options) {
        return add(new ModeSetting(name, description, value, options));
    }

    protected MultiSetting multi(String name, String description, List<String> options, boolean... defaults) {
        return add(new MultiSetting(name, description, options, defaults));
    }
}
