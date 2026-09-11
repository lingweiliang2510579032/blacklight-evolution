package com.codex.blacklight.integration.epicfight;

import java.util.function.Supplier;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.ActionAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.model.Armature;

/**
 * Visual-only Blacklight action that reuses a motion clip already loaded by Epic Fight.
 *
 * <p>The Blacklight accessor and JSON are still loaded normally, so Epic Fight validates
 * this as an {@link ActionAnimation}. Only the baked joint keyframes are borrowed after
 * loading; attack phases and colliders remain under Blacklight's server-authoritative
 * combat system.</p>
 */
public final class BorrowedActionAnimation extends ActionAnimation {
    private final Supplier<AnimationAccessor<? extends StaticAnimation>> motionSource;

    public BorrowedActionAnimation(float transitionTime,
                                   AnimationAccessor<? extends ActionAnimation> accessor,
                                   AssetAccessor<? extends Armature> armature,
                                   Supplier<AnimationAccessor<? extends StaticAnimation>> motionSource) {
        super(transitionTime, accessor, armature);
        this.motionSource = motionSource;
    }

    @Override
    public void loadAnimation() {
        super.loadAnimation();
        AnimationAccessor<? extends StaticAnimation> source = motionSource.get();
        if (source != null) {
            this.animationClip = source.get().getAnimationClip();
        }
    }
}
