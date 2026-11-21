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
import net.minecraft.util.Formatting;
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
 * the Screen input signatures (changed in 1.21.9+). Scrolling uses mouseScrolled.
 * Left click: toggle module. Right click: expand settings. Drag a header: move the panel.
 * Mouse wheel over a panel: scroll. Type: search. Esc / Right Shift: close.
 */
public final class ClickGuiScreen extends Screen {
    private static final int PW = 126, IP = 4, RW = PW - 2 * IP, HEADER = 24, ROW = 18, RGAP = 2, SROW = 16;
    private static final int GAP = 10, PAD = 7, TOP = 44;
    private static final int PANEL_BG = 0xF0101018, HEAD_BG = 0xFF161622, ROW_BG = 0xFF1B1B2A, SET_BG = 0xFF131320;
    private static final int BOX = 0xFF2E2E44, TEXT = 0xFFF2F2FC, DIM = 0xFF8E8EA8, OFF_TEXT = 0xFFB6B6CC;

    /** Panel positions survive closing and reopening the menu (until the game is closed). */
    private static final Map<Category, int[]> POS = new EnumMap<>(Category.class);

    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final Map<Module, Float> progress = new IdentityHashMap<>();
    private final Map<Object, Float> enAnim = new IdentityHashMap<>();
    private final Map<Object, Float> hvAnim = new IdentityHashMap<>();
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

    private float anim(Map<Object, Float> map, Object key, boolean on, float speed) {
        float cur = map.getOrDefault(key, on ? 1f : 0f);
        cur = RenderUtil.approach(cur, on ? 1f : 0f, dt * speed);
        map.put(key, cur);
        return cur;
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

        ClickGuiModule cfg = NexusClient.GUI_CFG;
        int accent = cfg.accent();
        if (cfg.blur()) super.render(ctx, mx, my, delta);
        else ctx.fill(0, 0, width, height, RenderUtil.alpha(0xFF000000, 0.45f * open));
        ctx.fillGradient(0, 0, width, height, RenderUtil.alpha(accent, 0.12f * open), RenderUtil.alpha(0xFF000000, 0.40f * open));

        tooltip = null;
        bounds.clear();
        drawChrome(ctx, accent);

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

        int slide = Math.round((1f - open) * -12f);
        for (Category c : all) {
            int[] p = POS.get(c);
            p[0] = Math.max(0, Math.min(Math.max(0, width - PW), p[0]));
            p[1] = Math.max(0, Math.min(Math.max(0, height - HEADER), p[1]));
            List<Module> mods = modulesFor(c);
            if (mods.isEmpty()) continue;
            drawPanel(ctx, c, mods, p[0], p[1] + slide, mx, my, lClick, rClick, accent);
        }

        if (tooltip != null) {
            int tw = RenderUtil.width(tooltip);
            RenderUtil.roundRect(ctx, mx + 8, my + 8, mx + 18 + tw, my + 22, 3, 0xF0101018);
            RenderUtil.roundRect(ctx, mx + 8, my + 8, mx + 10, my + 22, 1, accent);
            RenderUtil.text(ctx, tooltip, mx + 13, my + 12, TEXT);
        }
    }

    /** Logo, search bar and hint line. */
    private void drawChrome(DrawContext ctx, int accent) {
        float a = open;
        ctx.drawText(mc.textRenderer, Text.literal("NEXUS").formatted(Formatting.BOLD), 12, 10,
                RenderUtil.alpha(accent, a), true);
        RenderUtil.text(ctx, "client  " + NexusClient.CONFIG.currentProfile(), 12, 22, RenderUtil.alpha(DIM, a));

        int sw = 200, sx = (width - sw) / 2, sy = 10;
        RenderUtil.roundRect(ctx, sx - 1, sy - 1, sx + sw + 1, sy + 21, 6, RenderUtil.alpha(accent, 0.35f * a));
        RenderUtil.roundRect(ctx, sx, sy, sx + sw, sy + 20, 5, RenderUtil.alpha(HEAD_BG, 0.96f * a));
        boolean caret = (System.currentTimeMillis() / 500L) % 2 == 0;
        String shown = search.isEmpty() ? "Search modules..." : search + (caret ? "_" : "");
        RenderUtil.text(ctx, shown, sx + 8, sy + 6, RenderUtil.alpha(search.isEmpty() ? DIM : TEXT, a));

        String hint = "Right-click: settings   Drag header: move   Scroll: panel   Esc: close";
        RenderUtil.text(ctx, hint, (width - RenderUtil.width(hint)) / 2, height - 12, RenderUtil.alpha(DIM, 0.8f * a));
    }

    private int contentHeight(List<Module> mods) {
        int h = 0;
        for (Module m : mods) {
            h += ROW + RGAP;
            float p = progress.getOrDefault(m, 0f);
            if (p > 0.001f) {
                int vis = 0;
                for (Setting<?> s : m.getSettings()) if (s.isVisible()) vis++;
                h += Math.round((vis + 1) * SROW * p) + 2;
            }
        }
        return h;
    }

    private void drawPanel(DrawContext ctx, Category cat, List<Module> mods, int x, int y,
                           int mx, int my, boolean lClick, boolean rClick, int accent) {
        float a = open;
        int accent2 = NexusClient.GUI_CFG.accent2();

        int contentH = contentHeight(mods);
        int top = y + HEADER + 3;
        int maxVisible = Math.max(2 * ROW, height - top - 14);
        int visibleH = Math.min(contentH, maxVisible);
        int maxScroll = Math.max(0, contentH - visibleH);
        float[] sc = scroll.computeIfAbsent(cat, k -> new float[2]);
        sc[0] = Math.max(0f, Math.min((float) maxScroll, sc[0]));
        sc[1] += (sc[0] - sc[1]) * Math.min(1f, dt * 14f);
        if (Math.abs(sc[0] - sc[1]) < 0.3f) sc[1] = sc[0];
        int off = Math.round(sc[1]);
        int bottom = top + visibleH;
        int boxBottom = bottom + 4;
        bounds.put(cat, new int[] { x, y, PW, boxBottom - y });

        // soft shadow, body, header
        RenderUtil.roundRect(ctx, x - 3, y - 2, x + PW + 3, boxBottom + 4, 9, RenderUtil.alpha(0xFF000000, 0.14f * a));
        RenderUtil.roundRect(ctx, x - 1, y - 1, x + PW + 1, boxBottom + 2, 7, RenderUtil.alpha(0xFF000000, 0.22f * a));
        RenderUtil.roundRect(ctx, x, y, x + PW, boxBottom, 6, RenderUtil.alpha(PANEL_BG, a));
        RenderUtil.roundRect(ctx, x, y, x + PW, y + HEADER, 6, RenderUtil.alpha(HEAD_BG, a));
        ctx.fillGradient(x + 3, y + 2, x + PW - 3, y + HEADER - 2,
                RenderUtil.alpha(accent2, 0.38f * a), RenderUtil.alpha(HEAD_BG, 0.0f));
        ctx.fill(x + 8, y + HEADER - 1, x + PW - 8, y + HEADER, RenderUtil.alpha(accent, 0.9f * a));

        // category badge + title
        RenderUtil.roundRect(ctx, x + PAD - 1, y + 6, x + PAD + 11, y + 18, 3, RenderUtil.alpha(accent, 0.9f * a));
        String letter = cat.getDisplayName().substring(0, 1);
        RenderUtil.text(ctx, letter, x + PAD + 5 - RenderUtil.width(letter) / 2, y + 8, RenderUtil.alpha(0xFF101018, a));
        RenderUtil.text(ctx, cat.getDisplayName(), x + PAD + 16, y + 8, RenderUtil.alpha(TEXT, a));
        String cnt = Integer.toString(mods.size());
        RenderUtil.text(ctx, cnt, x + PW - PAD - RenderUtil.width(cnt), y + 8, RenderUtil.alpha(DIM, a));

        boolean hoverHeader = mx >= x && mx < x + PW && my >= y && my < y + HEADER;
        if (hoverHeader) {
            tooltip = "Drag to move";
            if (lClick && dragging == null) { dragging = cat; dragDx = mx - x; dragDy = my - y; }
        }

        clipTop = top;
        clipBottom = bottom;
        int rx = x + IP;

        ctx.enableScissor(x, top, x + PW, bottom);
        int cy = top - off;
        for (Module m : mods) {
            boolean hover = mx >= rx && mx < rx + RW && my >= cy && my < cy + ROW && my >= top && my < bottom;
            float en = anim(enAnim, m, m.isEnabled(), 10f);
            float hv = anim(hvAnim, m, hover, 14f);

            RenderUtil.roundRect(ctx, rx, cy, rx + RW, cy + ROW, 4, RenderUtil.alpha(ROW_BG, a));
            if (en > 0.01f) {
                RenderUtil.roundRect(ctx, rx, cy, rx + RW, cy + ROW, 4, RenderUtil.alpha(accent, 0.26f * en * a));
                RenderUtil.roundRect(ctx, rx + 1, cy + 4, rx + 3, cy + ROW - 4, 1, RenderUtil.alpha(accent, en * a));
            }
            if (hv > 0.01f) RenderUtil.roundRect(ctx, rx, cy, rx + RW, cy + ROW, 4, RenderUtil.alpha(0x30FFFFFF, hv * a));

            if (hover) {
                tooltip = m.getDescription();
                if (lClick && !(m instanceof ClickGuiModule)) m.toggle();
                if (rClick) {
                    if (!expanded.remove(m)) expanded.add(m);
                }
            }
            RenderUtil.text(ctx, m.getName(), rx + 8, cy + 5, RenderUtil.alpha(RenderUtil.lerp(OFF_TEXT, TEXT, en), a));
            String sign = expanded.contains(m) ? "-" : "+";
            RenderUtil.text(ctx, sign, rx + RW - 8 - RenderUtil.width(sign), cy + 5, RenderUtil.alpha(DIM, a));
            cy += ROW + RGAP;

            float p = RenderUtil.approach(progress.getOrDefault(m, 0f), expanded.contains(m) ? 1f : 0f, dt * 8f);
            progress.put(m, p);
            if (p > 0.001f) cy += drawSettings(ctx, m, rx, cy, p, mx, my, lClick, accent) + 2;
        }
        ctx.disableScissor();

        if (maxScroll > 0) {
            int barH = Math.max(14, visibleH * visibleH / contentH);
            int barY = top + Math.round((visibleH - barH) * (sc[1] / maxScroll));
            RenderUtil.roundRect(ctx, x + PW - 3, barY, x + PW - 1, barY + barH, 1, RenderUtil.alpha(accent, 0.85f * a));
        }
    }

    private int drawSettings(DrawContext ctx, Module m, int x, int y, float p, int mx, int my, boolean lClick, int accent) {
        float a = open;
        List<Setting<?>> vis = new ArrayList<>();
        for (Setting<?> s : m.getSettings()) if (s.isVisible()) vis.add(s);
        int full = (vis.size() + 1) * SROW;
        int h = Math.round(full * p);
        boolean inClip = my >= clipTop && my < clipBottom;

        ctx.enableScissor(x, y, x + RW, y + h);
        RenderUtil.roundRect(ctx, x, y, x + RW, y + full, 4, RenderUtil.alpha(SET_BG, a));
        int ry = y;
        for (Setting<?> s : vis) {
            boolean hov = inClip && mx >= x && mx < x + RW && my >= ry && my < ry + SROW && my < y + h;
            if (hov) {
                ctx.fill(x + 2, ry, x + RW - 2, ry + SROW, RenderUtil.alpha(0x22FFFFFF, a));
                tooltip = s.getDescription();
            }
            int tcol = RenderUtil.alpha(TEXT, a);
            if (s instanceof BoolSetting b) {
                RenderUtil.text(ctx, b.getName(), x + PAD, ry + 4, tcol);
                float t = anim(enAnim, s, b.get(), 14f);
                int tx = x + RW - PAD - 20, ty = ry + 3;
                RenderUtil.roundRect(ctx, tx, ty, tx + 20, ty + 10, 5, RenderUtil.alpha(RenderUtil.lerp(BOX, accent, t), a));
                int kx = tx + 1 + Math.round(t * 10f);
                RenderUtil.roundRect(ctx, kx, ty + 1, kx + 8, ty + 9, 4, RenderUtil.alpha(0xFFFFFFFF, a));
                if (hov && lClick) b.toggle();
            } else if (s instanceof NumberSetting n) {
                RenderUtil.text(ctx, n.getName(), x + PAD, ry + 1, tcol);
                String val = fmt(n);
                RenderUtil.text(ctx, val, x + RW - PAD - RenderUtil.width(val), ry + 1, RenderUtil.alpha(DIM, a));
                int sx = x + PAD, sw = RW - 2 * PAD, by = ry + SROW - 5;
                double t = (n.get() - n.getMin()) / (n.getMax() - n.getMin());
                int fillW = (int) (sw * t);
                RenderUtil.roundRect(ctx, sx, by, sx + sw, by + 3, 1, RenderUtil.alpha(BOX, a));
                RenderUtil.roundRect(ctx, sx, by, sx + Math.max(2, fillW), by + 3, 1, RenderUtil.alpha(accent, a));
                RenderUtil.roundRect(ctx, sx + fillW - 2, by - 1, sx + fillW + 3, by + 4, 2, RenderUtil.alpha(0xFFFFFFFF, a));
                if (hov && lClick) { slider = n; sliderX = sx; sliderW = sw; }
                if (slider == n) {
                    double nt = Math.max(0.0, Math.min(1.0, (mx - sliderX) / (double) sliderW));
                    n.set(n.getMin() + nt * (n.getMax() - n.getMin()));
                }
            } else if (s instanceof ModeSetting md) {
                RenderUtil.text(ctx, md.getName(), x + PAD, ry + 4, tcol);
                String v = md.get();
                int vw = RenderUtil.width(v);
                RenderUtil.roundRect(ctx, x + RW - PAD - vw - 8, ry + 2, x + RW - PAD, ry + SROW - 2, 3, RenderUtil.alpha(accent, 0.22f * a));
                RenderUtil.text(ctx, v, x + RW - PAD - vw - 4, ry + 4, RenderUtil.alpha(accent, a));
                if (hov && lClick) md.cycle();
            } else if (s instanceof ColorSetting cs) {
                RenderUtil.text(ctx, cs.getName() + (cs.isRainbow() ? " (rainbow)" : ""), x + PAD, ry + 4, tcol);
                int bx = x + RW - PAD - 12, by = ry + 3;
                RenderUtil.roundRect(ctx, bx, by, bx + 12, by + 10, 3, RenderUtil.alpha(cs.color(), a));
                if (hov && lClick) cs.setRainbow(!cs.isRainbow());
            }
            ry += SROW;
        }

        boolean hovBind = inClip && mx >= x && mx < x + RW && my >= ry && my < ry + SROW && my < y + h;
        if (hovBind) {
            ctx.fill(x + 2, ry, x + RW - 2, ry + SROW, RenderUtil.alpha(0x22FFFFFF, a));
            tooltip = "Click, then press a key. Backspace clears.";
            if (lClick) { listening = m; snapshotKeys(); }
        }
        RenderUtil.text(ctx, "Bind", x + PAD, ry + 4, RenderUtil.alpha(TEXT, a));
        String kn = listening == m ? "..." : keyName(m.getKeyBind());
        int kw = RenderUtil.width(kn);
        RenderUtil.roundRect(ctx, x + RW - PAD - kw - 8, ry + 2, x + RW - PAD, ry + SROW - 2, 3, RenderUtil.alpha(BOX, a));
        RenderUtil.text(ctx, kn, x + RW - PAD - kw - 4, ry + 4, RenderUtil.alpha(accent, a));
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
