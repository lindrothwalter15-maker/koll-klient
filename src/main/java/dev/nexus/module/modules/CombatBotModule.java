package dev.nexus.module.modules;

import dev.nexus.NexusClient;
import dev.nexus.event.TickEvent;
import dev.nexus.module.Category;
import dev.nexus.module.Module;
import dev.nexus.setting.BoolSetting;
import dev.nexus.setting.NumberSetting;
import dev.nexus.util.InvUtil;
import dev.nexus.util.RotationUtil;
import dev.nexus.util.TargetUtil;
import net.minecraft.client.option.GameOptions;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Basic duel bot: faces the nearest target, chases it, strafes, bunny-hops, lets KillAura hit,
 * keeps a totem in the offhand and eats a golden apple when low. It does NOT path-find or avoid
 * lava/cliffs and knows nothing about specific gamemodes. Toggle it off any time (bind a key).
 */
public final class CombatBotModule extends Module {
    /** True while the bot is eating; KillAura pauses so it does not switch weapons mid-bite. */
    public static boolean EATING;

    private final NumberSetting range = add(new NumberSetting("Range", "How far it looks for targets", 30, 5, 60, 1));
    private final BoolSetting players = add(new BoolSetting("Players", "Fight players", true));
    private final BoolSetting mobs = add(new BoolSetting("Mobs", "Fight hostile mobs", false));
    private final BoolSetting jump = add(new BoolSetting("Jump", "Bunny-hop while fighting", true));
    private final NumberSetting strafe = add(new NumberSetting("StrafeTicks", "Ticks between strafe direction changes", 18, 5, 60, 1));
    private final BoolSetting autoGapple = add(new BoolSetting("AutoGapple", "Eat a golden apple when low", true));
    private final NumberSetting healAt = add(new NumberSetting("HealAt", "Eat at this health (HP)", 8, 2, 18, 1));
    private final BoolSetting helpers = add(new BoolSetting("Helpers", "Auto-enable KillAura and AutoTotem", true));

    private int strafeTimer, strafeDir = 1, eatTicks;
    private boolean wasKillAura, wasAutoTotem;
    private LivingEntity target;

    public CombatBotModule() {
        super("CombatBot", "Basic duel bot (chase, strafe, fight, heal)", Category.MISC);
        listen(TickEvent.class, e -> tick());
    }

    @Override public String getInfo() { return target == null ? "idle" : target.getName().getString(); }

    @Override
    protected void onEnable() {
        // Never start by itself from a saved profile.
        if (mc.player == null) { setEnabled(false); return; }
        if (helpers.get()) {
            Module ka = NexusClient.MODULES.get("KillAura");
            Module at = NexusClient.MODULES.get("AutoTotem");
            if (ka != null) { wasKillAura = ka.isEnabled(); ka.setEnabled(true); }
            if (at != null) { wasAutoTotem = at.isEnabled(); at.setEnabled(true); }
        }
    }

    @Override
    protected void onDisable() {
        releaseKeys();
        EATING = false;
        target = null;
        if (helpers.get()) {
            Module ka = NexusClient.MODULES.get("KillAura");
            Module at = NexusClient.MODULES.get("AutoTotem");
            if (ka != null && !wasKillAura) ka.setEnabled(false);
            if (at != null && !wasAutoTotem) at.setEnabled(false);
        }
    }

    private void tick() {
        if (mc.player == null || mc.world == null || mc.currentScreen != null) { releaseKeys(); return; }
        GameOptions o = mc.options;

        if (autoGapple.get()) handleHeal();
        if (EATING) {
            o.forwardKey.setPressed(false);
            o.sprintKey.setPressed(false);
            o.jumpKey.setPressed(false);
            return;
        }

        target = TargetUtil.best(range.get(), players.get(), mobs.get(), true, false);
        if (target == null) { releaseKeys(); return; }
        TargetUtil.report(target);
        RotationUtil.rotateTowards(target, 45f);

        double dist = Math.sqrt(TargetUtil.distSqToBox(mc.player.getEyePos(), target.getBoundingBox()));
        boolean chase = dist > 2.4;
        o.forwardKey.setPressed(chase);
        o.sprintKey.setPressed(chase);
        o.backKey.setPressed(dist < 1.2);

        if (dist < 5.0) {
            if (--strafeTimer <= 0) {
                strafeDir = -strafeDir;
                strafeTimer = strafe.asInt() + ThreadLocalRandom.current().nextInt(9);
            }
            o.leftKey.setPressed(strafeDir < 0);
            o.rightKey.setPressed(strafeDir > 0);
        } else {
            o.leftKey.setPressed(false);
            o.rightKey.setPressed(false);
        }
        o.jumpKey.setPressed(jump.get() && mc.player.isOnGround() && dist < 3.5);
    }

    private void handleHeal() {
        if (!EATING) {
            float hp = mc.player.getHealth() + mc.player.getAbsorptionAmount();
            if (hp <= healAt.asFloat() && !mc.player.isUsingItem()) {
                int slot = InvUtil.findHotbar(s -> s.isOf(Items.GOLDEN_APPLE) || s.isOf(Items.ENCHANTED_GOLDEN_APPLE));
                if (slot != -1) {
                    InvUtil.select(slot);
                    mc.options.useKey.setPressed(true);
                    EATING = true;
                    eatTicks = 0;
                }
            }
        } else {
            eatTicks++;
            if (eatTicks > 45 || (eatTicks > 4 && !mc.player.isUsingItem())) {
                mc.options.useKey.setPressed(false);
                EATING = false;
            }
        }
    }

    private void releaseKeys() {
        if (mc.options == null) return;
        GameOptions o = mc.options;
        o.forwardKey.setPressed(false);
        o.backKey.setPressed(false);
        o.leftKey.setPressed(false);
        o.rightKey.setPressed(false);
        o.jumpKey.setPressed(false);
        o.sprintKey.setPressed(false);
        o.useKey.setPressed(false);
    }
}
