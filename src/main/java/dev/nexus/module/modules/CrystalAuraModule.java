package dev.nexus.module.modules;

import dev.nexus.event.TickEvent;
import dev.nexus.module.Category;
import dev.nexus.module.Module;
import dev.nexus.setting.BoolSetting;
import dev.nexus.setting.NumberSetting;
import dev.nexus.util.DamageUtil;
import dev.nexus.util.InvUtil;
import dev.nexus.util.RotationUtil;
import dev.nexus.util.TargetUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/** Crystal PvP: places crystals on obsidian/bedrock and breaks them, scored by target damage vs self damage. */
public final class CrystalAuraModule extends Module {
    private final BoolSetting place = add(new BoolSetting("Place", "Place crystals", true));
    private final BoolSetting breakC = add(new BoolSetting("Break", "Break crystals", true));
    private final NumberSetting targetRange = add(new NumberSetting("TargetRange", "Max target distance", 10, 3, 20, 1));
    private final NumberSetting placeRange = add(new NumberSetting("PlaceRange", "Placement reach", 4.5, 1, 6, 0.1));
    private final NumberSetting breakRange = add(new NumberSetting("BreakRange", "Break reach", 4.5, 1, 6, 0.1));
    private final NumberSetting minDamage = add(new NumberSetting("MinDamage", "Min damage to target", 4, 0, 36, 0.5));
    private final NumberSetting maxSelf = add(new NumberSetting("MaxSelf", "Max damage to yourself", 8, 0, 36, 0.5));
    private final BoolSetting antiSuicide = add(new BoolSetting("AntiSuicide", "Never act if it could kill you", true));
    private final NumberSetting placeDelay = add(new NumberSetting("PlaceDelay", "Ticks between placements", 2, 0, 20, 1));
    private final NumberSetting breakDelay = add(new NumberSetting("BreakDelay", "Ticks between hits", 1, 0, 20, 1));
    private final BoolSetting autoSwitch = add(new BoolSetting("AutoSwitch", "Switch to crystals in hotbar", true));
    private final BoolSetting rotate = add(new BoolSetting("Rotate", "Turn the camera towards crystals", false));

    private int placeTimer, breakTimer;
    private LivingEntity target;

    public CrystalAuraModule() {
        super("CrystalAura", "Crystal placement/breaking assistance", Category.COMBAT);
        listen(TickEvent.class, e -> tick());
    }

    @Override public String getInfo() { return target == null ? "" : target.getName().getString(); }
    @Override protected void onDisable() { target = null; }

    private void tick() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) {
            target = null;
            return;
        }
        if (placeTimer > 0) placeTimer--;
        if (breakTimer > 0) breakTimer--;
        target = TargetUtil.best(targetRange.get(), true, false, true, false);
        if (target == null) return;
        TargetUtil.report(target);

        if (breakC.get() && breakTimer == 0 && doBreak()) breakTimer = breakDelay.asInt();
        if (place.get() && placeTimer == 0 && doPlace()) placeTimer = placeDelay.asInt();
    }

    private boolean acceptable(float dmgTarget, float dmgSelf) {
        if (dmgTarget < minDamage.asFloat() || dmgSelf > maxSelf.asFloat()) return false;
        return !(antiSuicide.get() && dmgSelf >= mc.player.getHealth() + mc.player.getAbsorptionAmount() - 1f);
    }

    private boolean doBreak() {
        Vec3d eye = mc.player.getEyePos();
        double rsq = breakRange.get() * breakRange.get();
        EndCrystalEntity best = null;
        float bestScore = -Float.MAX_VALUE;
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof EndCrystalEntity c) || !c.isAlive()) continue;
            Vec3d p = new Vec3d(c.getX(), c.getY(), c.getZ());
            if (eye.squaredDistanceTo(p) > rsq) continue;
            float dt = DamageUtil.crystal(target, p);
            float ds = DamageUtil.crystal(mc.player, p);
            if (!acceptable(dt, ds)) continue;
            if (dt - ds > bestScore) { bestScore = dt - ds; best = c; }
        }
        if (best == null) return false;
        if (rotate.get()) RotationUtil.rotateTowards(best, 180f);
        mc.interactionManager.attackEntity(mc.player, best);
        mc.player.swingHand(Hand.MAIN_HAND);
        return true;
    }

    private boolean doPlace() {
        int r = (int) Math.ceil(placeRange.get());
        double rsq = placeRange.get() * placeRange.get();
        Vec3d eye = mc.player.getEyePos();
        BlockPos base = mc.player.getBlockPos();
        BlockPos.Mutable m = new BlockPos.Mutable();
        BlockPos.Mutable up = new BlockPos.Mutable();
        BlockPos bestPos = null;
        float bestScore = -Float.MAX_VALUE;

        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    m.set(base.getX() + dx, base.getY() + dy, base.getZ() + dz);
                    BlockState bs = mc.world.getBlockState(m);
                    if (!bs.isOf(Blocks.OBSIDIAN) && !bs.isOf(Blocks.BEDROCK)) continue;
                    up.set(m.getX(), m.getY() + 1, m.getZ());
                    if (!mc.world.getBlockState(up).isAir()) continue;
                    up.set(m.getX(), m.getY() + 2, m.getZ());
                    if (!mc.world.getBlockState(up).isAir()) continue;

                    Vec3d center = new Vec3d(m.getX() + 0.5, m.getY() + 1.0, m.getZ() + 0.5);
                    if (eye.squaredDistanceTo(center) > rsq) continue;

                    float dt = DamageUtil.crystal(target, center);
                    if (dt < minDamage.asFloat()) continue;
                    float ds = DamageUtil.crystal(mc.player, center);
                    if (!acceptable(dt, ds)) continue;
                    float score = dt - ds;
                    if (score <= bestScore) continue;

                    Box box = new Box(m.getX(), m.getY() + 1, m.getZ(), m.getX() + 1, m.getY() + 3, m.getZ() + 1);
                    if (!mc.world.getOtherEntities(null, box).isEmpty()) continue;
                    bestScore = score;
                    bestPos = m.toImmutable();
                }
            }
        }
        if (bestPos == null) return false;

        Hand hand;
        if (mc.player.getOffHandStack().isOf(Items.END_CRYSTAL)) {
            hand = Hand.OFF_HAND;
        } else {
            if (!mc.player.getMainHandStack().isOf(Items.END_CRYSTAL)) {
                if (!autoSwitch.get()) return false;
                int slot = InvUtil.findHotbar(s -> s.isOf(Items.END_CRYSTAL));
                if (slot == -1) return false;
                InvUtil.select(slot);
            }
            hand = Hand.MAIN_HAND;
        }
        BlockHitResult hit = new BlockHitResult(
                new Vec3d(bestPos.getX() + 0.5, bestPos.getY() + 1.0, bestPos.getZ() + 0.5),
                Direction.UP, bestPos, false);
        mc.interactionManager.interactBlock(mc.player, hand, hit);
        mc.player.swingHand(hand);
        return true;
    }
}
