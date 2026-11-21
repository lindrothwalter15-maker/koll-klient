package dev.nexus.module.modules;

import dev.nexus.event.TickEvent;
import dev.nexus.module.Category;
import dev.nexus.module.Module;

import dev.nexus.setting.NumberSetting;
import dev.nexus.util.DamageUtil;
import dev.nexus.util.InvUtil;
import dev.nexus.util.TargetUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.TntMinecartEntity;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * Cart PvP: places a rail near the target, a TNT minecart on it, then lights it with flint and steel or a
 * fire charge. Damage is estimated with explosion power 4.5 and checked against your self-damage limit.
 */
public final class CartAuraModule extends Module {
    private final NumberSetting targetRange = add(new NumberSetting("TargetRange", "Max target distance", 6, 3, 12, 0.5));
    private final NumberSetting placeRange = add(new NumberSetting("PlaceRange", "Placement reach", 4.5, 1, 6, 0.1));
    private final NumberSetting minDamage = add(new NumberSetting("MinDamage", "Min damage to target", 4, 0, 36, 0.5));
    private final NumberSetting maxSelf = add(new NumberSetting("MaxSelf", "Max damage to yourself", 8, 0, 36, 0.5));
    private final NumberSetting delay = add(new NumberSetting("Delay", "Ticks between actions", 3, 0, 20, 1));
    private int timer;
    private LivingEntity target;

    public CartAuraModule() {
        super("CartAura", "TNT minecart placement and ignition", Category.COMBAT);
        listen(TickEvent.class, e -> tick());
    }

    @Override public String getInfo() { return target == null ? "" : target.getName().getString(); }
    @Override protected void onDisable() { target = null; }

    private void tick() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) { target = null; return; }
        if (timer > 0) { timer--; return; }
        target = TargetUtil.best(targetRange.get(), true, false, true, false);
        if (target == null) return;
        TargetUtil.report(target);
        Vec3d eye = mc.player.getEyePos();
        double rsq = placeRange.get() * placeRange.get();

        // 1) light an existing TNT minecart when it would hurt the target and not you
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof TntMinecartEntity c) || !c.isAlive() || c.isPrimed()) continue;
            Vec3d p = new Vec3d(c.getX(), c.getY(), c.getZ());
            if (eye.squaredDistanceTo(p) > rsq) continue;
            float dt = DamageUtil.explosion(target, p, 4.5f);
            float ds = DamageUtil.explosion(mc.player, p, 4.5f);
            if (dt < minDamage.asFloat() || ds > maxSelf.asFloat() || ds >= mc.player.getHealth() + mc.player.getAbsorptionAmount() - 1f) continue;
            int slot = InvUtil.findHotbar(s -> s.isOf(Items.FLINT_AND_STEEL) || s.isOf(Items.FIRE_CHARGE));
            if (slot == -1) return;
            InvUtil.select(slot);
            mc.interactionManager.interactEntity(mc.player, c, Hand.MAIN_HAND);
            mc.player.swingHand(Hand.MAIN_HAND);
            timer = delay.asInt();
            return;
        }

        BlockPos tb = target.getBlockPos();

        // 2) put a TNT minecart on a rail next to the target
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -1; dy <= 0; dy++) {
                    BlockPos pos = tb.add(dx, dy, dz);
                    if (!mc.world.getBlockState(pos).isIn(BlockTags.RAILS)) continue;
                    if (eye.squaredDistanceTo(Vec3d.ofCenter(pos)) > rsq) continue;
                    if (!mc.world.getOtherEntities(null, new Box(pos), en -> en instanceof AbstractMinecartEntity).isEmpty()) continue;
                    int slot = InvUtil.findHotbar(s -> s.isOf(Items.TNT_MINECART));
                    if (slot == -1) return;
                    InvUtil.select(slot);
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
                            new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false));
                    mc.player.swingHand(Hand.MAIN_HAND);
                    timer = delay.asInt();
                    return;
                }
            }
        }

        // 3) place a rail on solid ground at or next to the target
        int rail = InvUtil.findHotbar(s -> s.isOf(Items.RAIL));
        if (rail == -1) return;
        BlockPos[] cands = { tb, tb.east(), tb.west(), tb.north(), tb.south() };
        for (BlockPos p : cands) {
            if (!mc.world.getBlockState(p).isReplaceable()) continue;
            BlockPos g = p.down();
            if (!mc.world.getBlockState(g).isSolidBlock(mc.world, g)) continue;
            if (eye.squaredDistanceTo(Vec3d.ofCenter(p)) > rsq) continue;
            InvUtil.select(rail);
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
                    new BlockHitResult(new Vec3d(p.getX() + 0.5, p.getY(), p.getZ() + 0.5), Direction.UP, g, false));
            mc.player.swingHand(Hand.MAIN_HAND);
            timer = delay.asInt();
            return;
        }
    }
}
