package dev.nexus.module.modules;

import dev.nexus.event.TickEvent;
import dev.nexus.module.Category;
import dev.nexus.module.Module;

import dev.nexus.setting.NumberSetting;
import dev.nexus.util.InvUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.Set;

/** Eats food from your hotbar when hungry, then switches back. Skips bad foods and golden apples. */
public final class AutoEatModule extends Module {
    private static final Set<Item> SKIP = Set.of(Items.ROTTEN_FLESH, Items.SPIDER_EYE, Items.POISONOUS_POTATO,
            Items.PUFFERFISH, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE);

    private final NumberSetting hunger = add(new NumberSetting("Hunger", "Eat at or below this food level", 14, 1, 19, 1));
    private boolean eating;
    private int prevSlot, ticks;

    public AutoEatModule() {
        super("AutoEat", "Eats automatically when hungry", Category.MISC);
        listen(TickEvent.class, e -> tick());
    }

    private void tick() {
        if (mc.player == null || mc.world == null || CombatBotModule.EATING) return;
        if (!eating) {
            if (mc.currentScreen != null || mc.player.isUsingItem()) return;
            if (mc.player.getHungerManager().getFoodLevel() > hunger.asInt()) return;
            int slot = InvUtil.findHotbar(s -> s.contains(DataComponentTypes.FOOD) && !SKIP.contains(s.getItem()));
            if (slot == -1) return;
            prevSlot = InvUtil.selected();
            InvUtil.select(slot);
            mc.options.useKey.setPressed(true);
            eating = true;
            ticks = 0;
        } else {
            ticks++;
            if (ticks > 60 || (ticks > 4 && !mc.player.isUsingItem())) finish();
        }
    }

    private void finish() {
        mc.options.useKey.setPressed(false);
        InvUtil.select(prevSlot);
        eating = false;
    }

    @Override
    protected void onDisable() {
        if (eating && mc.player != null) finish();
        eating = false;
    }
}
