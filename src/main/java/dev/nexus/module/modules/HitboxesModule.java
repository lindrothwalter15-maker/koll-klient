package dev.nexus.module.modules;

import dev.nexus.event.TickEvent;
import dev.nexus.module.Category;
import dev.nexus.module.Module;

import dev.nexus.setting.NumberSetting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;

/** Makes other players' hitboxes wider on your client, so they are easier to hit. */
public final class HitboxesModule extends Module {
    private final NumberSetting expand = add(new NumberSetting("Expand", "Extra width on each side (blocks)", 0.2, 0.05, 0.6, 0.05));

    public HitboxesModule() {
        super("Hitboxes", "Expands player hitboxes", Category.COMBAT);
        listen(TickEvent.class, e -> tick(true));
    }

    private void tick(boolean grow) {
        if (mc.world == null) return;
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player) continue;
            Box base = p.getDimensions(p.getPose()).getBoxAt(p.getX(), p.getY(), p.getZ());
            p.setBoundingBox(grow ? base.expand(expand.get(), 0.0, expand.get()) : base);
        }
    }

    @Override protected void onDisable() { tick(false); }
}
