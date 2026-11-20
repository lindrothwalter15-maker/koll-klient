package dev.nexus.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class NumberSetting extends Setting<Double> {
    private final double min, max, step;

    public NumberSetting(String name, String description, double def, double min, double max, double step) {
        super(name, description, clamp(def, min, max));
        this.min = min; this.max = max; this.step = step;
    }

    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }

    @Override public void set(Double v) {
        double s = step > 0 ? Math.round(v / step) * step : v;
        value = clamp(s, min, max);
    }

    public double getMin() { return min; }
    public double getMax() { return max; }
    public double getStep() { return step; }
    public float asFloat() { return value.floatValue(); }
    public int asInt() { return (int) Math.round(value); }

    @Override public JsonElement toJson() { return new JsonPrimitive(value); }
    @Override public void fromJson(JsonElement e) {
        if (e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) set(e.getAsDouble());
    }
}
