package dev.nexus;

import dev.nexus.config.ConfigManager;
import dev.nexus.event.EventBus;
import dev.nexus.event.Render2DEvent;
import dev.nexus.event.TickEvent;
import dev.nexus.module.Module;
import dev.nexus.module.ModuleManager;
import dev.nexus.module.modules.AutoTotemModule;
import dev.nexus.module.modules.BowAimModule;
import dev.nexus.module.modules.ClickGuiModule;
import dev.nexus.module.modules.CombatBotModule;
import dev.nexus.module.modules.CrystalAuraModule;
import dev.nexus.module.modules.HudModule;
import dev.nexus.module.modules.KillAuraModule;
import dev.nexus.module.modules.*;
import dev.nexus.module.modules.SprintModule;
import dev.nexus.module.modules.TriggerBotModule;
import dev.nexus.module.modules.WTapModule;
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
        MODULES.register(new KillAuraModule());
        MODULES.register(new CrystalAuraModule());
        MODULES.register(new AutoTotemModule());
        MODULES.register(new BowAimModule());
        MODULES.register(new TriggerBotModule());
        MODULES.register(new WTapModule());
        MODULES.register(new CombatBotModule());
        MODULES.register(new SprintModule());
        MODULES.register(new GodModeModule());
        MODULES.register(new AimAssistModule());
        MODULES.register(new AutoPotModule());
        MODULES.register(new AutoGappleModule());
        MODULES.register(new MaceAuraModule());
        MODULES.register(new CartAuraModule());
        MODULES.register(new AutoShieldModule());
        MODULES.register(new StrafeModule());
        MODULES.register(new AutoCritModule());
        MODULES.register(new STapModule());
        MODULES.register(new AutoWebModule());
        MODULES.register(new AutoArmorModule());
        MODULES.register(new HitboxesModule());
        MODULES.register(new FastBowModule());
        MODULES.register(new AntiVoidModule());
        MODULES.register(new AutoMendModule());
        MODULES.register(new JesusModule());
        MODULES.register(new NoFallModule());
        MODULES.register(new FlightModule());
        MODULES.register(new SpeedModule());
        MODULES.register(new SpiderModule());
        MODULES.register(new FullBrightModule());
        MODULES.register(new GlowEspModule());
        MODULES.register(new AutoEatModule());
        MODULES.register(new AutoJumpModule());
        MODULES.register(new AntiAfkModule());
        MODULES.register(new AutoRespawnModule());
        MODULES.register(new ChestStealerModule());
        MODULES.register(new AutoToolModule());
        MODULES.register(new ScaffoldModule());
        MODULES.register(new SneakModule());
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
