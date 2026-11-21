package dev.nexus.module.modules;

import dev.nexus.event.TickEvent;
import dev.nexus.module.Category;
import dev.nexus.module.Module;

import dev.nexus.setting.NumberSetting;
import dev.nexus.util.MoveUtil;
import net.minecraft.util.math.Vec3d;

/** Creative-style flight: look + WASD to move, Space up, Shift down. */
public final class FlightModule extends Module {
    private final NumberSetting speed = add(new NumberSetting("Speed", "Horizontal speed (blocks/tick)", 0.4, 0.05, 3.0, 0.05));
    private final NumberSetting vertical = add(new NumberSetting("Vertical", "Up/down speed (blocks/tick)", 0.3, 0.05, 2.0, 0.05));

    public FlightModule() {
        super("Flight", "Fly in survival", Category.MOVEMENT);
        listen(TickEvent.class, e -> tick());
    }

    private void tick() {
        if (mc.player == null || mc.currentScreen != null) return;
        MoveUtil.setHorizontal(speed.get(), true);
        double vy = mc.options.jumpKey.isPressed() ? vertical.get() : mc.options.sneakKey.isPressed() ? -vertical.get() : 0.0;
        Vec3d v = mc.player.getVelocity();
        mc.player.setVelocity(v.x, vy, v.z);
    }
}
