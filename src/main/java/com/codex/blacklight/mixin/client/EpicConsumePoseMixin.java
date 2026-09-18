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
        ConsumeVictimPose.epic(patch.getOriginal(),pose,partial);
        original.call(armature,pose);
    }
}
