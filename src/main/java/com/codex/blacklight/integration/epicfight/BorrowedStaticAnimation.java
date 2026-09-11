package com.codex.blacklight.integration.epicfight;

import java.util.function.Supplier;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.model.Armature;

/** Blacklight living-motion accessor backed by a compatible Epic Fight clip. */
public final class BorrowedStaticAnimation extends StaticAnimation {
    private final Supplier<AnimationAccessor<? extends StaticAnimation>> motionSource;

    public BorrowedStaticAnimation(float transitionTime,
                                   AnimationAccessor<? extends StaticAnimation> accessor,
                                   AssetAccessor<? extends Armature> armature,
                                   Supplier<AnimationAccessor<? extends StaticAnimation>> motionSource) {
        super(transitionTime, false, accessor, armature);
        this.motionSource = motionSource;
    }

    @Override
    public void loadAnimation() {
        super.loadAnimation();
        AnimationAccessor<? extends StaticAnimation> source = motionSource.get();
        if (source != null) this.animationClip = source.get().getAnimationClip();
    }
}
