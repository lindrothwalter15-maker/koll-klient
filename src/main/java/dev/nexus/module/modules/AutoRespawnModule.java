package dev.nexus.module.modules;

import dev.nexus.event.TickEvent;
import dev.nexus.module.Category;
import dev.nexus.module.Module;

import dev.nexus.setting.NumberSetting;

/** Respawns automatically after you die. */
public final class AutoRespawnModule extends Module {
    private final NumberSetting delay = add(new NumberSetting("Delay", "Ticks to wait before respawning", 10, 0, 100, 1));
    private int timer;

    public AutoRespawnModule() {
        super("AutoRespawn", "Respawn automatically", Category.MISC);
        listen(TickEvent.class, e -> tick());
    }

    private void tick() {
        if (mc.player == null || !mc.player.isDead()) { timer = 0; return; }
        if (++timer >= delay.asInt()) {
            mc.player.requestRespawn();
            timer = 0;
        }
    }
}
