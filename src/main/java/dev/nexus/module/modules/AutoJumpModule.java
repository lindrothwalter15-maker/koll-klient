package dev.nexus.module.modules;

import dev.nexus.event.TickEvent;
import dev.nexus.module.Category;
import dev.nexus.module.Module;

import dev.nexus.util.MoveUtil;
import net.minecraft.util.math.Vec3d;

/** Jumps automatically when you walk into a block. */
public final class AutoJumpModule extends Module {
    public AutoJumpModule() {
        super("AutoJump", "Jump over obstacles automatically", Category.MOVEMENT);
        listen(TickEvent.class, e -> tick());
    }

    private void tick() {
        if (mc.player == null || mc.currentScreen != null) return;
        if (mc.player.isOnGround() && mc.player.horizontalCollision && MoveUtil.moving() && !mc.player.isSneaking()) {
            Vec3d v = mc.player.getVelocity();
            mc.player.setVelocity(v.x, 0.42, v.z);
        }
    }
}
