package com.codex.blacklight.integration.epicfight;

import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.animation.types.ActionAnimation;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/** Blender-authored right-claw plant, held and reversed at server-owned timing markers. */
public final class GroundSpikeActionAnimation extends ActionAnimation {
    public static final float TOTAL_TIME = 2.20F;
    public static final float PLANT_TIME = 0.55F;
    public static final float WITHDRAW_START = 1.60F;

    public GroundSpikeActionAnimation(
            float transitionTime,
            AnimationAccessor<? extends ActionAnimation> accessor,
            AssetAccessor<? extends Armature> armature) {
        super(transitionTime, accessor, armature);
    }

    @Override
    public Pose getPoseByTime(LivingEntityPatch<?> entityPatch, float time, float partialTicks) {
        Pose pose = getRawPose(Math.clamp(time, 0.0F, TOTAL_TIME));
        modifyPose(this, pose, entityPatch, time, partialTicks);
        return pose;
    }

    @Override
    public float getPlaySpeed(LivingEntityPatch<?> entityPatch, DynamicAnimation animation) {
        return 1.0F;
    }

    @Override
    public float getTotalTime() {
        return TOTAL_TIME;
    }
}
