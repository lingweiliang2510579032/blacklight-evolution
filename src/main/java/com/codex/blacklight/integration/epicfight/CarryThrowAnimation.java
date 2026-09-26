package com.codex.blacklight.integration.epicfight;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.*;
import yesman.epicfight.api.animation.property.AnimationProperty.ActionAnimationProperty;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.utils.datastructure.ParameterizedHashMap;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/** Throwing retains the runner's physical travel; the clip only poses the body. */
public final class CarryThrowAnimation extends ActionAnimation {
    public CarryThrowAnimation(AnimationAccessor<? extends ActionAnimation> accessor){
        super(.06F,accessor,Armatures.BIPED);
        addProperty(ActionAnimationProperty.STOP_MOVEMENT,false);addProperty(ActionAnimationProperty.MOVE_ON_LINK,false);
    }
    @Override public ParameterizedHashMap<EntityState.StateFactor<?>> getStatesMap(LivingEntityPatch<?> patch,float time){
        var states=super.getStatesMap(patch,time);states.put(EntityState.MOVEMENT_LOCKED,false);return states;
    }
    @Override protected boolean validateMovement(LivingEntityPatch<?> patch,AssetAccessor<? extends DynamicAnimation> animation){return false;}
}
