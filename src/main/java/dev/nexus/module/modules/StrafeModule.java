package dev.nexus.module.modules;

import dev.nexus.event.TickEvent;
import dev.nexus.module.Category;
import dev.nexus.module.Module;

import dev.nexus.NexusClient;
import dev.nexus.setting.NumberSetting;
import dev.nexus.util.TargetUtil;
import net.minecraft.entity.LivingEntity;

import java.util.concurrent.ThreadLocalRandom;

/** Circle-strafes around your current combat target by alternating left and right. */
public final class StrafeModule extends Module {
    private final NumberSetting ticks = add(new NumberSetting("Ticks", "Ticks between direction changes", 16, 4, 60, 1));
    private final NumberSetting range = add(new NumberSetting("Range", "Strafe within this distance", 5, 2, 8, 0.5));
    private int timer, dir = 1;
    private boolean active;

    public StrafeModule() {
        super("Strafe", "Circle strafe around the target", Category.COMBAT);
        listen(TickEvent.class, e -> tick());
    }

    private void tick() {
        Module bot = NexusClient.MODULES.get("CombatBot");
        LivingEntity t = TargetUtil.reported();
        if (mc.player == null || mc.currentScreen != null || t == null || (bot != null && bot.isEnabled())
                || TargetUtil.distSqToBox(mc.player.getEyePos(), t.getBoundingBox()) > range.get() * range.get()) {
            release();
            return;
        }
        if (--timer <= 0) {
            dir = -dir;
            timer = ticks.asInt() + ThreadLocalRandom.current().nextInt(6);
        }
        mc.options.leftKey.setPressed(dir < 0);
        mc.options.rightKey.setPressed(dir > 0);
        active = true;
    }

    private void release() {
        if (active && mc.options != null) {
            mc.options.leftKey.setPressed(false);
            mc.options.rightKey.setPressed(false);
            active = false;
        }
    }

    @Override protected void onDisable() { release(); }
}
