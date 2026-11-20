package dev.nexus.event;

/** Posted once per client tick (end of tick). Singleton to avoid allocations. */
public final class TickEvent {
    public static final TickEvent INSTANCE = new TickEvent();
    private TickEvent() {}
}
