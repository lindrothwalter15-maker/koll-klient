package dev.nexus.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.util.math.Vec3d;

public final class MoveUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private MoveUtil() {}

    public static boolean moving() {
        GameOptions o = mc.options;
        return o.forwardKey.isPressed() || o.backKey.isPressed() || o.leftKey.isPressed() || o.rightKey.isPressed();
    }

    /**
     * Sets horizontal velocity to {@code speed} (blocks/tick) in the direction of the movement keys,
     * relative to where you look. Vertical velocity is kept. Returns false when no key is held.
     */
    public static boolean setHorizontal(double speed, boolean zeroWhenIdle) {
        GameOptions o = mc.options;
        double f = (o.forwardKey.isPressed() ? 1 : 0) - (o.backKey.isPressed() ? 1 : 0);
        double s = (o.leftKey.isPressed() ? 1 : 0) - (o.rightKey.isPressed() ? 1 : 0);
        Vec3d v = mc.player.getVelocity();
        if (f == 0 && s == 0) {
            if (zeroWhenIdle) mc.player.setVelocity(0, v.y, 0);
            return false;
        }
        double len = Math.sqrt(f * f + s * s);
        f /= len;
        s /= len;
        double rad = Math.toRadians(mc.player.getYaw());
        double vx = (-Math.sin(rad) * f + Math.cos(rad) * s) * speed;
        double vz = (Math.cos(rad) * f + Math.sin(rad) * s) * speed;
        mc.player.setVelocity(vx, v.y, vz);
        return true;
    }
}
