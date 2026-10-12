package com.codex.blacklight.mixin.client;

import com.codex.blacklight.client.ClientTargetOutline;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class TargetLockGlowMixin {
    @Inject(method="shouldEntityAppearGlowing",at=@At("HEAD"),cancellable=true)
    private void blacklight$lockOutline(Entity entity,CallbackInfoReturnable<Boolean> result) {
        if(ClientTargetOutline.matches(entity))result.setReturnValue(true);
    }
}
