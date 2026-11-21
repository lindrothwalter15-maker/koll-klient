package dev.nexus.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class RotationUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private RotationUtil() {}

    /** Turns the camera towards the entity's hitbox center, at most maxStep degrees per call. */
    public static void rotateTowards(Entity target, float maxStep) {
        Vec3d eye = mc.player.getEyePos();
        Vec3d to = target.getBoundingBox().getCenter();
        double dx = to.x - eye.x, dy = to.y - eye.y, dz = to.z - eye.z;
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        rotateTo(yaw, pitch, maxStep);
    }

    public static void rotateTo(float yaw, float pitch, float maxStep) {
        float cy = mc.player.getYaw(), cp = mc.player.getPitch();
        float dyaw = MathHelper.wrapDegrees(yaw - cy);
        float dpitch = pitch - cp;
        mc.player.setYaw(cy + MathHelper.clamp(dyaw, -maxStep, maxStep));
        mc.player.setPitch(MathHelper.clamp(cp + MathHelper.clamp(dpitch, -maxStep, maxStep), -90f, 90f));
    }

    /** Signed yaw difference (degrees) between where you look and the entity's hitbox center. */
    public static float yawDiff(Entity target) {
        Vec3d eye = mc.player.getEyePos();
        Vec3d to = target.getBoundingBox().getCenter();
        float yaw = (float) (Math.toDegrees(Math.atan2(to.z - eye.z, to.x - eye.x)) - 90.0);
        return MathHelper.wrapDegrees(yaw - mc.player.getYaw());
    }
}
