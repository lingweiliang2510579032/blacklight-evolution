package com.codex.blacklight.mixin.client;

import com.codex.blacklight.mode.PrototypeModeSystem;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;

/** Reject automatic equipment/key mode changes before they alter perspective or HUD. */
@Mixin(value=LocalPlayerPatch.class,remap=false)
public abstract class PrototypePlayerModeMixin {
    @Inject(method="toEpicFightMode",at=@At("HEAD"),cancellable=true)
    private void blacklight$requirePrototype(boolean sync, CallbackInfo callback) {
        var player=Minecraft.getInstance().player;
        if(player!=null && !PrototypeModeSystem.isActive(player)) callback.cancel();
    }
}
