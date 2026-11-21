package dev.nexus.module.modules;

import dev.nexus.NexusClient;
import dev.nexus.event.TickEvent;
import dev.nexus.module.Category;
import dev.nexus.module.Module;
import dev.nexus.setting.BoolSetting;
import dev.nexus.setting.ModeSetting;
import dev.nexus.setting.NumberSetting;
import dev.nexus.setting.Setting;
import dev.nexus.util.RotationUtil;
import dev.nexus.util.TargetUtil;
import net.minecraft.client.option.GameOptions;
import net.minecraft.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Duel bot with selectable fighting styles. Each style switches on a set of combat modules, tunes
 * them, and uses a matching movement pattern (preferred distance, hopping). It does NOT path-find,
 * avoid lava/cliffs, place obsidian or know any server's rules. Toggle it off any time.
 */
public final class CombatBotModule extends Module {
    /** True while a module is eating/drinking; the bot and KillAura pause. */
    public static boolean EATING;

    private static final String[] STYLES = { "Sword", "Axe", "Crystal", "Mace", "Cart", "Pot" };

    private final ModeSetting style = add(new ModeSetting("Style", "Fighting style", "Sword", STYLES));
    private final NumberSetting range = add(new NumberSetting("Range", "How far it looks for targets", 30, 5, 60, 1));
    private final BoolSetting players = add(new BoolSetting("Players", "Fight players", true));
    private final BoolSetting mobs = add(new BoolSetting("Mobs", "Fight hostile mobs", false));
    private final BoolSetting jump = add(new BoolSetting("Jump", "Bunny-hop in melee styles", true));
    private final NumberSetting strafe = add(new NumberSetting("StrafeTicks", "Ticks between strafe changes", 18, 5, 60, 1));

    private final Map<String, Boolean> before = new HashMap<>();
    private String applied = "";
    private int strafeTimer, strafeDir = 1;
    private LivingEntity target;

    public CombatBotModule() {
        super("CombatBot", "Duel bot with fighting styles", Category.MISC);
        listen(TickEvent.class, e -> tick());
    }

    @Override public String getInfo() { return style.get() + (target == null ? "" : " " + target.getName().getString()); }

    private static String[] preset(String s) {
        return switch (s) {
            case "Axe" -> new String[] { "KillAura", "AutoCrit", "STap", "AutoTotem", "AutoGapple", "AutoArmor" };
            case "Crystal" -> new String[] { "CrystalAura", "AutoTotem", "AutoGapple", "AutoArmor", "AutoMend" };
            case "Mace" -> new String[] { "MaceAura", "NoFall", "AutoTotem", "AutoGapple", "AutoArmor" };
            case "Cart" -> new String[] { "CartAura", "KillAura", "AutoTotem", "AutoGapple", "AutoArmor" };
            case "Pot" -> new String[] { "KillAura", "AutoPot", "AutoGapple", "AutoArmor", "AutoMend" };
            default -> new String[] { "KillAura", "AutoCrit", "WTap", "AutoTotem", "AutoGapple", "AutoArmor" };
        };
    }

    private static double desiredDistance(String s) {
        return switch (s) {
            case "Crystal" -> 5.5;
            case "Cart" -> 4.5;
            case "Mace" -> 3.2;
            default -> 2.6;
        };
    }

    private static void tweak(String module, String setting, Object v) {
        Module m = NexusClient.MODULES.get(module);
        if (m == null) return;
        for (Setting<?> s : m.getSettings()) {
            if (!s.getName().equals(setting)) continue;
            if (s instanceof ModeSetting ms && v instanceof String str) ms.set(str);
            else if (s instanceof BoolSetting bs && v instanceof Boolean b) bs.set(b);
            else if (s instanceof NumberSetting ns && v instanceof Number n) ns.set(n.doubleValue());
        }
    }

    private void apply() {
        String s = style.get();
        for (String name : preset(s)) {
            Module m = NexusClient.MODULES.get(name);
            if (m == null) continue;
            before.putIfAbsent(name, m.isEnabled());
            m.setEnabled(true);
        }
        switch (s) {
            case "Axe" -> { tweak("KillAura", "Weapon", "Auto"); tweak("KillAura", "CritsOnly", true); }
            case "Pot" -> { tweak("KillAura", "Weapon", "Sword"); tweak("KillAura", "CritsOnly", false); }
            case "Cart" -> { tweak("KillAura", "Weapon", "Sword"); tweak("KillAura", "CritsOnly", false); }
            case "Sword" -> { tweak("KillAura", "Weapon", "Sword"); tweak("KillAura", "CritsOnly", true); }
            default -> { }
        }
        applied = s;
    }

    private void revert() {
        for (Map.Entry<String, Boolean> e : before.entrySet()) {
            Module m = NexusClient.MODULES.get(e.getKey());
            if (m != null && !e.getValue()) m.setEnabled(false);
        }
        before.clear();
        applied = "";
    }

    @Override
    protected void onEnable() {
        // Never start by itself from a saved profile.
        if (mc.player == null) { setEnabled(false); return; }
        apply();
    }

    @Override
    protected void onDisable() {
        releaseKeys();
        EATING = false;
        target = null;
        revert();
    }

    private void tick() {
        if (mc.player == null || mc.world == null || mc.currentScreen != null) { releaseKeys(); return; }
        if (!applied.equals(style.get())) { revert(); apply(); }
        GameOptions o = mc.options;

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

        String s = style.get();
        double want = desiredDistance(s);
        double dist = Math.sqrt(TargetUtil.distSqToBox(mc.player.getEyePos(), target.getBoundingBox()));
        boolean chase = dist > want + 0.4;
        o.forwardKey.setPressed(chase);
        o.sprintKey.setPressed(chase);
        o.backKey.setPressed(dist < want - 1.0);

        if (dist < want + 3.0) {
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

        boolean melee = !s.equals("Crystal") && !s.equals("Cart");
        o.jumpKey.setPressed(jump.get() && melee && mc.player.isOnGround() && dist < 3.6);
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
    }
}
