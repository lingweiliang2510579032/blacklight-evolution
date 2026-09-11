package com.codex.blacklight.mixin.client;

import com.codex.blacklight.registry.ModAttachments;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Removes vanilla dirt sprint particles that look like positional afterimages at high speed. */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    @Inject(method = "canSpawnSprintParticle", at = @At("HEAD"), cancellable = true)
    private void blacklight$hideVanillaSprintTrail(CallbackInfoReturnable<Boolean> callback) {
        LocalPlayer player = (LocalPlayer)(Object)this;
        if (player.getData(ModAttachments.PLAYER_STATE).isPrototypeMode()) {
            callback.setReturnValue(false);
        }
    }
}
