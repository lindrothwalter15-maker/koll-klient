package dev.nexus.module.modules;

import dev.nexus.event.TickEvent;
import dev.nexus.module.Category;
import dev.nexus.module.Module;
import dev.nexus.setting.BoolSetting;

/** Small working module used to verify the architecture end to end. */
public final class SprintModule extends Module {
    private final BoolSetting omni = add(new BoolSetting("Omni", "Sprint in all directions", false));

    public SprintModule() {
        super("Sprint", "Automatically sprints while moving", Category.MOVEMENT);
        listen(TickEvent.class, e -> onTick());
    }

    private void onTick() {
        if (mc.player == null) return;
        // NOTE: Input API changed across 1.21.x (PlayerInput/Vec2f). Verify on compile.
        var move = mc.player.input.getMovementInput();
        boolean moving = omni.get() ? (move.x != 0 || move.y != 0) : move.y > 0;
        if (moving && !mc.player.isSneaking() && !mc.player.horizontalCollision) mc.player.setSprinting(true);
    }
}
