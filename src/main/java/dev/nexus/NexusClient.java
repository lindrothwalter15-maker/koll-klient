package dev.nexus;

import dev.nexus.config.ConfigManager;
import dev.nexus.event.EventBus;
import dev.nexus.event.Render2DEvent;
import dev.nexus.event.TickEvent;
import dev.nexus.module.Module;
import dev.nexus.module.ModuleManager;
import dev.nexus.module.modules.ClickGuiModule;
import dev.nexus.module.modules.HudModule;
import dev.nexus.module.modules.SprintModule;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NexusClient implements ClientModInitializer {
    public static final String NAME = "Nexus";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);
    public static final EventBus EVENTS = new EventBus();
    public static final ModuleManager MODULES = new ModuleManager();
    public static final ConfigManager CONFIG = new ConfigManager();
    public static final ClickGuiModule GUI_CFG = new ClickGuiModule();

    /** Tracks previous key state so binds fire once per press, with no allocation. */
    private static final boolean[] keyDown = new boolean[GLFW.GLFW_KEY_LAST + 1];

    @Override
    public void onInitializeClient() {
        HudModule hud = new HudModule();
        MODULES.register(GUI_CFG);
        MODULES.register(hud);
        MODULES.register(new SprintModule());
        // Combat modules are registered here in later stages.

        hud.setEnabled(true); // default; a saved profile may override this
        CONFIG.init();

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> CONFIG.save(CONFIG.currentProfile()));

        // NOTE: HUD API (Fabric API 0.116+). If this fails to compile, check VanillaHudElements names.
        HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, Identifier.of("nexus", "hud"),
                (context, tickCounter) -> {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc.player == null || mc.options.hudHidden) return;
                    Render2DEvent e = Render2DEvent.INSTANCE;
                    e.context = context;
                    e.width = context.getScaledWindowWidth();
                    e.height = context.getScaledWindowHeight();
                    EVENTS.post(e);
                });

        LOGGER.info("{} initialised with {} modules", NAME, MODULES.all().size());
    }

    private void onTick(MinecraftClient mc) {
        if (mc.getWindow() == null) return;
        if (mc.currentScreen == null && mc.player != null) {
            long handle = mc.getWindow().getHandle();
            for (Module m : MODULES.all()) {
                int key = m.getKeyBind();
                if (key < 0 || key > GLFW.GLFW_KEY_LAST) continue;
                boolean down = GLFW.glfwGetKey(handle, key) == GLFW.GLFW_PRESS;
                if (down && !keyDown[key]) m.toggle();
                keyDown[key] = down;
            }
        }
        EVENTS.post(TickEvent.INSTANCE);
    }
}
