package dev.nexus.module.modules;

import dev.nexus.event.TickEvent;
import dev.nexus.module.Category;
import dev.nexus.module.Module;
import dev.nexus.setting.NumberSetting;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

/** Keeps a totem of undying in the offhand. */
public final class AutoTotemModule extends Module {
    private final NumberSetting delay = add(new NumberSetting("Delay", "Ticks between swaps", 1, 0, 10, 1));
    private int timer;

    public AutoTotemModule() {
        super("AutoTotem", "Keeps a totem in your offhand", Category.COMBAT);
        listen(TickEvent.class, e -> tick());
    }

    @Override
    public String getInfo() {
        if (mc.player == null) return "";
        int c = 0;
        var inv = mc.player.getInventory();
        for (int i = 0; i < inv.size(); i++) if (inv.getStack(i).isOf(Items.TOTEM_OF_UNDYING)) c += inv.getStack(i).getCount();
        return Integer.toString(c);
    }

    private void tick() {
        if (mc.player == null || mc.interactionManager == null || mc.currentScreen != null) return;
        if (timer > 0) { timer--; return; }
        if (mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) return;
        var inv = mc.player.getInventory();
        for (int i = 0; i < 36; i++) {
            if (!inv.getStack(i).isOf(Items.TOTEM_OF_UNDYING)) continue;
            int screenSlot = i < 9 ? 36 + i : i; // hotbar 0-8 -> 36-44, main 9-35 -> 9-35
            mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, screenSlot, 40,
                    SlotActionType.SWAP, mc.player);
            timer = delay.asInt();
            return;
        }
    }
}
