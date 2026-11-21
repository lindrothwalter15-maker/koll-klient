package dev.nexus.module.modules;

import dev.nexus.event.TickEvent;
import dev.nexus.module.Category;
import dev.nexus.module.Module;

import dev.nexus.setting.NumberSetting;
import net.minecraft.util.math.Vec3d;

/** Pushes you back up when you fall below a height (for bedwars/skywars style voids). */
public final class AntiVoidModule extends Module {
    private final NumberSetting level = add(new NumberSetting("Level", "Y level that counts as the void", 0, -64, 200, 1));

    public AntiVoidModule() {
        super("AntiVoid", "Saves you from the void", Category.COMBAT);
        listen(TickEvent.class, e -> tick());
    }

    private void tick() {
        if (mc.player == null || mc.player.isOnGround()) return;
        if (mc.player.getY() < level.get() && mc.player.getVelocity().y < 0) {
            Vec3d v = mc.player.getVelocity();
            mc.player.setVelocity(v.x, 0.7, v.z);
        }
    }
}
