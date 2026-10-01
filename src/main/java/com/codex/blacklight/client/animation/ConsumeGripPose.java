package com.codex.blacklight.client.animation;

import com.codex.blacklight.combat.*;
import com.codex.blacklight.registry.ModAttachments;
import com.codex.blacklight.power.PowerForm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.animation.JointTransform;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.physics.ik.FABRIK;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;

/** Keep the two palms on the rendered head when species proportions or terrain differ. */
public final class ConsumeGripPose {
    private ConsumeGripPose() {}
    public static void apply(LivingEntity actor, Armature armature, Pose pose, float partial) {
        if(actor.getData(ModAttachments.GRAB_STAGE)!=ConsumeVisual.CONSUME)return;
        var victims=actor.level().getEntitiesOfClass(LivingEntity.class,actor.getBoundingBox().inflate(5),v->{
            var scene=v.getData(ModAttachments.CONSUME_VISUAL);
            return scene.consumerId()==actor.getId()&&scene.stage()==ConsumeVisual.CONSUME&&scene.form()==PowerForm.CLAWS.ordinal();
        });
        if(victims.isEmpty())return;
        var victim=victims.getFirst();var visual=victim.getData(ModAttachments.CONSUME_VISUAL);
        float time=visual.motionProgress(actor.level().getGameTime(),partial),end=ConsumePlacementTrack.gripEnd(PowerForm.CLAWS);
        float weight=Math.clamp((actor.level().getGameTime()-visual.started()+partial)/4F,0,1)*Math.clamp((end-time)/.06F,0,1);
        if(weight<=0)return;
        Vec3 forward=Vec3.directionFromRotation(0,visual.facing()),left=new Vec3(forward.z,0,-forward.x);
        boolean quad=ConsumeSequence.quadruped(victim);
        Vec3 head=ConsumeVictimPose.point(victim,new Vec3(0,victim.getBbHeight()*(quad?.65:.86),quad?-victim.getBbWidth()*.45:0),partial);
        Vec3 target=victim.getPosition(partial).add(com.codex.blacklight.client.ClientCarryPlacement.consumeOffset(victim,partial))
                .add(head).subtract(actor.getPosition(partial));
        if(target.lengthSqr()>9)return;
        Pose original=Pose.interpolatePose(pose,pose,0);
        for(String side:new String[]{"R","L"}){
            var arm=armature.searchJointByName("Arm_"+side);var wrist=armature.searchJointByName("Tool_"+side);
            if(arm==null||wrist==null)continue;
            Vec3 contact=target.add(left.scale((side.equals("L")?1:-1)*Math.clamp(victim.getBbWidth()*.22,.09,.22)));
            Vec3f desired=new Vec3f((float)(-contact.dot(left)/.9375),(float)(contact.y/.9375),(float)(-contact.dot(forward)/.9375));
            var tool=pose.orElseEmpty("Tool_"+side).copy();
            var palm=new Vec3f(side.equals("L")?.00267F:-.00267F,.32362F,.07666F);
            var endpoint=OpenMatrix4f.transform3v(OpenMatrix4f.invert(wrist.getLocalTransform(),null),palm,null);
            pose.getJointTransformData().put("Tool_"+side,JointTransform.translation(endpoint));
            try{new FABRIK(pose,armature,arm,wrist).run(desired,12);}
            finally{pose.getJointTransformData().put("Tool_"+side,tool);}
        }
        var blended=Pose.interpolatePose(original,pose,weight);
        pose.getJointTransformData().putAll(blended.getJointTransformData());
    }
}
