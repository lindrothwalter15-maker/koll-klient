package dev.nexus.module.modules;

import dev.nexus.event.TickEvent;
import dev.nexus.module.Category;
import dev.nexus.module.Module;
import dev.nexus.setting.BoolSetting;
import dev.nexus.setting.NumberSetting;
import dev.nexus.util.RotationUtil;
import dev.nexus.util.TargetUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;

/**
 * Aim assist for bow (while drawing) and crossbow (while held). Simulates the arrow
 * flight (gravity 0.05, drag 0.99) to pick the pitch, and optionally leads moving targets.
 * Assumes a full draw (bow 3.0 b/t, crossbow 3.15 b/t).
 */
public final class BowAimModule extends Module {
    private final NumberSetting range = add(new NumberSetting("Range", "Max target distance", 50, 10, 100, 1));
    private final NumberSetting speed = add(new NumberSetting("Speed", "Max degrees per tick", 40, 5, 180, 1));
    private final BoolSetting predict = add(new BoolSetting("Predict", "Lead moving targets", true));
    private final BoolSetting players = add(new BoolSetting("Players", "Target players", true));
    private final BoolSetting mobs = add(new BoolSetting("Mobs", "Target hostile mobs", false));

    private LivingEntity lastTarget;
    private Vec3d lastPos = Vec3d.ZERO;

    public BowAimModule() {
        super("BowAim", "Bow/crossbow aim assistance", Category.COMBAT);
        listen(TickEvent.class, e -> tick());
    }

    private void tick() {
        if (mc.player == null || mc.world == null || mc.currentScreen != null) return;
        boolean bow = mc.player.isUsingItem() && mc.player.getActiveItem().isOf(Items.BOW);
        boolean xbow = mc.player.getMainHandStack().isOf(Items.CROSSBOW);
        if (!bow && !xbow) { lastTarget = null; return; }

        LivingEntity t = TargetUtil.best(range.get(), players.get(), mobs.get(), false, false);
        if (t == null) { lastTarget = null; return; }
        TargetUtil.report(t);

        float v = bow ? 3.0f : 3.15f;
        Vec3d tp = new Vec3d(t.getX(), t.getY() + t.getHeight() *  0.6, t.getZ());
        Vec3d vel = (t == lastTarget) ? tp.subtract(lastPos) : Vec3d.ZERO;
        lastTarget = t;
        lastPos = tp;

        Vec3d eye = mc.player.getEyePos();
        Vec3d aim = tp;
        if (predict.get()) {
            double ticks = Math.min(eye.distanceTo(tp) / (v * 0.85), 40.0);
            aim = tp.add(vel.multiply(ticks));
        }
        double dx = aim.x - eye.x, dy = aim.y - eye.y, dz = aim.z - eye.z;
        double h = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        RotationUtil.rotateTo(yaw, solvePitch(v, h, dy), speed.asFloat());
    }

    /** Brute-force the launch angle whose simulated trajectory is closest to (h, dy). Returns a Minecraft pitch. */
    private static float solvePitch(float v, double h, double dy) {
        float bestPitch = (float) -Math.toDegrees(Math.atan2(dy, h));
        double bestErr = Double.MAX_VALUE;
        for (float up = -45f; up <= 60f; up += 0.5f) {
            double rad = Math.toRadians(up);
            double vx = v * Math.cos(rad), vy = v * Math.sin(rad);
            double x = 0, y = 0;
            for (int t = 0; t < 120; t++) {
                double px = x, py = y;
                x += vx;
                y += vy;
                if (x >= h) {
                    double frac = (h - px) / (x - px);
                    double err = Math.abs(py + (y - py) * frac - dy);
                    if (err < bestErr) { bestErr = err; bestPitch = -up; }
                    break;
                }
                vx *= 0.99;
                vy = vy * 0.99 - 0.05;
                if (vx < 0.05) break;
            }
        }
        return bestPitch;
    }
}
