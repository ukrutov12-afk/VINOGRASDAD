package dev.fashion.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class BoolSetting extends Setting<Boolean> {
    public BoolSetting(String name, String description, boolean value) {
        super(name, description, value);
    }

    public boolean on() {
        return value;
    }

    public void toggle() {
        set(!value);
    }

    @Override
    public JsonElement save() {
        return new JsonPrimitive(value);
    }

    @Override
    public void load(JsonElement e) {
        if (e.isJsonPrimitive()) {
            value = e.getAsBoolean();
        }
    }
}
