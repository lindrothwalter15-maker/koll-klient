package dev.nexus.module.modules;

import dev.nexus.event.TickEvent;
import dev.nexus.module.Category;
import dev.nexus.module.Module;

import dev.nexus.setting.BoolSetting;
import net.minecraft.util.math.Vec3d;

/** Keeps you floating on top of water (and optionally lava). */
public final class JesusModule extends Module {
    private final BoolSetting lava = add(new BoolSetting("Lava", "Also float on lava", false));

    public JesusModule() {
        super("Jesus", "Float on water", Category.MOVEMENT);
        listen(TickEvent.class, e -> tick());
    }

    private void tick() {
        if (mc.player == null || mc.player.isSneaking()) return;
        if (mc.player.isTouchingWater() || (lava.get() && mc.player.isInLava())) {
            Vec3d v = mc.player.getVelocity();
            mc.player.setVelocity(v.x, Math.max(v.y, 0.11), v.z);
        }
    }
}
