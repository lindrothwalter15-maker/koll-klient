package dev.nexus.module.modules;

import dev.nexus.event.TickEvent;
import dev.nexus.module.Category;
import dev.nexus.module.Module;

import dev.nexus.setting.NumberSetting;
import dev.nexus.util.InvUtil;
import net.minecraft.item.BlockItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/** Places blocks under your feet while you walk (bridging). */
public final class ScaffoldModule extends Module {
    private final NumberSetting delay = add(new NumberSetting("Delay", "Ticks between placements", 1, 0, 10, 1));
    private int timer;

    public ScaffoldModule() {
        super("Scaffold", "Places blocks under you", Category.MOVEMENT);
        listen(TickEvent.class, e -> tick());
    }

    private void tick() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) return;
        if (timer > 0) { timer--; return; }
        BlockPos below = BlockPos.ofFloored(mc.player.getX(), mc.player.getY() - 1.0, mc.player.getZ());
        if (!mc.world.getBlockState(below).isReplaceable()) return;

        for (Direction d : Direction.values()) {
            if (d == Direction.UP) continue;
            BlockPos n = below.offset(d);
            if (!mc.world.getBlockState(n).isSolidBlock(mc.world, n)) continue;

            if (!(mc.player.getMainHandStack().getItem() instanceof BlockItem)) {
                int slot = InvUtil.findHotbar(s -> s.getItem() instanceof BlockItem);
                if (slot == -1) return;
                InvUtil.select(slot);
            }
            Direction face = d.getOpposite();
            Vec3d hitPos = Vec3d.ofCenter(n).add(face.getOffsetX() * 0.5, face.getOffsetY() * 0.5, face.getOffsetZ() * 0.5);
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(hitPos, face, n, false));
            mc.player.swingHand(Hand.MAIN_HAND);
            timer = delay.asInt();
            return;
        }
    }
}
