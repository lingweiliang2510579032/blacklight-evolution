package com.codex.blacklight.integration.epicfight;

import com.codex.blacklight.client.tuning.AnimationPoseSettings;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/** Static animation with an additive pose layer supplied by the in-game tuner. */
public final class TunableStaticAnimation extends StaticAnimation {
    private final AnimationPoseSettings.Action action;

    public TunableStaticAnimation(float transitionTime, boolean repeat,
                                  AnimationAccessor<? extends StaticAnimation> accessor,
                                  AssetAccessor<? extends Armature> armature,
                                  AnimationPoseSettings.Action action) {
        super(transitionTime, repeat, accessor, armature);
        this.action = action;
    }

    @Override
    public void modifyPose(DynamicAnimation animation, Pose pose,
                           LivingEntityPatch<?> entityPatch, float time, float partialTicks) {
        super.modifyPose(animation, pose, entityPatch, time, partialTicks);
        AnimationPoseSettings.apply(action, pose, time);
    }
}
