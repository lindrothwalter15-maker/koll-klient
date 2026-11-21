package dev.nexus.module.modules;

import dev.nexus.event.TickEvent;
import dev.nexus.module.Category;
import dev.nexus.module.Module;

import net.minecraft.entity.player.PlayerEntity;

/** Outlines all other players so you can see them through walls. */
public final class GlowEspModule extends Module {
    public GlowEspModule() {
        super("GlowESP", "Outline players through walls", Category.RENDER);
        listen(TickEvent.class, e -> tick());
    }

    private void tick() {
        if (mc.world == null) return;
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p != mc.player && !p.isGlowing()) p.setGlowing(true);
        }
    }

    @Override
    protected void onDisable() {
        if (mc.world == null) return;
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p != mc.player) p.setGlowing(false);
        }
    }
}
