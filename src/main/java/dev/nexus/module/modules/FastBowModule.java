package dev.nexus.module.modules;

import dev.nexus.event.TickEvent;
import dev.nexus.module.Category;
import dev.nexus.module.Module;

import dev.nexus.setting.NumberSetting;
import net.minecraft.item.Items;

/** Releases the bow after fewer draw ticks (arrows are weaker below 20). */
public final class FastBowModule extends Module {
    private final NumberSetting ticks = add(new NumberSetting("DrawTicks", "Release after this many ticks", 10, 3, 20, 1));

    public FastBowModule() {
        super("FastBow", "Shoot bows faster", Category.COMBAT);
        listen(TickEvent.class, e -> tick());
    }

    private void tick() {
        if (mc.player == null || mc.interactionManager == null) return;
        if (mc.player.isUsingItem() && mc.player.getActiveItem().isOf(Items.BOW)
                && mc.player.getItemUseTime() >= ticks.asInt()) {
            mc.interactionManager.stopUsingItem(mc.player);
        }
    }
}
