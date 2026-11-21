package dev.nexus.module.modules;

import dev.nexus.event.TickEvent;
import dev.nexus.module.Category;
import dev.nexus.module.Module;

import dev.nexus.setting.BoolSetting;
import dev.nexus.setting.NumberSetting;
import dev.nexus.util.InvUtil;
import dev.nexus.util.TargetUtil;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/** Throws XP bottles at your feet to repair armor when it gets low. */
public final class AutoMendModule extends Module {
    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };
    private final NumberSetting percent = add(new NumberSetting("Durability", "Repair below this percent", 60, 5, 95, 5));
    private final BoolSetting safe = add(new BoolSetting("OnlyWhenSafe", "Not while an enemy is close", true));
    private int timer;

    public AutoMendModule() {
        super("AutoMend", "Repairs armor with XP bottles", Category.COMBAT);
        listen(TickEvent.class, e -> tick());
    }

    private void tick() {
        if (mc.player == null || mc.world == null || mc.currentScreen != null) return;
        if (timer > 0) { timer--; return; }
        boolean needs = false;
        for (EquipmentSlot slot : SLOTS) {
            ItemStack s = mc.player.getEquippedStack(slot);
            if (s.isDamageable() && s.getMaxDamage() > 0
                    && 100f * (s.getMaxDamage() - s.getDamage()) / s.getMaxDamage() < percent.asFloat()) needs = true;
        }
        if (!needs) return;
        if (safe.get() && TargetUtil.best(8, true, false, true, false) != null) return;
        int slot = InvUtil.findHotbar(s -> s.isOf(Items.EXPERIENCE_BOTTLE));
        if (slot == -1) return;
        InvUtil.useDown(slot);
        timer = 6;
    }
}
