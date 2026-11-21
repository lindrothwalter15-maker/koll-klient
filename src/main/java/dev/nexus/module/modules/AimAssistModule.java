package dev.nexus.module.modules;

import dev.nexus.event.TickEvent;
import dev.nexus.module.Category;
import dev.nexus.module.Module;

import dev.nexus.setting.BoolSetting;
import dev.nexus.setting.NumberSetting;
import dev.nexus.util.RotationUtil;
import dev.nexus.util.TargetUtil;
import net.minecraft.entity.LivingEntity;

/** Smoothly pulls your aim towards a target inside your field of view while you attack. */
public final class AimAssistModule extends Module {
    private final NumberSetting range = add(new NumberSetting("Range", "Max distance", 4.5, 1, 8, 0.1));
    private final NumberSetting fov = add(new NumberSetting("FOV", "Field of view (degrees)", 90, 10, 360, 5));
    private final NumberSetting speed = add(new NumberSetting("Speed", "Max degrees per tick", 6, 1, 30, 1));
    private final BoolSetting onlyAttack = add(new BoolSetting("OnlyAttack", "Only while holding attack", true));
    private final BoolSetting players = add(new BoolSetting("Players", "Target players", true));
    private final BoolSetting mobs = add(new BoolSetting("Mobs", "Target hostile mobs", false));

    public AimAssistModule() {
        super("AimAssist", "Smooth aim assistance", Category.COMBAT);
        listen(TickEvent.class, e -> tick());
    }

    private void tick() {
        if (mc.player == null || mc.world == null || mc.currentScreen != null) return;
        if (onlyAttack.get() && !mc.options.attackKey.isPressed()) return;
        LivingEntity t = TargetUtil.best(range.get(), players.get(), mobs.get(), false, false);
        if (t == null || Math.abs(RotationUtil.yawDiff(t)) > fov.asFloat() / 2f) return;
        TargetUtil.report(t);
        RotationUtil.rotateTowards(t, speed.asFloat());
    }
}
