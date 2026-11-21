package dev.nexus.module.modules;

import dev.nexus.event.TickEvent;
import dev.nexus.module.Category;
import dev.nexus.module.Module;

import dev.nexus.setting.NumberSetting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import java.util.concurrent.ThreadLocalRandom;

/** Jumps, swings and turns slightly every so often so you are not kicked for idling. */
public final class AntiAfkModule extends Module {
    private final NumberSetting interval = add(new NumberSetting("Seconds", "Seconds between actions", 20, 5, 120, 1));
    private int timer;

    public AntiAfkModule() {
        super("AntiAFK", "Avoid idle kicks", Category.MISC);
        listen(TickEvent.class, e -> tick());
    }

    private void tick() {
        if (mc.player == null || mc.currentScreen != null) return;
        if (++timer < interval.asInt() * 20) return;
        timer = 0;
        if (mc.player.isOnGround()) {
            Vec3d v = mc.player.getVelocity();
            mc.player.setVelocity(v.x, 0.42, v.z);
        }
        mc.player.swingHand(Hand.MAIN_HAND);
        mc.player.setYaw(mc.player.getYaw() + (ThreadLocalRandom.current().nextBoolean() ? 12f : -12f));
    }
}
