package dev.nexus.config;

import com.google.gson.*;
import dev.nexus.NexusClient;
import dev.nexus.module.Module;
import dev.nexus.setting.Setting;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

/**
 * JSON persistence with multiple profiles.
 * Layout: config/nexus/profiles/NAME.json and config/nexus/client.json (last profile).
 * Writes go to a temp file first and are moved into place, so a crash can't corrupt a profile.
 */
public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path root = FabricLoader.getInstance().getConfigDir().resolve("nexus");
    private final Path profiles = root.resolve("profiles");
    private final Path clientFile = root.resolve("client.json");
    private String current = "default";
    private final ExecutorService io = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Nexus-IO");
        t.setDaemon(true);
        return t;
    });

    public String currentProfile() { return current; }

    public void init() {
        try { Files.createDirectories(profiles); } catch (IOException e) { NexusClient.LOGGER.error("Cannot create config dir", e); }
        if (Files.exists(clientFile)) {
            try (Reader r = Files.newBufferedReader(clientFile, StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                if (o.has("profile")) current = sanitize(o.get("profile").getAsString());
            } catch (Exception e) { NexusClient.LOGGER.warn("client.json unreadable, using default profile", e); }
        }
        load(current);
    }

    public List<String> listProfiles() {
        List<String> out = new ArrayList<>();
        try (Stream<Path> s = Files.list(profiles)) {
            s.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".json"))
             .forEach(n -> out.add(n.substring(0, n.length() - 5)));
        } catch (IOException ignored) {}
        return out;
    }

    public boolean load(String name) {
        name = sanitize(name);
        Path file = profiles.resolve(name + ".json");
        if (!Files.exists(file)) { current = name; return false; }
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject modules = JsonParser.parseReader(r).getAsJsonObject().getAsJsonObject("modules");
            if (modules != null) {
                for (Module m : NexusClient.MODULES.all()) {
                    JsonElement el = modules.get(m.getName());
                    if (el != null && el.isJsonObject()) applyModule(m, el.getAsJsonObject());
                }
            }
            current = name;
            return true;
        } catch (Exception e) {
            NexusClient.LOGGER.error("Failed to load profile {}", name, e);
            return false;
        }
    }

    public boolean save(String name) {
        name = sanitize(name);
        JsonObject modules = new JsonObject();
        for (Module m : NexusClient.MODULES.all()) modules.add(m.getName(), writeModule(m));
        JsonObject out = new JsonObject();
        out.addProperty("version", 1);
        out.add("modules", modules);
        try {
            Files.createDirectories(profiles);
            writeAtomic(profiles.resolve(name + ".json"), GSON.toJson(out));
            current = name;
            JsonObject client = new JsonObject();
            client.addProperty("profile", current);
            writeAtomic(clientFile, GSON.toJson(client));
            return true;
        } catch (IOException e) {
            NexusClient.LOGGER.error("Failed to save profile {}", name, e);
            return false;
        }
    }

    /** Builds the JSON on the calling thread, writes it on a background thread (no main-thread disk I/O). */
    public void saveAsync(String name) {
        final String n = sanitize(name);
        JsonObject modules = new JsonObject();
        for (Module m : NexusClient.MODULES.all()) modules.add(m.getName(), writeModule(m));
        JsonObject out = new JsonObject();
        out.addProperty("version", 1);
        out.add("modules", modules);
        final String json = GSON.toJson(out);
        current = n;
        io.execute(() -> {
            try {
                Files.createDirectories(profiles);
                writeAtomic(profiles.resolve(n + ".json"), json);
            } catch (IOException e) {
                NexusClient.LOGGER.error("Async save failed for profile {}", n, e);
            }
        });
    }

    public boolean delete(String name) {
        name = sanitize(name);
        if (name.equals("default")) return false;
        try { return Files.deleteIfExists(profiles.resolve(name + ".json")); }
        catch (IOException e) { return false; }
    }

    private JsonObject writeModule(Module m) {
        JsonObject o = new JsonObject();
        o.addProperty("enabled", m.isEnabled());
        o.addProperty("bind", m.getKeyBind());
        JsonObject s = new JsonObject();
        for (Setting<?> st : m.getSettings()) s.add(st.getName(), st.toJson());
        o.add("settings", s);
        return o;
    }

    private void applyModule(Module m, JsonObject o) {
        if (o.has("bind") && o.get("bind").isJsonPrimitive()) m.setKeyBind(o.get("bind").getAsInt());
        if (o.has("settings") && o.get("settings").isJsonObject()) {
            JsonObject s = o.getAsJsonObject("settings");
            for (Setting<?> st : m.getSettings()) if (s.has(st.getName())) st.fromJson(s.get(st.getName()));
        }
        if (o.has("enabled") && o.get("enabled").isJsonPrimitive()) m.setEnabled(o.get("enabled").getAsBoolean());
    }

    private static void writeAtomic(Path target, String content) throws IOException {
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) { w.write(content); }
        try { Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException e) { Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING); }
    }

    /** Prevents path traversal and odd filenames in profile names. */
    private static String sanitize(String name) {
        String s = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_\\-]", "");
        return s.isEmpty() ? "default" : s;
    }
}
