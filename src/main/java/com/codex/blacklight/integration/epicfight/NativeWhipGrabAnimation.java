package com.codex.blacklight.integration.epicfight;

import com.codex.blacklight.combat.LocalVisualMotion;
import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.*;
import yesman.epicfight.api.animation.property.AnimationProperty.ActionAnimationProperty;
import yesman.epicfight.api.asset.*;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/** Original local grab body track; ConsumeSystem exclusively owns the victim's pull. */
public final class NativeWhipGrabAnimation extends ActionAnimation {
    public NativeWhipGrabAnimation(AnimationAccessor<? extends ActionAnimation> accessor){
        super(.05F,accessor,Armatures.BIPED);addProperty(ActionAnimationProperty.STOP_MOVEMENT,true);
    }
    @Override public void loadAnimation(){
        var motion=LocalVisualMotion.find("grab_whip_native");
        if(motion.isEmpty()){super.loadAnimation();return;}
        animationClip=new JsonAssetLoader(motion.get().animation().deepCopy(),ResourceLocation.fromNamespaceAndPath("blacklight","local/grab_whip_native")).loadClipForAnimation(this);
        animationClip.bakeKeyframes();
    }
    @Override protected boolean validateMovement(LivingEntityPatch<?> patch,AssetAccessor<? extends DynamicAnimation> animation){return false;}
    @Override public void tick(LivingEntityPatch<?> patch){
        if(patch.getOriginal().getData(com.codex.blacklight.registry.ModAttachments.GRAB_STAGE)==com.codex.blacklight.combat.ConsumeVisual.WHIP_PULL){
            var clock=patch.getAnimator().getPlayerFor(getAccessor());
            if(clock.getElapsedTime()>.7F)clock.setElapsedTime(.7F,.7F);
        }
        super.tick(patch);
    }
}
