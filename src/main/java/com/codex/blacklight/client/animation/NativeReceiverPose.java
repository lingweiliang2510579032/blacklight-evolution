package com.codex.blacklight.client.animation;

import com.codex.blacklight.combat.*;
import com.codex.blacklight.entity.BlacklightHunter;
import com.codex.blacklight.registry.ModAttachments;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.model.GeoModel;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.*;
import com.codex.blacklight.mixin.client.AgeableModelAccessor;
import java.util.*;

/** Receiver animation, not a generic bend added over the walk cycle. */
public final class NativeReceiverPose {
    private NativeReceiverPose() {}
    public static boolean available(LivingEntity entity,float partial){
        if(entity instanceof BlacklightHunter h&&h.isConsumeRemains())return ReceiverMotions.clip("hunter_consume")!=null;
        var v=com.codex.blacklight.registry.ModAttachments.getData(entity, ModAttachments.CONSUME_VISUAL);
        if(v.active())return entity instanceof BlacklightHunter&&ReceiverMotions.clip(v.consuming()?"hunter_consume":"hunter_grab")!=null;
        var hit=com.codex.blacklight.registry.ModAttachments.getData(entity, ModAttachments.HIT_REACTION);return hit.weight(entity.level().getGameTime(),partial)>0&&ReceiverMotions.clip(ReceiverMotions.hitKey(entity,hit.kind(),hit.motion()))!=null;
    }
    public static boolean apply(GeoModel<?> model,LivingEntity entity,float partial){
        var visual=com.codex.blacklight.registry.ModAttachments.getData(entity, ModAttachments.CONSUME_VISUAL);String key;float progress,weight;
        if(entity instanceof BlacklightHunter hunter&&hunter.isConsumeRemains()) {
            key="hunter_consume";weight=1;
            var clip=ReceiverMotions.clip(key);if(clip==null)return false;
            progress=Math.min(HunterConsume.RECEIVER_END_SECONDS,HunterConsume.COMPLETE_SECONDS+hunter.consumeRemainsAge(partial)/20F)/clip.seconds();
        }else if(visual.active()){
            if(!(entity instanceof BlacklightHunter))return false;
            key=visual.consuming()?"hunter_consume":"hunter_grab";
            progress=visual.consuming()?visual.motionProgress(entity.level().getGameTime(),partial):
                    visual.stage()==ConsumeVisual.MOUNT_HOLD?1:visual.phaseProgress(entity.level().getGameTime(),partial);
            weight=net.minecraft.util.Mth.clamp((entity.level().getGameTime()-visual.started()+partial)/3F,0,1);
            var clip=ReceiverMotions.clip(key);
            if(clip!=null&&visual.consuming())progress=visual.stage()==ConsumeVisual.HUNTER_CONSUME
                    ?net.minecraft.util.Mth.clamp(HunterConsume.seconds(visual,entity.level().getGameTime(),partial)/clip.seconds(),0,1)
                    :net.minecraft.util.Mth.clamp(progress*(visual.duration()-1)/(20*clip.seconds()),0,1);
        }else{
            var hit=com.codex.blacklight.registry.ModAttachments.getData(entity, ModAttachments.HIT_REACTION);weight=hit.weight(entity.level().getGameTime(),partial);if(weight<=0)return false;
            key=ReceiverMotions.hitKey(entity,hit.kind(),hit.motion());progress=hit.progress(entity.level().getGameTime(),partial);
        }
        float[] row=ReceiverMotions.sample(key,progress);if(row==null)return false;
        applyRow(model,row,weight);
        var root=model.getAnimationProcessor().getBone("root");
        if(root!=null){root.setPosX(0);root.setPosZ(0);root.setPosY(row[1]*entity.getBbHeight()*16*weight);}
        return true;
    }
    public static void locomotion(GeoModel<?> model,BlacklightHunter entity,float partial){
        String key=entity.committedAttack()?"hunter_swipe":entity.walkAnimation.speed()>.04F
                ?entity.walkAnimation.speed()>.16F?"hunter_run":"hunter_walk":"hunter_idle";
        var clip=ReceiverMotions.clip(key);if(clip==null)return;
        float progress=entity.committedAttack()?net.minecraft.util.Mth.clamp((24-entity.attackTicksRemaining()+partial)/24F,0,1)
                :((entity.tickCount+partial)/20F)%clip.seconds()/clip.seconds();
        float[] row=ReceiverMotions.sample(key,progress);if(row!=null)applyRow(model,row,1);
    }
    private static void applyRow(GeoModel<?> model,float[] row,float weight){
        Map<CoreGeoBone,Quaternionf> desired=new IdentityHashMap<>();
        for(int i=0;i<ReceiverMotions.BONES.length;i++){
            CoreGeoBone bone=model.getAnimationProcessor().getBone(ReceiverMotions.BONES[i]);
            if(bone==null){String alias=switch(ReceiverMotions.BONES[i]){case "torso"->"body";case "right_thigh"->"right_leg";case "left_thigh"->"left_leg";default->"";};if(!alias.isEmpty())bone=model.getAnimationProcessor().getBone(alias);}
            if(bone==null)continue;
            int k=3+i*4;desired.put(bone,new Quaternionf(row[k],row[k+1],row[k+2],row[k+3]).mul(bindWorld(bone)));
        }
        for(var entry:desired.entrySet()){
            CoreGeoBone bone=entry.getKey(),parent=bone.getParent();
            Quaternionf parentWorld=parent==null?new Quaternionf():desired.getOrDefault(parent,bindWorld(parent));
            Quaternionf local=new Quaternionf(parentWorld).invert().mul(entry.getValue());
            Quaternionf old=new Quaternionf().rotationZYX(bone.getRotZ(),bone.getRotY(),bone.getRotX());
            Vector3f angles=old.slerp(local,weight).getEulerAnglesZYX(new Vector3f());bone.updateRotation(angles.x,angles.y,angles.z);
        }
    }
    public static void resetRoot(GeoModel<?> model){var root=model.getAnimationProcessor().getBone("root");if(root!=null){root.setPosX(0);root.setPosY(0);root.setPosZ(0);}}
    public static boolean vanilla(EntityModel<?> model,LivingEntity entity,float partial,Map<ModelPart,PartPose> saved){
        var hit=com.codex.blacklight.registry.ModAttachments.getData(entity, ModAttachments.HIT_REACTION);float weight=hit.weight(entity.level().getGameTime(),partial);if(weight<=0)return false;
        float[] row=ReceiverMotions.sample(ReceiverMotions.hitKey(entity,hit.kind(),hit.motion()),hit.progress(entity.level().getGameTime(),partial));if(row==null)return false;
        if(model instanceof HumanoidModel<?> h){
            part(h.head,3,row,weight,saved);part(h.hat,3,row,weight,saved);part(h.body,2,row,weight,saved);
            part(h.rightArm,5,row,weight,saved);part(h.leftArm,6,row,weight,saved);part(h.rightLeg,9,row,weight,saved);part(h.leftLeg,10,row,weight,saved);return true;
        }
        if(model instanceof HierarchicalModel<?> h){
            var mapping=Map.of("head",3,"body",2,"right_arm",5,"left_arm",6,"arms",5,"right_leg",9,"left_leg",10,"right_hind_leg",9,"left_hind_leg",10);
            mapping.forEach((name,index)->{if(h.root().hasChild(name))part(h.root().getChild(name),index,row,weight*.7F,saved);});
            if(h.root().hasChild("right_front_leg"))part(h.root().getChild("right_front_leg"),5,row,weight*.6F,saved);
            if(h.root().hasChild("left_front_leg"))part(h.root().getChild("left_front_leg"),6,row,weight*.6F,saved);return true;
        }
        if(model instanceof AgeableModelAccessor a){
            for(var head:a.blacklight$heads())part(head,3,row,weight*.6F,saved);
            int[] indices={2,9,10,5,6};int i=0;for(var body:a.blacklight$body()){if(i>=indices.length)break;part(body,indices[i++],row,weight*.5F,saved);}return true;
        }
        return false;
    }
    private static void part(ModelPart p,int index,float[] row,float weight,Map<ModelPart,PartPose> saved){
        saved.putIfAbsent(p,p.storePose());int k=3+index*4;
        Quaternionf q=new Quaternionf(row[3],row[4],row[5],row[6]).invert().mul(new Quaternionf(row[k],row[k+1],row[k+2],row[k+3]));
        q.set(-q.x,-q.y,q.z,q.w);var bind=p.getInitialPose();
        q.mul(new Quaternionf().rotationZYX(bind.zRot,bind.yRot,bind.xRot));
        Vector3f angles=new Quaternionf().rotationZYX(p.zRot,p.yRot,p.xRot).slerp(q,weight).getEulerAnglesZYX(new Vector3f());p.xRot=angles.x;p.yRot=angles.y;p.zRot=angles.z;
    }
    private static Quaternionf bindWorld(CoreGeoBone bone){var bind=bone.getInitialSnapshot();var q=new Quaternionf().rotationZYX(bind.getRotZ(),bind.getRotY(),bind.getRotX());return bone.getParent()==null?q:bindWorld(bone.getParent()).mul(q);}
}
