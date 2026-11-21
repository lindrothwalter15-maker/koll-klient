package dev.nexus.event;

import net.minecraft.client.gui.DrawContext;

/** Posted every frame while the HUD is drawn. Reused singleton: no allocation per frame. */
public final class Render2DEvent {
    public static final Render2DEvent INSTANCE = new Render2DEvent();
    public DrawContext context;
    public int width;
    public int height;
    private Render2DEvent() {}
}
