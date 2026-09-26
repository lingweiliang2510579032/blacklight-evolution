package com.codex.blacklight.client.animation;

import com.codex.blacklight.combat.HitReaction;
import com.codex.blacklight.integration.epicfight.BlacklightEpicFightAnimations;
import com.codex.blacklight.registry.ModAttachments;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.model.armature.HumanoidArmature;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public final class NativeHitPose {
    private NativeHitPose(){}
    public static boolean available(LivingEntityPatch<?> patch){
        return patch!=null&&patch.getArmature() instanceof HumanoidArmature
                &&BlacklightEpicFightAnimations.reaction(patch.getOriginal().getData(ModAttachments.HIT_REACTION).motion())!=null;
    }
    public static boolean apply(LivingEntityPatch<?> patch,Armature armature,Pose pose,float partial){
        var entity=patch.getOriginal();var hit=entity.getData(ModAttachments.HIT_REACTION);
        if(entity.getData(ModAttachments.CONSUME_VISUAL).active()||hit.weight(entity.level().getGameTime(),partial)<=0||!available(patch))return false;
        var clip=BlacklightEpicFightAnimations.reaction(hit.motion()).get();
        float age=entity.level().getGameTime()-hit.started()+partial;
        float progress=hit.kind()==HitReaction.GETUP?hit.progress(entity.level().getGameTime(),partial):
                hit.kind()==HitReaction.DOWN?.45F:Math.clamp(age/(hit.kind()==HitReaction.LAUNCH||hit.kind()==HitReaction.SLAM?12:Math.min(12,hit.duration())),0,1);
        var sample=clip.getPoseByTime(patch,progress*Math.max(0,clip.getTotalTime()-.001F),partial);
        float weight=hit.weight(entity.level().getGameTime(),partial);
        var result=Pose.interpolatePose(pose,sample,weight);
        pose.getJointTransformData().putAll(result.getJointTransformData());return true;
    }
}
