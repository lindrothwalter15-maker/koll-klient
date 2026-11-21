package dev.nexus.module.modules;

import dev.nexus.event.TickEvent;
import dev.nexus.module.Category;
import dev.nexus.module.Module;

import dev.nexus.util.InvUtil;
import net.minecraft.block.BlockState;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

/** While you mine, switches to the hotbar item that breaks the targeted block fastest. */
public final class AutoToolModule extends Module {
    public AutoToolModule() {
        super("AutoTool", "Picks the best tool while mining", Category.MISC);
        listen(TickEvent.class, e -> tick());
    }

    private void tick() {
        if (mc.player == null || mc.world == null || mc.currentScreen != null) return;
        if (!mc.options.attackKey.isPressed()) return;
        if (!(mc.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        BlockState st = mc.world.getBlockState(hit.getBlockPos());
        if (st.isAir()) return;
        var inv = mc.player.getInventory();
        int best = InvUtil.selected();
        float bestSpeed = inv.getStack(best).getMiningSpeedMultiplier(st);
        for (int i = 0; i < 9; i++) {
            float sp = inv.getStack(i).getMiningSpeedMultiplier(st);
            if (sp > bestSpeed) { bestSpeed = sp; best = i; }
        }
        InvUtil.select(best);
    }
}
