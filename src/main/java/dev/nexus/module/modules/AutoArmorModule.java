package dev.nexus.module.modules;

import dev.nexus.event.TickEvent;
import dev.nexus.module.Category;
import dev.nexus.module.Module;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

/** Equips armor from your inventory into any empty armor slot (for example after a piece breaks). */
public final class AutoArmorModule extends Module {
    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };
    private int timer;

    public AutoArmorModule() {
        super("AutoArmor", "Fills empty armor slots", Category.COMBAT);
        listen(TickEvent.class, e -> tick());
    }

    private void tick() {
        if (mc.player == null || mc.interactionManager == null || mc.currentScreen != null) return;
        if (timer-- > 0) return;
        var inv = mc.player.getInventory();
        for (EquipmentSlot slot : SLOTS) {
            if (!mc.player.getEquippedStack(slot).isEmpty()) continue;
            for (int i = 0; i < 36; i++) {
                ItemStack s = inv.getStack(i);
                EquippableComponent eq = s.get(DataComponentTypes.EQUIPPABLE);
                if (eq == null || eq.slot() != slot || !s.isDamageable() || s.isOf(Items.ELYTRA)) continue;
                int screenSlot = i < 9 ? 36 + i : i;
                mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, screenSlot, 0,
                        SlotActionType.QUICK_MOVE, mc.player);
                timer = 4;
                return;
            }
        }
    }
}
