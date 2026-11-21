package dev.nexus.module.modules;

import dev.nexus.event.TickEvent;
import dev.nexus.module.Category;
import dev.nexus.module.Module;

import dev.nexus.setting.BoolSetting;
import dev.nexus.setting.NumberSetting;
import dev.nexus.util.InvUtil;
import dev.nexus.util.RotationUtil;
import dev.nexus.util.TargetUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

/** Mace PvP: launches you upwards with a wind charge and smashes the target on the way down. */
public final class MaceAuraModule extends Module {
    private final NumberSetting range = add(new NumberSetting("Range", "Attack reach", 3.2, 1, 6, 0.1));
    private final NumberSetting minFall = add(new NumberSetting("MinFall", "Minimum fall (blocks) before smashing", 2.0, 0.5, 20, 0.5));
    private final BoolSetting wind = add(new BoolSetting("WindCharge", "Launch yourself with a wind charge", true));
    private final NumberSetting launchRange = add(new NumberSetting("LaunchRange", "Launch when target is this close", 6, 3, 12, 0.5));
    private final BoolSetting rotate = add(new BoolSetting("Rotate", "Face the target", true));

    private double fallStartY;
    private int launchTimer;
    private LivingEntity target;

    public MaceAuraModule() {
        super("MaceAura", "Mace smash attacks with wind charge launch", Category.COMBAT);
        listen(TickEvent.class, e -> tick());
    }

    @Override public String getInfo() { return target == null ? "" : target.getName().getString(); }
    @Override protected void onDisable() { target = null; }

    private void tick() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) { target = null; return; }
        double y = mc.player.getY();
        if (mc.player.isOnGround() || mc.player.getVelocity().y > 0) fallStartY = y;
        if (launchTimer > 0) launchTimer--;

        target = TargetUtil.best(Math.max(range.get(), launchRange.get()), true, false, true, false);
        if (target == null) return;
        TargetUtil.report(target);
        int mace = InvUtil.findHotbar(s -> s.isOf(Items.MACE));
        if (mace == -1) return;

        if (wind.get() && launchTimer == 0 && mc.player.isOnGround()) {
            int ws = InvUtil.findHotbar(s -> s.isOf(Items.WIND_CHARGE));
            if (ws != -1) {
                InvUtil.useDown(ws);
                launchTimer = 40;
                return;
            }
        }

        double fallen = fallStartY - y;
        if (!mc.player.isOnGround() && mc.player.getVelocity().y < 0 && fallen >= minFall.get()
                && TargetUtil.distSqToBox(mc.player.getEyePos(), target.getBoundingBox()) <= range.get() * range.get()
                && mc.player.getAttackCooldownProgress(0.5f) >= 0.9f) {
            InvUtil.select(mace);
            if (rotate.get()) RotationUtil.rotateTowards(target, 90f);
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }
}
