package com.codex.blacklight.client.animation;

import com.codex.blacklight.combat.GrabPlacement;
import com.codex.blacklight.combat.ConsumeVisual;
import com.codex.blacklight.power.PowerForm;
import com.codex.blacklight.registry.ModAttachments;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.physics.ik.FABRIK;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;

/** Move the shoulder and elbow together so the rendered palm stays on the held body. */
public final class GrabHoldPose {
    private GrabHoldPose() {}
    public static void apply(LivingEntity player,Armature armature,Pose pose,float partial) {
        int stage=player.getData(ModAttachments.GRAB_STAGE);
        if(stage==ConsumeVisual.NONE||stage==ConsumeVisual.CONSUME||com.codex.blacklight.combat.ConsumeSequence.mountedStage(stage))return;
        boolean blade=player.getData(ModAttachments.PLAYER_STATE).getActivePower()==PowerForm.BLADE;
        var held=player.level().getEntitiesOfClass(LivingEntity.class,player.getBoundingBox().inflate(4),e->{
            var visual=e.getData(ModAttachments.CONSUME_VISUAL);
            return visual.consumerId()==player.getId()&&(visual.stage()==ConsumeVisual.HOLD || blade &&
                    (visual.stage()==ConsumeVisual.CONSUME || visual.stage()==ConsumeVisual.THROW || visual.stage()==ConsumeVisual.CAPTURE &&
                     visual.phaseProgress(player.level().getGameTime(),partial)>=.6F));
        });
        if(held.isEmpty())return;
        var mob=held.getFirst();
        float yaw=Mth.rotLerp(partial,player.yRotO,player.getYRot());
        float pitch=Mth.lerp(partial,player.xRotO,player.getXRot());
        var forward=Vec3.directionFromRotation(0,yaw);
        var left=new Vec3(forward.z,0,-forward.x);
        var ownerPos=player.getPosition(partial);var mobPos=mob.getPosition(partial);
        double gripHeight=Math.clamp(GrabPlacement.palm(ownerPos,yaw,pitch).y-mobPos.y,
                mob.getBbHeight()*.15,mob.getBbHeight()*.85);
        var target=mobPos.add(0,gripHeight,0).subtract(forward.scale(mob.getBbWidth()*.4)).subtract(ownerPos);
        if(blade) {
            var visual=mob.getData(ModAttachments.CONSUME_VISUAL);
            float progress=visual.consuming()?visual.progress(player.level().getGameTime(),partial):0;
            double height=Math.clamp(GrabPlacement.bladeContact(ownerPos,yaw,pitch,progress).y-mobPos.y,mob.getBbHeight()*.15,mob.getBbHeight()*.85);
            target=mobPos.add(ConsumeVictimPose.point(mob,new Vec3(0,height,0),partial)).subtract(ownerPos);
        }
        var desired=new Vec3f((float)(-target.dot(left)/.9375),(float)(target.y/.9375),(float)(-target.dot(forward)/.9375));
        String side=blade?"R":"L",tool="Tool_"+side;
        var arm=armature.searchJointByName("Arm_"+side);
        var wrist=armature.searchJointByName(tool);
        boolean claws=player.getData(ModAttachments.PLAYER_STATE).getActivePower()==PowerForm.CLAWS;
        var palmLocal=blade?new Vec3f(-.0636396F,.68F,-.0636396F):claws?new Vec3f(.00267F,.32362F,.07666F):new Vec3f(0,.28F,0);
        // Solve for the palm itself. Repeatedly compensating a wrist target can
        // oscillate during abrupt pitch changes because the palm offset rotates
        // with the forearm. Temporarily move only this pose's tool endpoint;
        // the shared skeleton and the visible hand geometry stay unchanged.
        var toolPose=pose.orElseEmpty(tool).copy();
        var endpoint=OpenMatrix4f.transform3v(OpenMatrix4f.invert(wrist.getLocalTransform(),null),palmLocal,null);
        pose.getJointTransformData().put(tool,yesman.epicfight.api.animation.JointTransform.translation(endpoint));
        try {
            new FABRIK(pose,armature,arm,wrist).run(desired,16);
        } finally {
            pose.getJointTransformData().put(tool,toolPose);
        }
    }
}
