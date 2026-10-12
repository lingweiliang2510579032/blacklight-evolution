package com.codex.blacklight.mixin.client;

import com.codex.blacklight.registry.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Captures land traversal input while leaving vanilla swimming in control of fluids. */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends Input {
    @Inject(method = "tick", at = @At("TAIL"))
    private void blacklight$captureChargedJump(boolean isSneaking,
                                                float sneakingSpeedMultiplier,
                                                CallbackInfo callback) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null
                || minecraft.player.getAbilities().flying
                || minecraft.player.isPassenger()
                || minecraft.player.isInWater() && !com.codex.blacklight.movement.WaterRunning.onSurface(minecraft.player)
                || minecraft.player.isInLava()) return;
        if (com.codex.blacklight.registry.ModAttachments.getData(minecraft.player, ModAttachments.PLAYER_STATE).isPrototypeMode()) {
            // ClientMovementEvents reads the physical key directly.  Clearing only the
            // vanilla Input flag prevents the immediate 0.42 jump and its server packet.
            // In water/lava the same flag supplies buoyancy, so it must stay untouched.
            this.jumping = false;
        }
    }
}
