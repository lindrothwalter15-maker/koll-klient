package dev.nexus.gui;

import dev.nexus.NexusClient;
import dev.nexus.module.Category;
import dev.nexus.module.Module;
import dev.nexus.module.modules.ClickGuiModule;
import dev.nexus.render.RenderUtil;
import dev.nexus.setting.BoolSetting;
import dev.nexus.setting.ColorSetting;
import dev.nexus.setting.ModeSetting;
import dev.nexus.setting.NumberSetting;
import dev.nexus.setting.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Immediate-mode ClickGUI. Mouse buttons and keys are polled through GLFW so they do not depend on
 * the Screen input method signatures (which changed in 1.21.9+). Scrolling uses mouseScrolled.
 * Left click: toggle module. Right click: expand settings. Drag a header: move the panel.
 * Mouse wheel over a panel: scroll it. Type: search. Esc: close.
 */
public final class ClickGuiScreen extends Screen {
    private static final int PW = 116, HEADER = 20, ROW = 17, SROW = 15, GAP = 8, PAD = 5, TOP = 34;
    private static final int HEAD_BG = 0xFF171724, ROW_BG = 0xFF14141E, SET_BG = 0xFF0C0C14, BOX = 0xFF2A2A3C;
    private static final int HOVER = 0x30FFFFFF, TEXT = 0xFFE8E8F2, DIM = 0xFF8C8CA6;

    /** Panel positions survive closing and reopening the menu (until the game is closed). */
    private static final Map<Category, int[]> POS = new EnumMap<>(Category.class);

    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final Map<Module, Float> progress = new IdentityHashMap<>();
    private final Set<Module> expanded = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<Category, float[]> scroll = new EnumMap<>(Category.class); // {target, shown}
    private final Map<Category, int[]> bounds = new EnumMap<>(Category.class);   // {x, y, w, h}
    private final boolean[] prevKey = new boolean[GLFW.GLFW_KEY_LAST + 1];

    private String search = "";
    private float open;
    private float dt;
    private long last;
    private long win;
    private boolean init, prevL, prevR;
    private NumberSetting slider;
    private int sliderX, sliderW;
    private Module listening;
    private String tooltip;
    private Category dragging;
    private int dragDx, dragDy;
    private int clipTop, clipBottom;

    public ClickGuiScreen() {
        super(Text.literal("Nexus"));
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void removed() {
        NexusClient.CONFIG.saveAsync(NexusClient.CONFIG.currentProfile());
        super.removed();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        for (Map.Entry<Category, int[]> e : bounds.entrySet()) {
            int[] b = e.getValue();
            if (mouseX >= b[0] && mouseX < b[0] + b[2] && mouseY >= b[1] && mouseY < b[1] + b[3]) {
                float[] sc = scroll.computeIfAbsent(e.getKey(), k -> new float[2]);
                sc[0] += (float) (-verticalAmount * ROW * 1.5);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        win = mc.getWindow().getHandle();
        long now = System.nanoTime();
        dt = last == 0 ? 0f : Math.min((now - last) / 1_000_000_000f, 0.1f);
        last = now;

        boolean l = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean r = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        if (!init) { snapshotKeys(); prevL = l; prevR = r; init = true; }
        boolean lClick = l && !prevL;
        boolean rClick = r && !prevR;
        prevL = l;
        prevR = r;
        if (!l) { slider = null; dragging = null; }

        open = RenderUtil.approach(open, 1f, dt * 5f);
        handleKeys();

        if (NexusClient.GUI_CFG.blur()) super.render(ctx, mx, my, delta);
        else ctx.fill(0, 0, width, height, RenderUtil.alpha(0xFF000000, 0.5f * open));

        tooltip = null;
        bounds.clear();
        drawSearch(ctx);

        // Default positions: centered row over every category that has modules (kept when searching).
        List<Category> all = new ArrayList<>();
        for (Category c : Category.values()) if (!NexusClient.MODULES.inCategory(c).isEmpty()) all.add(c);
        int total = all.size() * PW + Math.max(0, all.size() - 1) * GAP;
        int startX = (width - total) / 2;
        for (int i = 0; i < all.size(); i++) {
            Category c = all.get(i);
            if (!POS.containsKey(c)) POS.put(c, new int[] { startX + i * (PW + GAP), TOP });
        }

        if (dragging != null && l) {
            int[] p = POS.get(dragging);
            if (p != null) { p[0] = mx - dragDx; p[1] = my - dragDy; }
        }

        int slide = Math.round((1f - open) * -10f);
        for (Category c : all) {
            int[] p = POS.get(c);
            p[0] = Math.max(0, Math.min(Math.max(0, width - PW), p[0]));
            p[1] = Math.max(0, Math.min(Math.max(0, height - HEADER), p[1]));
            List<Module> mods = modulesFor(c);
            if (mods.isEmpty()) continue;
            drawPanel(ctx, c, mods, p[0], p[1] + slide, mx, my, lClick, rClick);
        }

        if (tooltip != null) {
            int tw = RenderUtil.width(tooltip);
            ctx.fill(mx + 6, my + 6, mx + 14 + tw, my + 19, 0xF0101018);
            RenderUtil.text(ctx, tooltip, mx + 10, my + 9, TEXT);
        }
    }

    private void drawSearch(DrawContext ctx) {
        int sw = 170, sx = (width - sw) / 2, sy = 8;
        ctx.fill(sx, sy, sx + sw, sy + 16, RenderUtil.alpha(HEAD_BG, open));
        ctx.fill(sx, sy + 15, sx + sw, sy + 16, RenderUtil.alpha(NexusClient.GUI_CFG.accent(), open));
        boolean caret = (System.currentTimeMillis() / 500L) % 2 == 0;
        String shown = search.isEmpty() ? "Type to search..." : search + (caret ? "_" : "");
        RenderUtil.text(ctx, shown, sx + 6, sy + 4, RenderUtil.alpha(search.isEmpty() ? DIM : TEXT, open));
    }

    private int contentHeight(List<Module> mods) {
        int h = 0;
        for (Module m : mods) {
            h += ROW;
            float p = progress.getOrDefault(m, 0f);
            if (p > 0.001f) {
                int vis = 0;
                for (Setting<?> s : m.getSettings()) if (s.isVisible()) vis++;
                h += Math.round((vis + 1) * SROW * p);
            }
        }
        return h;
    }

    private void drawPanel(DrawContext ctx, Category cat, List<Module> mods, int x, int y,
                           int mx, int my, boolean lClick, boolean rClick) {
        int accent = NexusClient.GUI_CFG.accent();
        float a = open;

        int contentH = contentHeight(mods);
        int maxVisible = Math.max(2 * ROW, height - y - HEADER - 8);
        int visibleH = Math.min(contentH, maxVisible);
        int maxScroll = Math.max(0, contentH - visibleH);
        float[] sc = scroll.computeIfAbsent(cat, k -> new float[2]);
        sc[0] = Math.max(0f, Math.min((float) maxScroll, sc[0]));
        sc[1] += (sc[0] - sc[1]) * Math.min(1f, dt * 14f);
        if (Math.abs(sc[0] - sc[1]) < 0.3f) sc[1] = sc[0];
        int off = Math.round(sc[1]);
        bounds.put(cat, new int[] { x, y, PW, HEADER + visibleH + 1 });

        ctx.fill(x, y, x + PW, y + HEADER, RenderUtil.alpha(HEAD_BG, a));
        ctx.fill(x, y + HEADER - 1, x + PW, y + HEADER, RenderUtil.alpha(accent, a));
        RenderUtil.text(ctx, cat.getDisplayName(), x + PAD, y + 6, RenderUtil.alpha(TEXT, a));
        String cnt = Integer.toString(mods.size());
        RenderUtil.text(ctx, cnt, x + PW - PAD - RenderUtil.width(cnt), y + 6, RenderUtil.alpha(DIM, a));

        boolean hoverHeader = mx >= x && mx < x + PW && my >= y && my < y + HEADER;
        if (hoverHeader) {
            tooltip = "Drag to move";
            if (lClick && dragging == null) { dragging = cat; dragDx = mx - x; dragDy = my - y; }
        }

        int top = y + HEADER;
        int bottom = top + visibleH;
        clipTop = top;
        clipBottom = bottom;

        ctx.enableScissor(x, top, x + PW, bottom);
        int cy = top - off;
        for (Module m : mods) {
            boolean hover = mx >= x && mx < x + PW && my >= cy && my < cy + ROW && my >= top && my < bottom;
            ctx.fill(x, cy, x + PW, cy + ROW, RenderUtil.alpha(ROW_BG, a));
            if (m.isEnabled()) {
                ctx.fill(x, cy, x + PW, cy + ROW, RenderUtil.alpha(accent, 0.30f * a));
                ctx.fill(x, cy, x + 2, cy + ROW, RenderUtil.alpha(accent, a));
            }
            if (hover) {
                ctx.fill(x, cy, x + PW, cy + ROW, RenderUtil.alpha(HOVER, a));
                tooltip = m.getDescription();
                if (lClick && !(m instanceof ClickGuiModule)) m.toggle();
                if (rClick) {
                    if (!expanded.remove(m)) expanded.add(m);
                }
            }
            RenderUtil.text(ctx, m.getName(), x + PAD + 2, cy + 5,
                    RenderUtil.alpha(m.isEnabled() ? TEXT : 0xFFB0B0C4, a));
            String sign = expanded.contains(m) ? "-" : "+";
            RenderUtil.text(ctx, sign, x + PW - PAD - RenderUtil.width(sign), cy + 5, RenderUtil.alpha(DIM, a));
            cy += ROW;

            float target = expanded.contains(m) ? 1f : 0f;
            float p = RenderUtil.approach(progress.getOrDefault(m, 0f), target, dt * 8f);
            progress.put(m, p);
            if (p > 0.001f) cy += drawSettings(ctx, m, x, cy, p, mx, my, lClick);
        }
        ctx.disableScissor();

        if (maxScroll > 0) {
            int barH = Math.max(12, visibleH * visibleH / contentH);
            int barY = top + Math.round((visibleH - barH) * (sc[1] / maxScroll));
            ctx.fill(x + PW - 2, barY, x + PW, barY + barH, RenderUtil.alpha(accent, a));
        }
        ctx.fill(x, bottom, x + PW, bottom + 1, RenderUtil.alpha(accent, a));
    }

    private int drawSettings(DrawContext ctx, Module m, int x, int y, float p, int mx, int my, boolean lClick) {
        int accent = NexusClient.GUI_CFG.accent();
        float a = open;
        List<Setting<?>> vis = new ArrayList<>();
        for (Setting<?> s : m.getSettings()) if (s.isVisible()) vis.add(s);
        int full = (vis.size() + 1) * SROW;
        int h = Math.round(full * p);
        boolean inClip = my >= clipTop && my < clipBottom;

        ctx.enableScissor(x, y, x + PW, y + h);
        int ry = y;
        for (Setting<?> s : vis) {
            boolean hov = inClip && mx >= x && mx < x + PW && my >= ry && my < ry + SROW && my < y + h;
            ctx.fill(x, ry, x + PW, ry + SROW, RenderUtil.alpha(SET_BG, a));
            if (hov) {
                ctx.fill(x, ry, x + PW, ry + SROW, RenderUtil.alpha(HOVER, a));
                tooltip = s.getDescription();
            }
            int tcol = RenderUtil.alpha(TEXT, a);
            if (s instanceof BoolSetting b) {
                RenderUtil.text(ctx, b.getName(), x + PAD, ry + 4, tcol);
                int bx = x + PW - PAD - 10, by = ry + 3;
                ctx.fill(bx, by, bx + 9, by + 9, RenderUtil.alpha(BOX, a));
                if (b.get()) ctx.fill(bx + 2, by + 2, bx + 7, by + 7, RenderUtil.alpha(accent, a));
                if (hov && lClick) b.toggle();
            } else if (s instanceof NumberSetting n) {
                RenderUtil.text(ctx, n.getName(), x + PAD, ry + 1, tcol);
                String val = fmt(n);
                RenderUtil.text(ctx, val, x + PW - PAD - RenderUtil.width(val), ry + 1, RenderUtil.alpha(DIM, a));
                int sx = x + PAD, sw = PW - 2 * PAD, by = ry + SROW - 4;
                ctx.fill(sx, by, sx + sw, by + 2, RenderUtil.alpha(BOX, a));
                double t = (n.get() - n.getMin()) / (n.getMax() - n.getMin());
                ctx.fill(sx, by, sx + (int) (sw * t), by + 2, RenderUtil.alpha(accent, a));
                if (hov && lClick) { slider = n; sliderX = sx; sliderW = sw; }
                if (slider == n) {
                    double nt = Math.max(0.0, Math.min(1.0, (mx - sliderX) / (double) sliderW));
                    n.set(n.getMin() + nt * (n.getMax() - n.getMin()));
                }
            } else if (s instanceof ModeSetting md) {
                RenderUtil.text(ctx, md.getName(), x + PAD, ry + 4, tcol);
                String v = md.get();
                RenderUtil.text(ctx, v, x + PW - PAD - RenderUtil.width(v), ry + 4, RenderUtil.alpha(accent, a));
                if (hov && lClick) md.cycle();
            } else if (s instanceof ColorSetting cs) {
                RenderUtil.text(ctx, cs.getName() + (cs.isRainbow() ? " (rainbow)" : ""), x + PAD, ry + 4, tcol);
                int bx = x + PW - PAD - 10, by = ry + 3;
                ctx.fill(bx, by, bx + 9, by + 9, cs.color());
                if (hov && lClick) cs.setRainbow(!cs.isRainbow());
            }
            ry += SROW;
        }

        boolean hovBind = inClip && mx >= x && mx < x + PW && my >= ry && my < ry + SROW && my < y + h;
        ctx.fill(x, ry, x + PW, ry + SROW, RenderUtil.alpha(SET_BG, a));
        if (hovBind) {
            ctx.fill(x, ry, x + PW, ry + SROW, RenderUtil.alpha(HOVER, a));
            tooltip = "Click, then press a key. Backspace clears.";
            if (lClick) { listening = m; snapshotKeys(); }
        }
        RenderUtil.text(ctx, "Bind", x + PAD, ry + 4, RenderUtil.alpha(TEXT, a));
        String kn = listening == m ? "..." : keyName(m.getKeyBind());
        RenderUtil.text(ctx, kn, x + PW - PAD - RenderUtil.width(kn), ry + 4, RenderUtil.alpha(accent, a));
        ctx.disableScissor();
        return h;
    }

    private void handleKeys() {
        if (listening != null) {
            for (int k = GLFW.GLFW_KEY_SPACE; k <= GLFW.GLFW_KEY_LAST; k++) {
                if (!edge(k)) continue;
                if (k == GLFW.GLFW_KEY_ESCAPE) { listening = null; return; }
                boolean clear = k == GLFW.GLFW_KEY_BACKSPACE || k == GLFW.GLFW_KEY_DELETE;
                listening.setKeyBind(clear ? -1 : k);
                listening = null;
                return;
            }
            return;
        }
        for (int k = GLFW.GLFW_KEY_A; k <= GLFW.GLFW_KEY_Z; k++) {
            if (edge(k) && search.length() < 24) search += (char) ('a' + (k - GLFW.GLFW_KEY_A));
        }
        for (int k = GLFW.GLFW_KEY_0; k <= GLFW.GLFW_KEY_9; k++) {
            if (edge(k) && search.length() < 24) search += (char) ('0' + (k - GLFW.GLFW_KEY_0));
        }
        if (edge(GLFW.GLFW_KEY_SPACE) && search.length() < 24) search += ' ';
        if (edge(GLFW.GLFW_KEY_BACKSPACE) && !search.isEmpty()) search = search.substring(0, search.length() - 1);

        int openKey = NexusClient.GUI_CFG.getKeyBind();
        boolean typing = (openKey >= GLFW.GLFW_KEY_A && openKey <= GLFW.GLFW_KEY_Z)
                || (openKey >= GLFW.GLFW_KEY_0 && openKey <= GLFW.GLFW_KEY_9)
                || openKey == GLFW.GLFW_KEY_SPACE || openKey == GLFW.GLFW_KEY_BACKSPACE;
        if (openKey >= 0 && !typing && edge(openKey)) mc.setScreen(null);
    }

    private boolean edge(int k) {
        if (k < 0 || k > GLFW.GLFW_KEY_LAST) return false;
        boolean down = GLFW.glfwGetKey(win, k) == GLFW.GLFW_PRESS;
        boolean e = down && !prevKey[k];
        prevKey[k] = down;
        return e;
    }

    private void snapshotKeys() {
        long w = mc.getWindow().getHandle();
        for (int k = GLFW.GLFW_KEY_SPACE; k <= GLFW.GLFW_KEY_LAST; k++) {
            prevKey[k] = GLFW.glfwGetKey(w, k) == GLFW.GLFW_PRESS;
        }
    }

    private List<Module> modulesFor(Category cat) {
        List<Module> base = NexusClient.MODULES.inCategory(cat);
        String q = search.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) return base;
        List<Module> out = new ArrayList<>();
        for (Module m : base) if (m.getName().toLowerCase(Locale.ROOT).contains(q)) out.add(m);
        return out;
    }

    private static String fmt(NumberSetting n) {
        double v = n.get();
        if (n.getStep() >= 1) return Long.toString(Math.round(v));
        return Double.toString(Math.round(v * 100.0) / 100.0);
    }

    private static String keyName(int k) {
        if (k < 0) return "None";
        return switch (k) {
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RShift";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LShift";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCtrl";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCtrl";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "RAlt";
            case GLFW.GLFW_KEY_LEFT_ALT -> "LAlt";
            case GLFW.GLFW_KEY_SPACE -> "Space";
            case GLFW.GLFW_KEY_TAB -> "Tab";
            case GLFW.GLFW_KEY_UP -> "Up";
            case GLFW.GLFW_KEY_DOWN -> "Down";
            case GLFW.GLFW_KEY_LEFT -> "Left";
            case GLFW.GLFW_KEY_RIGHT -> "Right";
            default -> {
                String n = GLFW.glfwGetKeyName(k, 0);
                yield n != null ? n.toUpperCase(Locale.ROOT) : "Key " + k;
            }
        };
    }
}
