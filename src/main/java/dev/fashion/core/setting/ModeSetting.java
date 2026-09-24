package dev.fashion.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.List;

public final class ModeSetting extends Setting<Integer> {
    private final List<String> options;

    public ModeSetting(String name, String description, int value, String... options) {
        super(name, description, value);
        this.options = List.of(options);
    }

    public List<String> options() {
        return options;
    }

    public int index() {
        return value;
    }

    public String mode() {
        return options.get(value);
    }

    public boolean is(int index) {
        return value == index;
    }

    @Override
    public JsonElement save() {
        return new JsonPrimitive(options.get(value));
    }

    @Override
    public void load(JsonElement e) {
        if (e.isJsonPrimitive()) {
            int i = options.indexOf(e.getAsString());
            if (i >= 0) {
                value = i;
            }
        }
    }
}
