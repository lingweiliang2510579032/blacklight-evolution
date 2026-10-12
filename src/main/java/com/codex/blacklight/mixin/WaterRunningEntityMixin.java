package com.codex.blacklight.mixin;

import com.codex.blacklight.movement.WaterRunning;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class WaterRunningEntityMixin {
    @Inject(method = "canStandOnFluid", at = @At("HEAD"), cancellable = true)
    private void blacklight$runOnWater(FluidState fluid, CallbackInfoReturnable<Boolean> cir) {
        if ((Object)this instanceof Player player && fluid.is(FluidTags.WATER)
                && WaterRunning.canRun(player)) cir.setReturnValue(true);
    }
}
