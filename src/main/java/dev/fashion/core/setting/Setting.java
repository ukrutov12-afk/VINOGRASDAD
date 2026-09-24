package dev.fashion.core.setting;

import com.google.gson.JsonElement;

import java.util.function.BooleanSupplier;

public abstract class Setting<T> {
    private final String name;
    private final String description;
    protected T value;
    private BooleanSupplier visible = () -> true;
    private Runnable listener = () -> {
    };

    protected Setting(String name, String description, T value) {
        this.name = name;
        this.description = description;
        this.value = value;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public T get() {
        return value;
    }

    public void set(T v) {
        if (v == null || v.equals(value)) {
            return;
        }
        value = v;
        listener.run();
    }

    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S visibleWhen(BooleanSupplier supplier) {
        visible = supplier;
        return (S) this;
    }

    public boolean visible() {
        return visible.getAsBoolean();
    }

    public void onChange(Runnable r) {
        listener = r;
    }

    public abstract JsonElement save();

    public abstract void load(JsonElement e);
}
