package dev.nexus.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.List;

public final class ModeSetting extends Setting<String> {
    private final List<String> modes;

    public ModeSetting(String name, String description, String def, String... modes) {
        super(name, description, def);
        this.modes = List.of(modes);
        if (!this.modes.contains(def)) throw new IllegalArgumentException("Default '" + def + "' not in modes");
    }

    public List<String> getModes() { return modes; }
    public boolean is(String mode) { return value.equals(mode); }

    public void cycle() {
        int i = modes.indexOf(value);
        value = modes.get((i + 1) % modes.size());
    }

    @Override public void set(String v) { if (modes.contains(v)) value = v; }
    @Override public JsonElement toJson() { return new JsonPrimitive(value); }
    @Override public void fromJson(JsonElement e) {
        if (e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) set(e.getAsString());
    }
}
