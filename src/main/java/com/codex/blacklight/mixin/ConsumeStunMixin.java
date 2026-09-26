package com.codex.blacklight.mixin;

import com.codex.blacklight.combat.ConsumeSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.damagesource.StunType;

/** Damage still applies; a confirmed absorption cannot be replaced by a stun animation. */
@Mixin(value=LivingEntityPatch.class,remap=false)
public abstract class ConsumeStunMixin {
    @Inject(method="applyStun",at=@At("HEAD"),cancellable=true)
    private void blacklight$consumeStun(StunType type,float duration,CallbackInfoReturnable<Boolean> cir) {
        if(ConsumeSystem.committed(((LivingEntityPatch<?>)(Object)this).getOriginal()))cir.setReturnValue(false);
    }
}
