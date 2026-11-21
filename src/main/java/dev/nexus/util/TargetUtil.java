package dev.nexus.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/** Target selection. One pass over loaded entities, no per-entity allocation. */
public final class TargetUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static LivingEntity reported;
    private static long reportedAt;

    private TargetUtil() {}

    /** Combat modules report their current target so the Target HUD can show it. */
    public static void report(LivingEntity e) { reported = e; reportedAt = System.currentTimeMillis(); }

    public static LivingEntity reported() {
        LivingEntity r = reported;
        return (r != null && r.isAlive() && System.currentTimeMillis() - reportedAt < 600) ? r : null;
    }

    /** Best target within reach range (measured to the hitbox). Priority: lowest distance or lowest health. */
    public static LivingEntity best(double range, boolean players, boolean mobs, boolean walls, boolean byHealth) {
        if (mc.world == null || mc.player == null) return null;
        Vec3d eye = mc.player.getEyePos();
        double rsq = range * range;
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity le) || le == mc.player || !le.isAlive()) continue;
            if (le instanceof PlayerEntity p) {
                if (!players || p.isCreative() || p.isSpectator()) continue;
            } else if (!(mobs && le instanceof HostileEntity)) {
                continue;
            }
            double d = distSqToBox(eye, le.getBoundingBox());
            if (d > rsq) continue;
            if (!walls && !mc.player.canSee(le)) continue;
            double score = byHealth ? le.getHealth() + le.getAbsorptionAmount() : d;
            if (score < bestScore) { bestScore = score; best = le; }
        }
        return best;
    }

    public static double distSqToBox(Vec3d p, Box b) {
        double dx = Math.max(Math.max(b.minX - p.x, 0.0), p.x - b.maxX);
        double dy = Math.max(Math.max(b.minY - p.y, 0.0), p.y - b.maxY);
        double dz = Math.max(Math.max(b.minZ - p.z, 0.0), p.z - b.maxZ);
        return dx * dx + dy * dy + dz * dz;
    }
}
