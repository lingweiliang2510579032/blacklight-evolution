package com.codex.blacklight.integration.epicfight;
import com.codex.blacklight.power.PowerForm;
import com.codex.blacklight.registry.ModAttachments;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.animation.types.ActionAnimation;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/** One authored clip, played backwards when the server has retracted the claws. */
public final class MorphActionAnimation extends ActionAnimation {
    public MorphActionAnimation(AnimationAccessor<? extends ActionAnimation> accessor) {super(.12F,accessor,Armatures.BIPED);}
    @Override public Pose getPoseByTime(LivingEntityPatch<?> patch,float time,float partial) {
        boolean retract=patch.getOriginal().getData(ModAttachments.PLAYER_STATE).getActivePower()!=PowerForm.CLAWS;
        return super.getPoseByTime(patch,retract?Math.max(0,getTotalTime()-time):time,partial);
    }
}
