package com.codex.blacklight.mixin.client;

import com.codex.blacklight.client.animation.ConsumeVictimPose;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.client.renderer.patched.entity.PatchedEntityRenderer;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

@Mixin(value=PatchedEntityRenderer.class,remap=false)
public abstract class EpicConsumePoseMixin {
    @WrapOperation(method="setArmaturePose",at=@At(value="INVOKE",target="Lyesman/epicfight/api/model/Armature;setPose(Lyesman/epicfight/api/animation/Pose;)V"))
    private void blacklight$victim(Armature armature,Pose pose,Operation<Void> original,
            @Local(argsOnly=true) LivingEntityPatch<?> patch,@Local(argsOnly=true) float partial) {
        com.codex.blacklight.client.animation.NativeUtilityPose.apply(patch,pose,partial);
        if(!com.codex.blacklight.client.animation.NativeHitPose.apply(patch,armature,pose,partial))
            ConsumeVictimPose.epic(patch.getOriginal(),pose,partial);
        if(patch.getOriginal().getData(com.codex.blacklight.registry.ModAttachments.GRAB_STAGE)==com.codex.blacklight.combat.ConsumeVisual.HOLD) {
            var held=com.codex.blacklight.integration.epicfight.BlacklightEpicFightAnimations.grabHold(patch.getOriginal().getData(com.codex.blacklight.registry.ModAttachments.PLAYER_STATE).getActivePower()).get()
                    .getPoseByTime(patch,(patch.getOriginal().tickCount+partial)%40/20F,partial);
            for(String joint:new String[]{"Torso","Chest","Head","Shoulder_R","Arm_R","Elbow_R","Hand_R","Tool_R","Shoulder_L","Arm_L","Elbow_L","Hand_L","Tool_L"})
                pose.getJointTransformData().put(joint,held.orElseEmpty(joint).copy());
        }
        if(!com.codex.blacklight.client.animation.NativeConsumePose.actor(patch.getOriginal(),pose,partial))
            com.codex.blacklight.client.animation.GrabHoldPose.apply(patch.getOriginal(),armature,pose,partial);
        com.codex.blacklight.client.animation.WallContactPose.apply(patch,armature,pose,partial);
        original.call(armature,pose);
    }
}
