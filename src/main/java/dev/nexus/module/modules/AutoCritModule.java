package dev.nexus.module.modules;

import dev.nexus.event.TickEvent;
import dev.nexus.module.Category;
import dev.nexus.module.Module;

import dev.nexus.setting.NumberSetting;
import dev.nexus.util.TargetUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.math.Vec3d;

/** Jumps when your attack is ready and a target is close, so the hit lands while falling (critical). */
public final class AutoCritModule extends Module {
    private final NumberSetting range = add(new NumberSetting("Range", "Jump when a target is this close", 3.6, 2, 6, 0.1));

    public AutoCritModule() {
        super("AutoCrit", "Jumps for critical hits", Category.COMBAT);
        listen(TickEvent.class, e -> tick());
    }

    private void tick() {
        if (mc.player == null || mc.world == null || mc.currentScreen != null || !mc.player.isOnGround()) return;
        var held = mc.player.getMainHandStack();
        if (!held.isIn(ItemTags.SWORDS) && !held.isIn(ItemTags.AXES) && !held.isOf(net.minecraft.item.Items.MACE)) return;
        if (mc.player.getAttackCooldownProgress(0.5f) < 0.85f) return;
        LivingEntity t = TargetUtil.best(range.get(), true, false, true, false);
        if (t == null) return;
        Vec3d v = mc.player.getVelocity();
        mc.player.setVelocity(v.x, 0.42, v.z);
    }
}
