package dev.nexus.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/** ARGB color stored as an int. Optional rainbow mode (hue cycles over time). */
public final class ColorSetting extends Setting<Integer> {
    private boolean rainbow;

    public ColorSetting(String name, String description, int argb) { super(name, description, argb); }

    public boolean isRainbow() { return rainbow; }
    public void setRainbow(boolean r) { this.rainbow = r; }

    /** Current color; when rainbow is on, hue is derived from the system clock. */
    public int color() {
        if (!rainbow) return value;
        float hue = (System.currentTimeMillis() % 6000L) / 6000f;
        return 0xFF000000 | (java.awt.Color.HSBtoRGB(hue, 0.75f, 1f) & 0xFFFFFF);
    }

    @Override public JsonElement toJson() {
        JsonObject o = new JsonObject();
        o.add("argb", new JsonPrimitive(value));
        o.add("rainbow", new JsonPrimitive(rainbow));
        return o;
    }

    @Override public void fromJson(JsonElement e) {
        if (e == null || !e.isJsonObject()) return;
        JsonObject o = e.getAsJsonObject();
        if (o.has("argb") && o.get("argb").isJsonPrimitive()) value = o.get("argb").getAsInt();
        if (o.has("rainbow") && o.get("rainbow").isJsonPrimitive()) rainbow = o.get("rainbow").getAsBoolean();
    }
}
