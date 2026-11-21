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

    /** Linear blend of two ARGB colors. */
    public static int lerp(int c1, int c2, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a = (int) (((c1 >>> 24) & 0xFF) * (1 - t) + ((c2 >>> 24) & 0xFF) * t);
        int r = (int) (((c1 >> 16) & 0xFF) * (1 - t) + ((c2 >> 16) & 0xFF) * t);
        int g = (int) (((c1 >> 8) & 0xFF) * (1 - t) + ((c2 >> 8) & 0xFF) * t);
        int b = (int) ((c1 & 0xFF) * (1 - t) + (c2 & 0xFF) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /** Filled rectangle with rounded corners (radius r), drawn with horizontal strips. */
    public static void roundRect(DrawContext c, int x1, int y1, int x2, int y2, int r, int color) {
        int w = x2 - x1, h = y2 - y1;
        if (w <= 0 || h <= 0) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        if (r == 0) { c.fill(x1, y1, x2, y2, color); return; }
        c.fill(x1, y1 + r, x2, y2 - r, color);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int dx = r - (int) Math.round(Math.sqrt(r * r - dy * dy));
            c.fill(x1 + dx, y1 + i, x2 - dx, y1 + i + 1, color);
            c.fill(x1 + dx, y2 - i - 1, x2 - dx, y2 - i, color);
        }
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
