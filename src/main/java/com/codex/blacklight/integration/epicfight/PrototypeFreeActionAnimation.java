package com.codex.blacklight.integration.epicfight;

import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.property.AnimationProperty.ActionAnimationProperty;
import yesman.epicfight.api.animation.types.ActionAnimation;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.datastructure.ParameterizedHashMap;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/** Authored traversal pose; the existing movement system owns the actual dash. */
public final class PrototypeFreeActionAnimation extends ActionAnimation {
    public PrototypeFreeActionAnimation(float transitionTime,
            AnimationAccessor<? extends ActionAnimation> accessor,
            AssetAccessor<? extends Armature> armature) {
        super(transitionTime, accessor, armature);
        addProperty(ActionAnimationProperty.MOVE_VERTICAL, false);
        addProperty(ActionAnimationProperty.MOVE_ON_LINK, false);
    }

    @Override
    public ParameterizedHashMap<EntityState.StateFactor<?>> getStatesMap(
            LivingEntityPatch<?> entityPatch, float time) {
        var states = super.getStatesMap(entityPatch, time);
        states.put(EntityState.MOVEMENT_LOCKED, false);
        states.put(EntityState.UPDATE_LIVING_MOTION, true);
        return states;
    }
}
