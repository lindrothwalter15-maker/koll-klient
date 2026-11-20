package dev.nexus.module;

import dev.nexus.NexusClient;
import dev.nexus.setting.Setting;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Base class for every module. Event listeners declared through {@link #listen}
 * are registered on enable and fully removed on disable.
 */
public abstract class Module {
    protected static final MinecraftClient mc = MinecraftClient.getInstance();

    private record Pending<T>(Class<T> type, Consumer<T> handler) {}

    private final String name;
    private final String description;
    private final Category category;
    private final List<Setting<?>> settings = new ArrayList<>();
    private final List<Pending<?>> pending = new ArrayList<>();
    private boolean enabled;
    /** GLFW key code, or -1 for unbound. */
    private int keyBind = -1;

    protected Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public List<Setting<?>> getSettings() { return Collections.unmodifiableList(settings); }
    public boolean isEnabled() { return enabled; }
    public int getKeyBind() { return keyBind; }
    public void setKeyBind(int key) { this.keyBind = key; }

    protected <S extends Setting<?>> S add(S setting) { settings.add(setting); return setting; }

    protected <T> void listen(Class<T> type, Consumer<T> handler) { pending.add(new Pending<>(type, handler)); }

    public void toggle() { setEnabled(!enabled); }

    public void setEnabled(boolean value) {
        if (value == enabled) return;
        enabled = value;
        if (value) {
            for (Pending<?> p : pending) register(p);
            safe(this::onEnable);
        } else {
            NexusClient.EVENTS.unregisterAll(this);
            safe(this::onDisable);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void register(Pending<?> p) {
        NexusClient.EVENTS.register(this, (Class) p.type(), (Consumer) p.handler());
    }

    private void safe(Runnable r) {
        try { r.run(); } catch (Throwable t) { NexusClient.LOGGER.error("Module {} failed", name, t); }
    }

    /** Text appended in the ArrayList HUD (e.g. current mode). */
    public String getInfo() { return ""; }

    protected void onEnable() {}
    protected void onDisable() {}
}
