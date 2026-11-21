package dev.nexus.module.modules;

import dev.nexus.event.TickEvent;
import dev.nexus.module.Category;
import dev.nexus.module.Module;
import dev.nexus.setting.BoolSetting;
import dev.nexus.setting.ModeSetting;
import dev.nexus.setting.NumberSetting;
import dev.nexus.util.InvUtil;
import dev.nexus.util.RotationUtil;
import dev.nexus.util.TargetUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Hand;

/** Sword/axe combat: target selection, attack timing, weapon switching and shield breaking. */
public final class KillAuraModule extends Module {
    private final NumberSetting range = add(new NumberSetting("Range", "Attack reach (blocks)", 3.0, 1.0, 6.0, 0.1));
    private final ModeSetting weapon = add(new ModeSetting("Weapon", "Weapon choice (Auto = sword, axe vs shields)",
            "Auto", "Hold", "Sword", "Axe", "Auto"));
    private final ModeSetting priority = add(new ModeSetting("Priority", "Target selection", "Distance", "Distance", "Health"));
    private final NumberSetting cooldown = add(new NumberSetting("Cooldown", "Attack when charge >= this", 0.92, 0.5, 1.0, 0.01));
    private final BoolSetting critsOnly = add(new BoolSetting("CritsOnly", "Only attack while falling (critical hits)", false));
    private final BoolSetting players = add(new BoolSetting("Players", "Target players", true));
    private final BoolSetting mobs = add(new BoolSetting("Mobs", "Target hostile mobs", false));
    private final BoolSetting walls = add(new BoolSetting("ThroughWalls", "Target without line of sight", false));
    private final BoolSetting rotate = add(new BoolSetting("Rotate", "Turn the camera towards the target", true));
    private final NumberSetting rotSpeed = add(new NumberSetting("RotSpeed", "Max degrees per tick", 60, 5, 180, 1));

    private LivingEntity target;

    public KillAuraModule() {
        super("KillAura", "Sword/axe combat assistance", Category.COMBAT);
        listen(TickEvent.class, e -> tick());
    }

    @Override public String getInfo() { return target == null ? "" : target.getName().getString(); }
    @Override protected void onDisable() { target = null; }

    private void tick() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) {
            target = null;
            return;
        }
        if (CombatBotModule.EATING) return;
        target = TargetUtil.best(range.get(), players.get(), mobs.get(), walls.get(), priority.is("Health"));
        if (target == null) return;
        TargetUtil.report(target);

        if (rotate.get()) RotationUtil.rotateTowards(target, rotSpeed.asFloat());
        selectWeapon();
        if (mc.player.getAttackCooldownProgress(0.5f) < cooldown.asFloat()) return;
        if (critsOnly.get() && !canCrit()) return;

        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private boolean canCrit() {
        return !mc.player.isOnGround() && mc.player.getVelocity().y < 0
                && !mc.player.isClimbing() && !mc.player.isTouchingWater();
    }

    private void selectWeapon() {
        if (weapon.is("Hold")) return;
        boolean axe = weapon.is("Axe") || (weapon.is("Auto") && target.isBlocking());
        int slot = InvUtil.findHotbar(s -> s.isIn(axe ? ItemTags.AXES : ItemTags.SWORDS));
        if (slot == -1 && weapon.is("Auto")) slot = InvUtil.findHotbar(s -> s.isIn(axe ? ItemTags.SWORDS : ItemTags.AXES));
        if (slot != -1) InvUtil.select(slot);
    }
}
