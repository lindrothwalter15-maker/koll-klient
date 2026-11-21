package dev.nexus.module.modules;

import dev.nexus.event.TickEvent;
import dev.nexus.module.Category;
import dev.nexus.module.Module;
import dev.nexus.setting.BoolSetting;
import dev.nexus.setting.NumberSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;

/** Attacks automatically when your crosshair is on a valid target. Does not rotate the camera. */
public final class TriggerBotModule extends Module {
    private final NumberSetting cooldown = add(new NumberSetting("Cooldown", "Attack when charge >= this", 0.92, 0.5, 1.0, 0.01));
    private final BoolSetting critsOnly = add(new BoolSetting("CritsOnly", "Only attack while falling (critical hits)", false));
    private final BoolSetting weaponOnly = add(new BoolSetting("WeaponOnly", "Only when holding a sword or axe", true));
    private final BoolSetting players = add(new BoolSetting("Players", "Target players", true));
    private final BoolSetting mobs = add(new BoolSetting("Mobs", "Target hostile mobs", false));

    public TriggerBotModule() {
        super("TriggerBot", "Attacks when your crosshair is on a target", Category.COMBAT);
        listen(TickEvent.class, e -> tick());
    }

    private void tick() {
        if (mc.player == null || mc.interactionManager == null || mc.currentScreen != null) return;
        if (!(mc.crosshairTarget instanceof EntityHitResult hit)) return;
        Entity e = hit.getEntity();
        if (!(e instanceof LivingEntity le) || !le.isAlive()) return;
        if (le instanceof PlayerEntity p) {
            if (!players.get() || p.isCreative() || p.isSpectator()) return;
        } else if (!(mobs.get() && le instanceof HostileEntity)) {
            return;
        }
        if (weaponOnly.get()) {
            ItemStack s = mc.player.getMainHandStack();
            if (!s.isIn(ItemTags.SWORDS) && !s.isIn(ItemTags.AXES)) return;
        }
        if (mc.player.getAttackCooldownProgress(0.5f) < cooldown.asFloat()) return;
        if (critsOnly.get() && !(!mc.player.isOnGround() && mc.player.getVelocity().y < 0
                && !mc.player.isClimbing() && !mc.player.isTouchingWater())) return;

        mc.interactionManager.attackEntity(mc.player, le);
        mc.player.swingHand(Hand.MAIN_HAND);
    }
}
