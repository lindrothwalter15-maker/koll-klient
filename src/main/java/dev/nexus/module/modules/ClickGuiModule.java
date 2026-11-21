package dev.nexus.module.modules;

import dev.nexus.gui.ClickGuiScreen;
import dev.nexus.module.Category;
import dev.nexus.module.Module;
import dev.nexus.render.RenderUtil;
import dev.nexus.setting.BoolSetting;
import dev.nexus.setting.ColorSetting;
import org.lwjgl.glfw.GLFW;

/** Holds GUI/theme settings. Enabling it (via its keybind) opens the ClickGUI. */
public final class ClickGuiModule extends Module {
    private final ColorSetting accent = add(new ColorSetting("Accent", "Theme color (click = rainbow)", 0xFF7C5CFF));
    private final BoolSetting blur = add(new BoolSetting("Blur", "Blur the game behind the menu", true));

    public ClickGuiModule() {
        super("ClickGUI", "Opens the module menu", Category.RENDER);
        setKeyBind(GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    public int accent() { return accent.color(); }

    /** Accent color for list entry i (animated when rainbow is on). */
    public int accentAt(int i) { return accent.isRainbow() ? RenderUtil.rainbow(i) : accent.color(); }

    public boolean blur() { return blur.get(); }

    @Override
    protected void onEnable() {
        if (mc.player != null) mc.setScreen(new ClickGuiScreen());
        setEnabled(false);
    }
}
