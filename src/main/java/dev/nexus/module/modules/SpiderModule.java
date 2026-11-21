package dev.nexus.module.modules;

import dev.nexus.event.TickEvent;
import dev.nexus.module.Category;
import dev.nexus.module.Module;

import net.minecraft.util.math.Vec3d;

/** Climb walls by pushing against them. */
public final class SpiderModule extends Module {
    public SpiderModule() {
        super("Spider", "Climb walls", Category.MOVEMENT);
        listen(TickEvent.class, e -> tick());
    }

    private void tick() {
        if (mc.player == null || mc.player.isSneaking() || !mc.player.horizontalCollision) return;
        Vec3d v = mc.player.getVelocity();
        mc.player.setVelocity(v.x, 0.2, v.z);
    }
}
