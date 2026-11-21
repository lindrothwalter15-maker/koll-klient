package dev.nexus.module.modules;

import dev.nexus.event.TickEvent;
import dev.nexus.module.Category;
import dev.nexus.module.Module;

import dev.nexus.setting.NumberSetting;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

/** Quick-moves every item out of an opened chest into your inventory. */
public final class ChestStealerModule extends Module {
    private final NumberSetting delay = add(new NumberSetting("Delay", "Ticks between items", 1, 0, 10, 1));
    private int timer;

    public ChestStealerModule() {
        super("ChestStealer", "Takes everything from opened chests", Category.MISC);
        listen(TickEvent.class, e -> tick());
    }

    private void tick() {
        if (mc.player == null || mc.interactionManager == null
                || !(mc.currentScreen instanceof GenericContainerScreen screen)) { timer = 0; return; }
        if (timer-- > 0) return;
        if (mc.player.getInventory().getEmptySlot() == -1) return; // inventory full
        GenericContainerScreenHandler h = screen.getScreenHandler();
        int n = h.getRows() * 9;
        for (int i = 0; i < n; i++) {
            if (h.getSlot(i).getStack().isEmpty()) continue;
            mc.interactionManager.clickSlot(h.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
            timer = delay.asInt();
            return;
        }
    }
}
