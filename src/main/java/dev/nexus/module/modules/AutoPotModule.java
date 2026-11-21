package dev.nexus.module.modules;

import dev.nexus.event.TickEvent;
import dev.nexus.module.Category;
import dev.nexus.module.Module;

import dev.nexus.setting.NumberSetting;
import dev.nexus.util.InvUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potions;

/** Throws a splash potion of healing at your feet when your health is low. */
public final class AutoPotModule extends Module {
    private final NumberSetting below = add(new NumberSetting("Health", "Throw at or below this HP", 10, 2, 20, 1));
    private final NumberSetting delay = add(new NumberSetting("Delay", "Ticks between throws", 8, 1, 40, 1));
    private int timer;

    public AutoPotModule() {
        super("AutoPot", "Throws healing potions when low", Category.COMBAT);
        listen(TickEvent.class, e -> tick());
    }

    private static boolean isHealing(ItemStack s) {
        if (!s.isOf(Items.SPLASH_POTION)) return false;
        PotionContentsComponent pc = s.get(DataComponentTypes.POTION_CONTENTS);
        // NOTE: PotionContentsComponent.matches(...) is the 1.21 API. Verify on compile.
        return pc != null && (pc.matches(Potions.HEALING) || pc.matches(Potions.STRONG_HEALING));
    }

    private void tick() {
        if (mc.player == null || mc.currentScreen != null) return;
        if (timer > 0) { timer--; return; }
        if (mc.player.getHealth() + mc.player.getAbsorptionAmount() > below.asFloat()) return;
        int slot = InvUtil.findHotbar(AutoPotModule::isHealing);
        if (slot == -1) return;
        InvUtil.useDown(slot);
        timer = delay.asInt();
    }
}
