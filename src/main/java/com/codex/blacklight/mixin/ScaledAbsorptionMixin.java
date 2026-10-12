package com.codex.blacklight.mixin;

import com.codex.blacklight.power.CombatScale;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.world.effect.AbsoptionMobEffect.class)
public abstract class ScaledAbsorptionMixin {
    @Inject(method="addAttributeModifiers(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/ai/attributes/AttributeMap;I)V",at=@At("TAIL"))
    private void blacklight$scaledAbsorption(LivingEntity entity,net.minecraft.world.entity.ai.attributes.AttributeMap attributes,int amplifier,CallbackInfo callback){
        if(!entity.level().isClientSide)
            entity.setAbsorptionAmount(Math.max(entity.getAbsorptionAmount(),4F*(amplifier+1)*CombatScale.units(entity)));
    }
    @Redirect(method="removeAttributeModifiers",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;setAbsorptionAmount(F)V"))
    private void blacklight$removeScaledAbsorption(LivingEntity entity,float amount){
        float current=entity.getAbsorptionAmount();
        entity.setAbsorptionAmount(entity.level().isClientSide?amount:current+(amount-current)*CombatScale.units(entity));
    }
}
