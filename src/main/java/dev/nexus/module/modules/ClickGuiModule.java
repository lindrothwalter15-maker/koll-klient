package dev.nexus.module.modules;

import dev.nexus.gui.ClickGuiScreen;
import dev.nexus.module.Category;
import dev.nexus.module.Module;
import dev.nexus.render.RenderUtil;
import dev.nexus.setting.BoolSetting;
import dev.nexus.setting.ColorSetting;
import dev.nexus.setting.ModeSetting;
import org.lwjgl.glfw.GLFW;

/** Holds GUI/theme settings. Enabling it (via its keybind) opens the ClickGUI. */
public final class ClickGuiModule extends Module {
    private final ModeSetting theme = add(new ModeSetting("Theme", "Color theme", "Violet",
            "Violet", "Ocean", "Crimson", "Emerald", "Sunset", "Rainbow", "Custom"));
    private final ColorSetting custom = add(new ColorSetting("Custom", "Custom accent color (click = rainbow)", 0xFF8B5CF6));
    private final BoolSetting blur = add(new BoolSetting("Blur", "Blur the game behind the menu", true));

    public ClickGuiModule() {
        super("ClickGUI", "Opens the module menu", Category.RENDER);
        setKeyBind(GLFW.GLFW_KEY_RIGHT_SHIFT);
        custom.visibleWhen(() -> theme.is("Custom"));
    }

    public int accent() {
        return switch (theme.get()) {
            case "Ocean" -> 0xFF38BDF8;
            case "Crimson" -> 0xFFF43F5E;
            case "Emerald" -> 0xFF34D399;
            case "Sunset" -> 0xFFFB923C;
            case "Rainbow" -> RenderUtil.rainbow(0);
            case "Custom" -> custom.color();
            default -> 0xFF8B5CF6;
        };
    }

    /** Darker companion color used for gradients. */
    public int accent2() {
        return switch (theme.get()) {
            case "Ocean" -> 0xFF2563EB;
            case "Crimson" -> 0xFFBE123C;
            case "Emerald" -> 0xFF059669;
            case "Sunset" -> 0xFFE11D48;
            case "Rainbow" -> RenderUtil.rainbow(4);
            case "Custom" -> RenderUtil.lerp(custom.color(), 0xFF000000, 0.35f);
            default -> 0xFF5B3FD1;
        };
    }

    /** Accent for list entry i: a gradient from accent to accent2 (rainbow cycles). */
    public int accentAt(int i) {
        if (theme.is("Rainbow") || (theme.is("Custom") && custom.isRainbow())) return RenderUtil.rainbow(i);
        return RenderUtil.lerp(accent(), accent2(), Math.min(1f, i / 8f));
    }

    public boolean blur() { return blur.get(); }

    @Override
    protected void onEnable() {
        if (mc.player != null) mc.setScreen(new ClickGuiScreen());
        setEnabled(false);
    }
}
