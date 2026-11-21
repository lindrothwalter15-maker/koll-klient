package dev.nexus.module.modules;

import dev.nexus.event.TickEvent;
import dev.nexus.module.Category;
import dev.nexus.module.Module;
import dev.nexus.setting.BoolSetting;

/**
 * Sprint reset ("W-tap"): right after you hit something, sprint is cancelled for one tick so the
 * next hit gets sprint knockback again. A hit is detected from the attack cooldown resetting, so
 * it also works with your own manual clicks.
 */
public final class WTapModule extends Module {
    private final BoolSetting restart = add(new BoolSetting("Restart", "Start sprinting again next tick", true));
    private float prevCooldown = 1f;
    private boolean pendingRestart;

    public WTapModule() {
        super("WTap", "Resets sprint after each hit for extra knockback", Category.COMBAT);
        listen(TickEvent.class, e -> tick());
    }

    private void tick() {
        if (mc.player == null) return;
        float cd = mc.player.getAttackCooldownProgress(0f);
        boolean hit = prevCooldown > 0.5f && cd < 0.2f;
        prevCooldown = cd;

        if (pendingRestart) {
            pendingRestart = false;
            if (restart.get() && mc.player.input.getMovementInput().y > 0 && !mc.player.isSneaking()) {
                mc.player.setSprinting(true);
            }
        }
        if (hit && mc.player.isSprinting()) {
            mc.player.setSprinting(false);
            pendingRestart = true;
        }
    }
}
