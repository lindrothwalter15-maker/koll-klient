package dev.nexus.event;

import net.minecraft.entity.Entity;

/** Posted just before the client sends an attack on an entity. Reused singleton (no allocation). */
public final class AttackEvent {
    public static final AttackEvent INSTANCE = new AttackEvent();
    public Entity target;
    private AttackEvent() {}
}
