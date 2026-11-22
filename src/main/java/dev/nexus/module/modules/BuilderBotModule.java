package dev.nexus.module.modules;

import dev.nexus.event.TickEvent;
import dev.nexus.module.Category;
import dev.nexus.module.Module;
import dev.nexus.setting.ModeSetting;
import dev.nexus.setting.NumberSetting;
import dev.nexus.util.InvUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Builds small structures in front of you. In creative mode it gives itself the blocks it needs
 * (hotbar slots 3-9) and places them against existing blocks. Stops by itself when done.
 * Structures are plain block lists, so new ones are easy to add in buildPlan().
 */
public final class BuilderBotModule extends Module {
    private record Entry(BlockPos pos, Block block) {}

    private final ModeSetting structure = add(new ModeSetting("Structure", "What to build", "Row5x1", "Row5x1", "Pizza"));
    private final ModeSetting rowBlock = add(new ModeSetting("RowBlock", "Block for the row", "Stone",
            "Stone", "Obsidian", "Glass", "Planks", "Wool", "Sandstone"));
    private final NumberSetting radius = add(new NumberSetting("Radius", "Pizza radius", 2, 1, 2, 1));
    private final NumberSetting perTick = add(new NumberSetting("PerTick", "Blocks per tick", 2, 1, 6, 1));

    private final List<Entry> plan = new ArrayList<>();
    private final Map<Block, Integer> slots = new HashMap<>();
    private int stuck;

    public BuilderBotModule() {
        super("BuilderBot", "Builds structures using creative blocks", Category.MISC);
        listen(TickEvent.class, e -> tick());
    }

    @Override public String getInfo() { return plan.isEmpty() ? "" : plan.size() + " left"; }

    @Override
    protected void onEnable() {
        // Never start by itself from a saved profile.
        if (mc.player == null || mc.interactionManager == null) { setEnabled(false); return; }
        if (!mc.interactionManager.getCurrentGameMode().isCreative()) {
            say("BuilderBot needs creative mode.");
            setEnabled(false);
            return;
        }
        buildPlan();
        slots.clear();
        stuck = 0;
        say("Building " + plan.size() + " blocks...");
    }

    @Override protected void onDisable() { plan.clear(); }

    private void say(String s) {
        if (mc.player != null) mc.player.sendMessage(Text.literal(s), false);
    }

    private Block rowBlockType() {
        return switch (rowBlock.get()) {
            case "Obsidian" -> Blocks.OBSIDIAN;
            case "Glass" -> Blocks.GLASS;
            case "Planks" -> Blocks.OAK_PLANKS;
            case "Wool" -> Blocks.WHITE_WOOL;
            case "Sandstone" -> Blocks.SANDSTONE;
            default -> Blocks.STONE;
        };
    }

    private void buildPlan() {
        plan.clear();
        BlockPos feet = mc.player.getBlockPos();
        Direction f = mc.player.getHorizontalFacing();
        Direction r = f.rotateYClockwise();

        if (structure.is("Row5x1")) {
            BlockPos center = feet.offset(f, 2);
            Block b = rowBlockType();
            for (int i = -2; i <= 2; i++) plan.add(new Entry(center.offset(r, i), b));
        } else {
            int rad = radius.asInt();
            BlockPos center = feet.offset(f, rad + 1);
            for (int dx = -rad; dx <= rad; dx++) {
                for (int dz = -rad; dz <= rad; dz++) {
                    int d2 = dx * dx + dz * dz;
                    if (d2 > rad * rad + 1) continue;
                    Block b;
                    if (d2 > (rad - 1) * (rad - 1) + 1) b = Blocks.ORANGE_TERRACOTTA;        // crust
                    else if (d2 <= 2 && ((dx + dz) & 1) == 0) b = Blocks.RED_CONCRETE;          // pepperoni
                    else b = Blocks.YELLOW_CONCRETE;                                            // cheese
                    plan.add(new Entry(center.add(dx, 0, dz), b));
                }
            }
        }
    }

    private void tick() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null || plan.isEmpty()) return;
        int placed = 0;
        Iterator<Entry> it = plan.iterator();
        while (it.hasNext() && placed < perTick.asInt()) {
            Entry e = it.next();
            BlockState st = mc.world.getBlockState(e.pos());
            if (st.isOf(e.block()) || !st.isReplaceable()) { it.remove(); continue; }
            if (tryPlace(e)) placed++;
        }
        if (plan.isEmpty()) {
            say("BuilderBot: done.");
            setEnabled(false);
            return;
        }
        stuck = placed == 0 ? stuck + 1 : 0;
        if (stuck > 60) {
            say("BuilderBot stopped: " + plan.size() + " blocks could not be placed (out of reach or nothing to attach to).");
            setEnabled(false);
        }
    }

    private boolean tryPlace(Entry e) {
        BlockPos p = e.pos();
        if (mc.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(p)) > 27.0) return false;
        if (mc.player.getBoundingBox().intersects(p.getX(), p.getY(), p.getZ(), p.getX() + 1, p.getY() + 1, p.getZ() + 1)) return false;

        for (Direction d : Direction.values()) {
            BlockPos n = p.offset(d);
            BlockState ns = mc.world.getBlockState(n);
            if (ns.isAir() || ns.isReplaceable()) continue; // needs a real block to attach to
            InvUtil.select(hotbarSlotFor(e.block()));
            Direction face = d.getOpposite();
            Vec3d hit = Vec3d.ofCenter(n).add(face.getOffsetX() * 0.5, face.getOffsetY() * 0.5, face.getOffsetZ() * 0.5);
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(hit, face, n, false));
            mc.player.swingHand(Hand.MAIN_HAND);
            return true;
        }
        return false;
    }

    /** Hotbar slot (2-8) that holds this block; fills it from the creative inventory when needed. */
    private int hotbarSlotFor(Block block) {
        int slot = slots.computeIfAbsent(block, b -> 2 + (slots.size() % 7));
        var item = block.asItem();
        if (!mc.player.getInventory().getStack(slot).isOf(item)) {
            mc.interactionManager.clickCreativeStack(new ItemStack(item, 64), 36 + slot);
        }
        return slot;
    }
}
