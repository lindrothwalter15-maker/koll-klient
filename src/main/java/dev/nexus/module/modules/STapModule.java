package dev.nexus.module.modules;

import dev.nexus.event.TickEvent;
import dev.nexus.module.Category;
import dev.nexus.module.Module;

import dev.nexus.setting.NumberSetting;

/** After each hit, briefly taps back (S) to reset sprint and gain spacing. */
public final class STapModule extends Module {
    private final NumberSetting length = add(new NumberSetting("Ticks", "Ticks to hold back", 2, 1, 6, 1));
    private float prevCooldown = 1f;
    private int timer;
    private boolean wasForward;

    public STapModule() {
        super("STap", "Taps back after each hit", Category.COMBAT);
        listen(TickEvent.class, e -> tick());
    }

    private void tick() {
        if (mc.player == null || mc.currentScreen != null) return;
        float cd = mc.player.getAttackCooldownProgress(0f);
        boolean hit = prevCooldown > 0.5f && cd < 0.2f;
        prevCooldown = cd;

        if (timer > 0) {
            if (--timer == 0) {
                mc.options.backKey.setPressed(false);
                mc.options.forwardKey.setPressed(wasForward);
            }
            return;
        }
        if (hit) {
            wasForward = mc.options.forwardKey.isPressed();
            mc.options.forwardKey.setPressed(false);
            mc.options.backKey.setPressed(true);
            timer = length.asInt();
        }
    }

    @Override
    protected void onDisable() {
        if (timer > 0 && mc.options != null) {
            mc.options.backKey.setPressed(false);
            mc.options.forwardKey.setPressed(wasForward);
        }
        timer = 0;
    }
}
