package com.codex.blacklight.integration.epicfight;

import com.codex.blacklight.combat.LocalVisualMotion;
import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.*;
import yesman.epicfight.api.asset.*;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/** Native full-body consume; the server exclusively positions the two participants. */
public final class NativeConsumeAnimation extends ActionAnimation {
    private final String key;
    public NativeConsumeAnimation(AnimationAccessor<? extends ActionAnimation> accessor,String key) {
        super(.05F,accessor,Armatures.BIPED);this.key=key;
    }
    @Override public void loadAnimation() {
        var source=LocalVisualMotion.find(key);
        if(source.isEmpty()){super.loadAnimation();return;}
        animationClip=new JsonAssetLoader(source.get().animation().deepCopy(),ResourceLocation.fromNamespaceAndPath("blacklight","local/"+key)).loadClipForAnimation(this);
        animationClip.bakeKeyframes();
    }
    @Override public yesman.epicfight.api.utils.datastructure.ParameterizedHashMap<EntityState.StateFactor<?>> getStatesMap(LivingEntityPatch<?> patch,float time) {
        var states=super.getStatesMap(patch,time);
        if(!patch.getOriginal().getData(com.codex.blacklight.registry.ModAttachments.CONSUMING)) {
            states.put(EntityState.MOVEMENT_LOCKED,false);states.put(EntityState.TURNING_LOCKED,false);
            states.put(EntityState.UPDATE_LIVING_MOTION,true);states.put(EntityState.INACTION,false);
            states.put(EntityState.SKILL_EXECUTABLE,true);states.put(EntityState.COMBO_ATTACKS_DOABLE,true);
        }
        return states;
    }
    @Override protected boolean validateMovement(LivingEntityPatch<?> patch,AssetAccessor<? extends DynamicAnimation> animation){return false;}
}
