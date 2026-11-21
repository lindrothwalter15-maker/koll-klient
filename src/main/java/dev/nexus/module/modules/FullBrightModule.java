package dev.nexus.module.modules;

import dev.nexus.event.TickEvent;
import dev.nexus.module.Category;
import dev.nexus.module.Module;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

/** Permanent night vision (client side). */
public final class FullBrightModule extends Module {
    private int timer;

    public FullBrightModule() {
        super("FullBright", "See clearly in the dark", Category.RENDER);
        listen(TickEvent.class, e -> tick());
    }

    private void tick() {
        if (mc.player == null) return;
        if (timer++ % 40 == 0) {
            mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 400, 0, false, false));
        }
    }

    @Override
    protected void onDisable() {
        if (mc.player != null) mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        timer = 0;
    }
}
