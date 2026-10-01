package com.codex.blacklight.mixin.client;

import com.codex.blacklight.client.camera.PrototypeChaseCamera;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The source third-person camera has its own motion; vanilla gait bob would double it. */
@Mixin(GameRenderer.class)
public abstract class PrototypeCameraBobMixin {
    @Inject(method="bobView",at=@At("HEAD"),cancellable=true)
    private void blacklight$sourceCameraBob(PoseStack pose,float partial,CallbackInfo ci) {
        if(PrototypeChaseCamera.ownsView())ci.cancel();
    }
}
