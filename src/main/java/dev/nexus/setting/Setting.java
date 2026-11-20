package dev.nexus.setting;

import com.google.gson.JsonElement;
import java.util.function.BooleanSupplier;

/** Base class for a module setting. Serialization is handled by subclasses. */
public abstract class Setting<T> {
    private final String name;
    private final String description;
    protected T value;
    private final T defaultValue;
    private BooleanSupplier visible = () -> true;

    protected Setting(String name, String description, T defaultValue) {
        this.name = name;
        this.description = description;
        this.value = defaultValue;
        this.defaultValue = defaultValue;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public T get() { return value; }
    public void set(T v) { this.value = v; }
    public void reset() { this.value = defaultValue; }
    public boolean isVisible() { return visible.getAsBoolean(); }

    /** Show this setting in the GUI only while the condition holds. */
    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S visibleWhen(BooleanSupplier condition) {
        this.visible = condition;
        return (S) this;
    }

    public abstract JsonElement toJson();
    public abstract void fromJson(JsonElement element);
}
