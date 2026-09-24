package dev.fashion.core.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.List;

public final class MultiSetting extends Setting<Integer> {
    private final List<String> options;

    public MultiSetting(String name, String description, List<String> options, boolean... defaults) {
        super(name, description, mask(defaults));
        this.options = options;
    }

    private static int mask(boolean[] flags) {
        int m = 0;
        for (int i = 0; i < flags.length; i++) {
            if (flags[i]) {
                m |= 1 << i;
            }
        }
        return m;
    }

    public List<String> options() {
        return options;
    }

    public boolean has(int i) {
        return (value & (1 << i)) != 0;
    }

    public void toggle(int i) {
        set(value ^ (1 << i));
    }

    @Override
    public JsonElement save() {
        JsonArray a = new JsonArray();
        for (int i = 0; i < options.size(); i++) {
            if (has(i)) {
                a.add(options.get(i));
            }
        }
        return a;
    }

    @Override
    public void load(JsonElement e) {
        if (!e.isJsonArray()) {
            return;
        }
        int m = 0;
        for (JsonElement el : e.getAsJsonArray()) {
            int i = options.indexOf(el.getAsString());
            if (i >= 0) {
                m |= 1 << i;
            }
        }
        value = m;
    }
}
