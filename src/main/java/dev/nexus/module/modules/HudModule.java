package dev.nexus.module.modules;

import dev.nexus.NexusClient;
import dev.nexus.event.Render2DEvent;
import dev.nexus.event.TickEvent;
import dev.nexus.module.Category;
import dev.nexus.module.Module;
import dev.nexus.render.RenderUtil;
import dev.nexus.setting.BoolSetting;
import dev.nexus.setting.ColorSetting;
import dev.nexus.util.TargetUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** FPS / ping / coordinates / totems panel, armor status and module ArrayList. */
public final class HudModule extends Module {
    private static final EquipmentSlot[] ARMOR = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };

    private final BoolSetting showFps = add(new BoolSetting("FPS", "Show frames per second", true));
    private final BoolSetting showPing = add(new BoolSetting("Ping", "Show latency", true));
    private final BoolSetting showCoords = add(new BoolSetting("Coordinates", "Show XYZ", true));
    private final BoolSetting showTotems = add(new BoolSetting("Totems", "Show totem count", true));
    private final BoolSetting showArmor = add(new BoolSetting("Armor", "Show armor status", true));
    private final BoolSetting showModules = add(new BoolSetting("ArrayList", "List enabled modules", true));
    private final BoolSetting showTarget = add(new BoolSetting("TargetHUD", "Show current combat target", true));
    private final BoolSetting background = add(new BoolSetting("Background", "Dark panel behind text", true));
    private final ColorSetting textColor = add(new ColorSetting("TextColor", "Color of the info text", 0xFFFFFFFF));
    private final BoolSetting useTheme = add(new BoolSetting("ThemeBars", "Use the menu theme for bars and list", true));
    private final ColorSetting barColor = add(new ColorSetting("BarColor", "Color of bars and list", 0xFF8B5CF6));

    private final String[] lines = new String[5];
    private int lineCount;
    private int lineWidth;
    private int tick;
    private String[] labels = new String[0];
    private int[] widths = new int[0];

    public HudModule() {
        super("HUD", "Heads-up display", Category.HUD);
        barColor.visibleWhen(() -> !useTheme.get());
        listen(TickEvent.class, e -> update());
        listen(Render2DEvent.class, this::render);
    }

    private void update() {
        if (mc.player == null) { lineCount = 0; return; }
        int n = 0;
        if (showFps.get()) lines[n++] = "FPS " + mc.getCurrentFps();
        if (showPing.get()) lines[n++] = "Ping " + pingText();
        if (showCoords.get()) {
            lines[n++] = "XYZ " + (int) Math.floor(mc.player.getX()) + " "
                    + (int) Math.floor(mc.player.getY()) + " " + (int) Math.floor(mc.player.getZ());
        }
        if (showTotems.get()) lines[n++] = "Totems " + countTotems();
        int w = 0;
        for (int i = 0; i < n; i++) w = Math.max(w, RenderUtil.width(lines[i]));
        lineCount = n;
        lineWidth = w;
        if (showModules.get() && tick++ % 5 == 0) rebuildList();
    }

    private String pingText() {
        var net = mc.getNetworkHandler();
        if (net == null) return "-";
        var entry = net.getPlayerListEntry(mc.player.getUuid());
        return entry == null ? "-" : entry.getLatency() + "ms";
    }

    private int countTotems() {
        // NOTE: PlayerInventory layout changed in 1.21.5+; verify offhand is counted.
        var inv = mc.player.getInventory();
        int c = 0;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            if (s.isOf(Items.TOTEM_OF_UNDYING)) c += s.getCount();
        }
        return c;
    }

    private static String label(Module m) {
        String info = m.getInfo();
        return info.isEmpty() ? m.getName() : m.getName() + " " + info;
    }

    private void rebuildList() {
        List<Module> list = new ArrayList<>();
        for (Module m : NexusClient.MODULES.all()) {
            if (m.isEnabled() && m != this && !(m instanceof ClickGuiModule)) list.add(m);
        }
        list.sort(Comparator.comparingInt((Module m) -> -RenderUtil.width(label(m))));
        String[] ls = new String[list.size()];
        int[] ws = new int[list.size()];
        for (int i = 0; i < ls.length; i++) {
            ls[i] = label(list.get(i));
            ws[i] = RenderUtil.width(ls[i]);
        }
        labels = ls;
        widths = ws;
    }

    private void render(Render2DEvent e) {
        if (mc.player == null) return;
        DrawContext c = e.context;
        int accent = useTheme.get() ? NexusClient.GUI_CFG.accent() : barColor.color();
        int txt = textColor.color();

        if (lineCount > 0) {
            int h = lineCount * 11 + 4;
            if (background.get()) c.fill(2, 2, 2 + lineWidth + 10, 2 + h, 0x90000000);
            c.fill(2, 2, 4, 2 + h, accent);
            for (int i = 0; i < lineCount; i++) RenderUtil.text(c, lines[i], 8, 5 + i * 11, txt);
        }

        if (showTarget.get()) {
            LivingEntity t = TargetUtil.reported();
            if (t != null) {
                float hp = t.getHealth() + t.getAbsorptionAmount();
                float max = Math.max(1f, t.getMaxHealth());
                String name = t.getName().getString();
                String hpText = (Math.round(hp * 10f) / 10f) + " HP";
                int w = Math.max(90, Math.max(RenderUtil.width(name), RenderUtil.width(hpText)) + 12);
                int x = e.width / 2 + 14, y = e.height / 2 + 14;
                c.fill(x, y, x + w, y + 34, 0x90000000);
                c.fill(x, y, x + 2, y + 34, accent);
                RenderUtil.text(c, name, x + 6, y + 4, 0xFFFFFFFF);
                RenderUtil.text(c, hpText, x + 6, y + 15, 0xFFB0B0C4);
                c.fill(x + 6, y + 27, x + w - 6, y + 30, 0xFF202020);
                float f = Math.min(1f, hp / max);
                int col = 0xFF000000 | (java.awt.Color.HSBtoRGB(f * 0.33f, 0.9f, 1f) & 0xFFFFFF);
                c.fill(x + 6, y + 27, x + 6 + Math.round((w - 12) * f), y + 30, col);
            }
        }

        if (showArmor.get()) {
            int x = e.width - 4 * 18 - 4;
            int y = e.height - 20;
            for (EquipmentSlot slot : ARMOR) {
                ItemStack s = mc.player.getEquippedStack(slot);
                if (!s.isEmpty()) {
                    c.drawItem(s, x, y);
                    if (s.isDamageable()) {
                        float f = 1f - (float) s.getDamage() / (float) s.getMaxDamage();
                        int col = 0xFF000000 | (java.awt.Color.HSBtoRGB(f * 0.33f, 1f, 1f) & 0xFFFFFF);
                        c.fill(x, y + 17, x + 16, y + 19, 0xFF202020);
                        c.fill(x, y + 17, x + Math.round(16 * f), y + 19, col);
                    }
                }
                x += 18;
            }
        }

        if (showModules.get()) {
            String[] ls = labels;
            int[] ws = widths;
            int y = 3;
            for (int i = 0; i < ls.length; i++) {
                int x = e.width - ws[i] - 8;
                int col = useTheme.get() ? NexusClient.GUI_CFG.accentAt(i) : barColor.color();
                if (background.get()) c.fill(x - 3, y, e.width, y + 11, 0x90000000);
                c.fill(e.width - 2, y, e.width, y + 11, col);
                RenderUtil.text(c, ls[i], x, y + 2, col);
                y += 11;
            }
        }
    }
}
