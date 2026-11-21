package dev.nexus.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/**
 * End crystal damage estimate (explosion power 6). Uses the vanilla distance/exposure formula,
 * difficulty scaling and armor/toughness reduction. Does NOT include enchantments or potion effects,
 * so it overestimates damage to well-protected players (errs on the safe side for self damage).
 */
public final class DamageUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private DamageUtil() {}

    public static float crystal(LivingEntity e, Vec3d pos) {
        double dist = Math.sqrt(e.squaredDistanceTo(pos)) / 12.0;
        if (dist > 1.0) return 0f;
        double impact = (1.0 - dist) * exposure(pos, e);
        float dmg = (float) ((impact * impact + impact) / 2.0 * 7.0 * 12.0 + 1.0);

        if (e instanceof PlayerEntity && mc.world != null) {
            switch (mc.world.getDifficulty()) {
                case PEACEFUL -> dmg = 0f;
                case EASY -> dmg = Math.min(dmg / 2f + 1f, dmg);
                case HARD -> dmg = dmg * 1.5f;
                default -> { }
            }
        }
        return armor(e, dmg);
    }

    private static float armor(LivingEntity e, float dmg) {
        float armor = (float) e.getArmor();
        // NOTE: attribute constant names changed in 1.21.3+ (ARMOR_TOUGHNESS). Verify on compile.
        float tough = (float) e.getAttributeValue(EntityAttributes.ARMOR_TOUGHNESS);
        float f = 2f + tough / 4f;
        float g = MathHelper.clamp(armor - dmg / f, armor * 0.2f, 20f);
        return Math.max(0f, dmg * (1f - g / 25f));
    }

    /** Fraction of 3 sample points (feet, middle, head) that the explosion can see. */
    private static double exposure(Vec3d src, LivingEntity e) {
        if (mc.world == null) return 1.0;
        Box b = e.getBoundingBox();
        double cx = (b.minX + b.maxX) / 2.0, cz = (b.minZ + b.maxZ) / 2.0;
        double[] ys = { b.minY + 0.1, (b.minY + b.maxY) / 2.0, b.maxY - 0.1 };
        int visible = 0;
        for (double y : ys) {
            Vec3d p = new Vec3d(cx, y, cz);
            RaycastContext ctx = new RaycastContext(src, p, RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE, e);
            if (mc.world.raycast(ctx).getType() == HitResult.Type.MISS) visible++;
        }
        return visible / 3.0;
    }
}
