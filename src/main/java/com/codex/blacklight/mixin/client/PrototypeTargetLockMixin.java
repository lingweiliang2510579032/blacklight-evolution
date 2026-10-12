package com.codex.blacklight.mixin.client;

import com.codex.blacklight.client.camera.PrototypeChaseCamera;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Transfer vanilla mouse deltas to Blacklight's independent orbit controller. */
@Mixin(MouseHandler.class)
public abstract class PrototypeTargetLockMixin {
    @WrapOperation(method="turnPlayer",at=@At(value="INVOKE",
            target="Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void blacklight$sourceMouse(LocalPlayer player,double x,double y,Operation<Void> original){
        if(!PrototypeChaseCamera.mouse(x,y))original.call(player,x,y);
    }
}
