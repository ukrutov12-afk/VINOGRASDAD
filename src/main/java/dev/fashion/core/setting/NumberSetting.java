package dev.fashion.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Locale;

public final class NumberSetting extends Setting<Double> {
    private final double min;
    private final double max;
    private final double step;
    private final String unit;

    public NumberSetting(String name, String description, double value, double min, double max, double step, String unit) {
        super(name, description, value);
        this.min = min;
        this.max = max;
        this.step = step;
        this.unit = unit;
    }

    public double min() {
        return min;
    }

    public double max() {
        return max;
    }

    public double step() {
        return step;
    }

    public float asFloat() {
        return value.floatValue();
    }

    public double asDouble() {
        return value;
    }

    public double fraction() {
        return (value - min) / (max - min);
    }

    public void setFraction(double f) {
        double raw = min + Math.max(0, Math.min(1, f)) * (max - min);
        double snapped = Math.round(raw / step) * step;
        set(Math.max(min, Math.min(max, Math.round(snapped * 1e6) / 1e6)));
    }

    public String format(double v) {
        int decimals = step >= 1 ? 0 : step >= 0.1 ? 1 : 2;
        return String.format(Locale.ROOT, "%." + decimals + "f", v) + unit;
    }

    @Override
    public JsonElement save() {
        return new JsonPrimitive(value);
    }

    @Override
    public void load(JsonElement e) {
        if (e.isJsonPrimitive()) {
            value = Math.max(min, Math.min(max, e.getAsDouble()));
        }
    }
}
