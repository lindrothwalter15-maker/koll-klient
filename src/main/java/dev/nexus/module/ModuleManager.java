package dev.nexus.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ModuleManager {
    private final Map<String, Module> byName = new LinkedHashMap<>();
    private final List<Module> all = new ArrayList<>();

    public void register(Module module) {
        String key = module.getName().toLowerCase(Locale.ROOT);
        if (byName.putIfAbsent(key, module) != null)
            throw new IllegalStateException("Duplicate module: " + module.getName());
        all.add(module);
    }

    public List<Module> all() { return Collections.unmodifiableList(all); }

    public Module get(String name) { return byName.get(name.toLowerCase(Locale.ROOT)); }

    public List<Module> inCategory(Category c) {
        List<Module> out = new ArrayList<>();
        for (Module m : all) if (m.getCategory() == c) out.add(m);
        return out;
    }

    /** Case-insensitive substring search, used by the ClickGUI search box. */
    public List<Module> search(String query) {
        String q = query.toLowerCase(Locale.ROOT).trim();
        List<Module> out = new ArrayList<>();
        for (Module m : all) if (q.isEmpty() || m.getName().toLowerCase(Locale.ROOT).contains(q)) out.add(m);
        return out;
    }
}
