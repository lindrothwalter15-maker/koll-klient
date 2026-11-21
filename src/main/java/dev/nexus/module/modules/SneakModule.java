package dev.nexus.module.modules;

import dev.nexus.event.TickEvent;
import dev.nexus.module.Category;
import dev.nexus.module.Module;

/** Holds the sneak key for you. */
public final class SneakModule extends Module {
    public SneakModule() {
        super("Sneak", "Always sneak", Category.MOVEMENT);
        listen(TickEvent.class, e -> tick());
    }

    private void tick() {
        if (mc.player != null && mc.currentScreen == null) mc.options.sneakKey.setPressed(true);
    }

    @Override
    protected void onDisable() {
        if (mc.options != null) mc.options.sneakKey.setPressed(false);
    }
}
