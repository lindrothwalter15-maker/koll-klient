package dev.nexus.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;

import java.util.function.Predicate;

public final class InvUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private InvUtil() {}

    /** Hotbar index (0-8) of the first stack matching the predicate, or -1. */
    public static int findHotbar(Predicate<ItemStack> p) {
        var inv = mc.player.getInventory();
        for (int i = 0; i < 9; i++) if (p.test(inv.getStack(i))) return i;
        return -1;
    }

    public static int selected() { return mc.player.getInventory().getSelectedSlot(); }

    // NOTE: selected-slot accessors changed in 1.21.5+. Verify on compile.
    public static void select(int slot) {
        if (slot < 0 || slot > 8) return;
        if (mc.player.getInventory().getSelectedSlot() != slot) mc.player.getInventory().setSelectedSlot(slot);
    }
}
