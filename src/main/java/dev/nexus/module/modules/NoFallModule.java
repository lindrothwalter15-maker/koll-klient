package dev.nexus.module.modules;

import dev.nexus.event.TickEvent;
import dev.nexus.module.Category;
import dev.nexus.module.Module;

import dev.nexus.setting.NumberSetting;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

/** Tells the server you are on the ground once you have fallen far enough, so you take no fall damage. */
public final class NoFallModule extends Module {
    private final NumberSetting distance = add(new NumberSetting("Distance", "Fall distance before it kicks in", 2.5, 1.0, 10.0, 0.5));
    private double fallStartY;

    public NoFallModule() {
        super("NoFall", "Prevents fall damage", Category.MOVEMENT);
        listen(TickEvent.class, e -> tick());
    }

    private void tick() {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        double y = mc.player.getY();
        if (mc.player.isOnGround() || mc.player.isTouchingWater() || mc.player.isClimbing() || mc.player.getVelocity().y > 0) {
            fallStartY = y;
            return;
        }
        if (fallStartY - y > distance.get()) {
            // NOTE: packet constructor gained a horizontalCollision flag in 1.21.2. Verify on compile.
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true, mc.player.horizontalCollision));
        }
    }
}
