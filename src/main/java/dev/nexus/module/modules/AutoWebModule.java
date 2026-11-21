package dev.nexus.module.modules;

import dev.nexus.event.TickEvent;
import dev.nexus.module.Category;
import dev.nexus.module.Module;

import dev.nexus.setting.NumberSetting;
import dev.nexus.util.InvUtil;
import dev.nexus.util.TargetUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/** Places a cobweb on the block the target stands in. */
public final class AutoWebModule extends Module {
    private final NumberSetting range = add(new NumberSetting("Range", "Placement reach", 4.5, 1, 6, 0.1));
    private final NumberSetting delay = add(new NumberSetting("Delay", "Ticks between webs", 12, 1, 60, 1));
    private int timer;

    public AutoWebModule() {
        super("AutoWeb", "Webs the target's feet", Category.COMBAT);
        listen(TickEvent.class, e -> tick());
    }

    private void tick() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) return;
        if (timer > 0) { timer--; return; }
        LivingEntity t = TargetUtil.best(range.get() + 1, true, false, true, false);
        if (t == null) return;
        BlockPos feet = t.getBlockPos();
        BlockPos ground = feet.down();
        if (!mc.world.getBlockState(feet).isAir() || !mc.world.getBlockState(ground).isSolidBlock(mc.world, ground)) return;
        if (mc.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(feet)) > range.get() * range.get()) return;
        int slot = InvUtil.findHotbar(s -> s.isOf(Items.COBWEB));
        if (slot == -1) return;
        InvUtil.select(slot);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
                new BlockHitResult(new Vec3d(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5), Direction.UP, ground, false));
        mc.player.swingHand(Hand.MAIN_HAND);
        timer = delay.asInt();
    }
}
