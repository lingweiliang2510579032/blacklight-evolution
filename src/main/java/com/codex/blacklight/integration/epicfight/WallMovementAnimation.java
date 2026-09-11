package com.codex.blacklight.integration.epicfight;

import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.types.MovementAnimation;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/** Vertical/tangential wall speed controls cadence; ground gait yaw must not twist wall feet. */
public final class WallMovementAnimation extends MovementAnimation {
    private final double nominalSpeed;
    public WallMovementAnimation(boolean repeat, double nominalSpeed, AnimationAccessor<? extends MovementAnimation> accessor) {
        super(.10F,repeat,accessor,Armatures.BIPED);this.nominalSpeed=nominalSpeed;
    }
    @Override public float getPlaySpeed(LivingEntityPatch<?> patch, DynamicAnimation animation) {
        if(nominalSpeed<=0 || animation.isLinkAnimation())return 1;
        return (float)Math.clamp(patch.getOriginal().getDeltaMovement().length()/nominalSpeed,.55,1.8);
    }
}
