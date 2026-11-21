package dev.nexus.module.modules;

import dev.nexus.event.TickEvent;
import dev.nexus.module.Category;
import dev.nexus.module.Module;

import dev.nexus.setting.BoolSetting;
import dev.nexus.setting.NumberSetting;
import dev.nexus.util.MoveUtil;
import net.minecraft.util.math.Vec3d;

/** Faster ground/air movement, optionally with automatic bunny hops. */
public final class SpeedModule extends Module {
    private final NumberSetting speed = add(new NumberSetting("Speed", "Horizontal speed (blocks/tick)", 0.32, 0.1, 1.5, 0.01));
    private final BoolSetting hop = add(new BoolSetting("Hop", "Bunny hop while moving", true));

    public SpeedModule() {
        super("Speed", "Move faster", Category.MOVEMENT);
        listen(TickEvent.class, e -> tick());
    }

    private void tick() {
        if (mc.player == null || mc.currentScreen != null || mc.player.isSneaking()) return;
        if (!MoveUtil.setHorizontal(speed.get(), false)) return;
        if (hop.get() && mc.player.isOnGround()) {
            Vec3d v = mc.player.getVelocity();
            mc.player.setVelocity(v.x, 0.42, v.z);
        }
    }
}
