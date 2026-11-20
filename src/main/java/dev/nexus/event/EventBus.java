package dev.nexus.event;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Minimal typed event bus. Listeners are tied to an owner object so that all
 * of an owner's listeners can be removed at once (prevents leaks when a module
 * is disabled). Dispatch iterates a copy-on-write list: no allocation per post.
 */
public final class EventBus {
    private record Listener(Object owner, Consumer<Object> handler) {}

    private final Map<Class<?>, CopyOnWriteArrayList<Listener>> listeners = new IdentityHashMap<>();

    @SuppressWarnings("unchecked")
    public synchronized <T> void register(Object owner, Class<T> type, Consumer<T> handler) {
        listeners.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>())
                 .add(new Listener(owner, (Consumer<Object>) handler));
    }

    public synchronized void unregisterAll(Object owner) {
        for (CopyOnWriteArrayList<Listener> list : listeners.values()) {
            list.removeIf(l -> l.owner == owner);
        }
    }

    public <T> T post(T event) {
        CopyOnWriteArrayList<Listener> list = listeners.get(event.getClass());
        if (list == null) return event;
        for (int i = 0, n = list.size(); i < n; i++) {
            try {
                list.get(i).handler.accept(event);
            } catch (Throwable t) {
                // One broken module must never crash the game loop.
                dev.nexus.NexusClient.LOGGER.error("Event handler failed for {}", event.getClass().getSimpleName(), t);
            }
        }
        return event;
    }
}
