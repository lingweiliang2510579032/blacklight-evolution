package com.codex.blacklight.integration.epicfight;

import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.*;
import yesman.epicfight.api.animation.property.AnimationProperty.ActionAnimationProperty;
import yesman.epicfight.api.asset.*;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.api.utils.datastructure.ParameterizedHashMap;

/** Visual action only. Native source Coord is diagnostic and must not move the player twice. */
public final class NativeTraversalAction extends ActionAnimation {
    private final String id;
    public NativeTraversalAction(AnimationAccessor<? extends ActionAnimation> accessor,String id){
        super(.06F,accessor,Armatures.BIPED);this.id=id;
        addProperty(ActionAnimationProperty.STOP_MOVEMENT,false);
        addProperty(ActionAnimationProperty.MOVE_ON_LINK,false);
        addProperty(ActionAnimationProperty.MOVE_VERTICAL,false);
    }
    @Override public void loadAnimation(){
        var data=NativeTraversalLibrary.require(id);
        animationClip=new JsonAssetLoader(data.animation().deepCopy(),ResourceLocation.fromNamespaceAndPath("blacklight","local/"+id)).loadClipForAnimation(this);
        animationClip.bakeKeyframes();
    }
    @Override protected boolean validateMovement(LivingEntityPatch<?> patch,AssetAccessor<? extends DynamicAnimation> animation){return false;}
    @Override public ParameterizedHashMap<EntityState.StateFactor<?>> getStatesMap(LivingEntityPatch<?> patch,float time){
        var states=super.getStatesMap(patch,time);
        if(!id.equals("basic_jump_charge")){
            states.put(EntityState.MOVEMENT_LOCKED,false);
            states.put(EntityState.TURNING_LOCKED,false);
            states.put(EntityState.SKILL_EXECUTABLE,true);
            states.put(EntityState.COMBO_ATTACKS_DOABLE,true);
            states.put(EntityState.UPDATE_LIVING_MOTION,true);
            // Keep the complete recovery when standing; moving can cancel its tail.
            float minimum=id.equals("basic_land_devastating")?.3F:id.equals("basic_land_heavy")?.15F:.1F;
            if(id.startsWith("basic_land_")&&time>=minimum
                    &&patch.getOriginal().getDeltaMovement().horizontalDistance()>.04)
                states.put(EntityState.INACTION,false);
        }
        return states;
    }
}
