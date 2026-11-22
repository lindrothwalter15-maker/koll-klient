package dev.nexus.mixin;

import dev.nexus.NexusClient;
import dev.nexus.event.AttackEvent;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fires an AttackEvent right before every attack, whoever started it (you, KillAura, TriggerBot). */
@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {
    // require = 0: if the target method ever changes, the game still starts and the hook is just skipped.
    @Inject(method = "attackEntity", at = @At("HEAD"), require = 0)
    private void nexus$onAttack(PlayerEntity player, Entity target, CallbackInfo ci) {
        AttackEvent e = AttackEvent.INSTANCE;
        e.target = target;
        NexusClient.EVENTS.post(e);
    }
}
