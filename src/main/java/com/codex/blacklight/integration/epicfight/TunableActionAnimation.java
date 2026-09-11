package com.codex.blacklight.integration.epicfight;

import com.codex.blacklight.client.tuning.AnimationPoseSettings;
import java.util.function.Supplier;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.animation.types.ActionAnimation;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/** Action animation whose final playable motion is supplied by the in-game timeline. */
public final class TunableActionAnimation extends ActionAnimation {
    private final AnimationPoseSettings.Action action;
    private final Supplier<AnimationAccessor<? extends StaticAnimation>> motionSource;

    public TunableActionAnimation(float transitionTime,
                                  AnimationAccessor<? extends ActionAnimation> accessor,
                                  AssetAccessor<? extends Armature> armature,
                                  AnimationPoseSettings.Action action) {
        this(transitionTime, accessor, armature, action, null);
    }

    public TunableActionAnimation(float transitionTime,
                                  AnimationAccessor<? extends ActionAnimation> accessor,
                                  AssetAccessor<? extends Armature> armature,
                                  AnimationPoseSettings.Action action,
                                  Supplier<AnimationAccessor<? extends StaticAnimation>> motionSource) {
        super(transitionTime, accessor, armature);
        this.action = action;
        this.motionSource = motionSource;
    }

    @Override
    public void loadAnimation() {
        super.loadAnimation();
        if (motionSource != null) {
            AnimationAccessor<? extends StaticAnimation> source = motionSource.get();
            if (source != null) {
                this.animationClip = source.get().getAnimationClip();
            }
        }
    }

    @Override
    public void modifyPose(DynamicAnimation animation, Pose pose,
                           LivingEntityPatch<?> entityPatch, float time, float partialTicks) {
        super.modifyPose(animation, pose, entityPatch, time, partialTicks);
        AnimationPoseSettings.apply(action, pose, time);
    }

    @Override
    public float getPlaySpeed(LivingEntityPatch<?> entityPatch, DynamicAnimation animation) {
        // Retimes the complete composed clip, so the base motion cannot continue
        // underneath after the user's authored timeline has already reached contact.
        return super.getPlaySpeed(entityPatch, animation) * action.runtimeSpeed();
    }

    @Override
    public boolean canBePlayedReverse() {
        return true;
    }
}
