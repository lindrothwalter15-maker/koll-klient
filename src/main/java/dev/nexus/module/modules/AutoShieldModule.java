package dev.nexus.module.modules;

import dev.nexus.event.TickEvent;
import dev.nexus.module.Category;
import dev.nexus.module.Module;

import dev.nexus.setting.NumberSetting;
import dev.nexus.util.TargetUtil;
import net.minecraft.item.Items;

/** Raises an offhand shield while an enemy is close and your attack is still recharging. */
public final class AutoShieldModule extends Module {
    private final NumberSetting range = add(new NumberSetting("Range", "Block when an enemy is this close", 4.5, 2, 8, 0.5));
    private boolean held;

    public AutoShieldModule() {
        super("AutoShield", "Blocks with your offhand shield", Category.COMBAT);
        listen(TickEvent.class, e -> tick());
    }

    private void tick() {
        if (mc.player == null || mc.world == null || mc.currentScreen != null) { release(); return; }
        boolean want = mc.player.getOffHandStack().isOf(Items.SHIELD)
                && mc.player.getAttackCooldownProgress(0.5f) < 0.9f
                && TargetUtil.best(range.get(), true, false, true, false) != null;
        if (want) { mc.options.useKey.setPressed(true); held = true; }
        else release();
    }

    private void release() {
        if (held && mc.options != null) { mc.options.useKey.setPressed(false); held = false; }
    }

    @Override protected void onDisable() { release(); }
}
