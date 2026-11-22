package dev.nexus.module.modules;

import dev.nexus.event.TickEvent;
import dev.nexus.module.Category;
import dev.nexus.module.Module;
import dev.nexus.setting.BoolSetting;
import net.minecraft.text.Text;

/**
 * Invulnerability for singleplayer / LAN worlds. It sets the invulnerable flag on your player inside the
 * integrated server (the server decides damage), so it does nothing on other people's servers.
 */
public final class GodModeModule extends Module {
    private final BoolSetting hunger = add(new BoolSetting("Hunger", "Keep food level full", true));
    private int timer;

    public GodModeModule() {
        super("GodMode", "Invulnerable in singleplayer", Category.MISC);
        listen(TickEvent.class, e -> tick());
    }

    @Override
    protected void onEnable() {
        // Never start by itself from a saved profile.
        if (mc.player == null) { setEnabled(false); return; }
        if (mc.getServer() == null) {
            mc.player.sendMessage(Text.literal("GodMode only works in singleplayer or your own world."), false);
            setEnabled(false);
            return;
        }
        timer = 0;
        apply(true);
    }

    @Override
    protected void onDisable() {
        if (mc.player != null && mc.getServer() != null) apply(false);
    }

    private void tick() {
        if (mc.player == null || mc.getServer() == null) return;
        if (timer++ % 10 == 0) apply(true); // re-applies after respawn too
    }

    private void apply(boolean on) {
        var server = mc.getServer();
        if (server == null || mc.player == null) return;
        var uuid = mc.player.getUuid();
        boolean food = hunger.get();
        server.execute(() -> {
            var sp = server.getPlayerManager().getPlayer(uuid);
            if (sp == null) return;
            sp.setInvulnerable(on);
            if (on && food) sp.getHungerManager().setFoodLevel(20);
        });
    }
}
