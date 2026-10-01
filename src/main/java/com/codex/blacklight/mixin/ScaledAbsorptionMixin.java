package com.codex.blacklight.mixin;

import com.codex.blacklight.power.CombatScale;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets="net.minecraft.world.effect.AbsorptionMobEffect")
public abstract class ScaledAbsorptionMixin {
    @Inject(method="onEffectStarted",at=@At("TAIL"))
    private void blacklight$scaledAbsorption(LivingEntity entity,int amplifier,CallbackInfo callback){
        if(!entity.level().isClientSide)
            entity.setAbsorptionAmount(Math.max(entity.getAbsorptionAmount(),4F*(amplifier+1)*CombatScale.units(entity)));
    }
}
