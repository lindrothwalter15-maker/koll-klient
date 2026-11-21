package dev.nexus.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class RenderUtil {
    private RenderUtil() {}

    /** Multiplies the alpha channel of an ARGB color by mul (0..1). */
    public static int alpha(int argb, float mul) {
        float m = Math.max(0f, Math.min(1f, mul));
        int a = (int) (((argb >>> 24) & 0xFF) * m);
        return (a << 24) | (argb & 0xFFFFFF);
    }

    public static int width(String s) {
        return MinecraftClient.getInstance().textRenderer.getWidth(s);
    }

    public static void text(DrawContext c, String s, int x, int y, int color) {
        c.drawText(MinecraftClient.getInstance().textRenderer, s, x, y, color, true);
    }

    public static float approach(float cur, float target, float step) {
        if (cur < target) return Math.min(target, cur + step);
        return Math.max(target, cur - step);
    }

    public static int rainbow(int index) {
        float hue = ((System.currentTimeMillis() / 20L + index * 40L) % 360L) / 360f;
        return 0xFF000000 | (java.awt.Color.HSBtoRGB(hue, 0.7f, 1f) & 0xFFFFFF);
    }
}
