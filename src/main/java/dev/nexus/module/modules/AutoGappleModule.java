package dev.nexus.module.modules;

import dev.nexus.event.TickEvent;
import dev.nexus.module.Category;
import dev.nexus.module.Module;

import dev.nexus.setting.NumberSetting;
import dev.nexus.util.InvUtil;
import net.minecraft.item.Items;

/** Eats a golden apple from your hotbar when your health is low, then switches back. */
public final class AutoGappleModule extends Module {
    private final NumberSetting below = add(new NumberSetting("Health", "Eat at or below this HP", 8, 2, 18, 1));
    private boolean eating;
    private int prevSlot, ticks;

    public AutoGappleModule() {
        super("AutoGapple", "Eats golden apples when low", Category.COMBAT);
        listen(TickEvent.class, e -> tick());
    }

    private void tick() {
        if (mc.player == null || mc.world == null) return;
        if (!eating) {
            if (mc.currentScreen != null || mc.player.isUsingItem()) return;
            if (mc.player.getHealth() + mc.player.getAbsorptionAmount() > below.asFloat()) return;
            int slot = InvUtil.findHotbar(s -> s.isOf(Items.GOLDEN_APPLE) || s.isOf(Items.ENCHANTED_GOLDEN_APPLE));
            if (slot == -1) return;
            prevSlot = InvUtil.selected();
            InvUtil.select(slot);
            mc.options.useKey.setPressed(true);
            eating = true;
            CombatBotModule.EATING = true;
            ticks = 0;
        } else {
            ticks++;
            if (ticks > 45 || (ticks > 4 && !mc.player.isUsingItem())) finish();
        }
    }

    private void finish() {
        mc.options.useKey.setPressed(false);
        InvUtil.select(prevSlot);
        eating = false;
        CombatBotModule.EATING = false;
    }

    @Override
    protected void onDisable() {
        if (eating && mc.player != null) finish();
        eating = false;
    }
}
