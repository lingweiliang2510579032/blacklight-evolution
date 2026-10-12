package com.codex.blacklight.client.animation;

import com.codex.blacklight.combat.HitReaction;
import com.codex.blacklight.animation.MotionLibrary;
import com.codex.blacklight.registry.ModAttachments;
import com.codex.blacklight.animation.BlacklightPose;

public final class NativeHitPose {
    private NativeHitPose(){}
    private static String key(int motion) { return new String[]{"reaction_body","reaction_left","reaction_right","reaction_backhand","reaction_up","reaction_fall","reaction_down","reaction_rise"}[net.minecraft.util.Mth.clamp(motion==8?3:motion,0,7)]; }
    public static boolean available(net.minecraft.world.entity.LivingEntity entity){
        return entity!=null && MotionLibrary.find(key(com.codex.blacklight.registry.ModAttachments.getData(entity, ModAttachments.HIT_REACTION).motion())).isPresent();
    }
    public static boolean apply(net.minecraft.world.entity.LivingEntity entity,com.codex.blacklight.animation.BlacklightSkeleton armature,BlacklightPose pose,float partial){
        var hit=com.codex.blacklight.registry.ModAttachments.getData(entity, ModAttachments.HIT_REACTION);
        if(com.codex.blacklight.registry.ModAttachments.getData(entity, ModAttachments.CONSUME_VISUAL).active()||hit.weight(entity.level().getGameTime(),partial)<=0||!available(entity))return false;
        var clip=MotionLibrary.find(key(hit.motion())).orElseThrow();
        float age=entity.level().getGameTime()-hit.started()+partial;
        float progress=hit.kind()==HitReaction.GETUP?hit.progress(entity.level().getGameTime(),partial):
                hit.kind()==HitReaction.DOWN?.45F:net.minecraft.util.Mth.clamp(age/(hit.kind()==HitReaction.LAUNCH||hit.kind()==HitReaction.SLAM?12:Math.min(12,hit.duration())),0,1);
        var sample=MotionLibrary.pose(key(hit.motion()),progress*Math.max(0,clip.duration()-.001F),false);
        float weight=hit.weight(entity.level().getGameTime(),partial);
        var result=BlacklightPose.interpolatePose(pose,sample,weight);
        pose.getJointTransformData().putAll(result.getJointTransformData());return true;
    }
}
