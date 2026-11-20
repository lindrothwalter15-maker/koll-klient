package dev.nexus;

import dev.nexus.config.ConfigManager;
import dev.nexus.event.EventBus;
import dev.nexus.event.TickEvent;
import dev.nexus.module.Module;
import dev.nexus.module.ModuleManager;
import dev.nexus.module.modules.SprintModule;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NexusClient implements ClientModInitializer {
    public static final String NAME = "Nexus";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);
    public static final EventBus EVENTS = new EventBus();
    public static final ModuleManager MODULES = new ModuleManager();
    public static final ConfigManager CONFIG = new ConfigManager();

    /** Tracks previous key state so binds fire once per press, with no allocation. */
    private static final boolean[] keyDown = new boolean[GLFW.GLFW_KEY_LAST + 1];

    @Override
    public void onInitializeClient() {
        MODULES.register(new SprintModule());
        // Further modules (combat, HUD, ...) are registered here in later stages.

        CONFIG.init();

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> CONFIG.save(CONFIG.currentProfile()));
        LOGGER.info("{} initialised with {} modules", NAME, MODULES.all().size());
    }

    private void onTick(MinecraftClient mc) {
        if (mc.getWindow() == null) return;
        // Key binds only fire in-world with no screen open.
        if (mc.currentScreen == null && mc.player != null) {
            // NOTE: Window handle accessor may differ in 1.21.11 (getHandle). Verify on compile.
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
