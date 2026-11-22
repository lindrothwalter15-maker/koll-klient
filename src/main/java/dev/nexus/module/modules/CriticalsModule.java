package dev.nexus.module.modules;

import dev.nexus.event.AttackEvent;
import dev.nexus.module.Category;
import dev.nexus.module.Module;
import dev.nexus.setting.BoolSetting;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

/**
 * Critical hits without jumping. Right before each attack it sends two movement packets (a tiny hop and
 * back, both "in the air") so the server sees you falling. Vanilla still requires that you are not
 * sprinting when you hit. Works for your own clicks and for KillAura/TriggerBot.
 */
public final class CriticalsModule extends Module {
    private final BoolSetting onlyGround = add(new BoolSetting("OnlyOnGround", "Only when you stand on the ground", true));

    public CriticalsModule() {
        super("Criticals", "Critical hits without jumping", Category.COMBAT);
        listen(AttackEvent.class, e -> onAttack(e));
    }

    private void onAttack(AttackEvent e) {
        if (mc.player == null || mc.getNetworkHandler() == null || !(e.target instanceof LivingEntity)) return;
        if (mc.player.isTouchingWater() || mc.player.isInLava() || mc.player.isClimbing() || mc.player.hasVehicle()) return;
        if (onlyGround.get() && !mc.player.isOnGround()) return;
        double x = mc.player.getX(), y = mc.player.getY(), z = mc.player.getZ();
        boolean hc = mc.player.horizontalCollision;
        // NOTE: PositionAndOnGround gained a horizontalCollision flag in 1.21.2. Verify on compile.
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 0.0625, z, false, hc));
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, false, hc));
    }
}
