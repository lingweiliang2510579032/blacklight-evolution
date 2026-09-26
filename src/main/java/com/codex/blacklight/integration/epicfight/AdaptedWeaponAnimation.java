package com.codex.blacklight.integration.epicfight;

import com.codex.blacklight.BlacklightNeoMod;
import com.codex.blacklight.combat.WeaponMotionProfile;
import com.codex.blacklight.combat.WeaponMove;
import com.codex.blacklight.power.PowerForm;
import java.util.ArrayList;
import org.joml.Quaternionf;
import yesman.epicfight.api.animation.*;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.property.AnimationProperty.ActionAnimationProperty;
import yesman.epicfight.api.animation.types.ActionAnimation;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.utils.TimePairList;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/** Own action/state/coordinate track; dependency supplies only joint keyframes, never attack code. */
public final class AdaptedWeaponAnimation extends ActionAnimation {
    private final WeaponMove move;
    private final WeaponMotionProfile profile;
    public AdaptedWeaponAnimation(AnimationAccessor<? extends ActionAnimation> accessor,WeaponMove move) {
        super(.05F,accessor,Armatures.BIPED);
        this.move=move;this.profile=WeaponMotionProfile.forMove(move);
        var coord=new ArrayList<Keyframe>();
        for(int i=0;i<move.duration;i++) {
            float time=i/20F;
            coord.add(new Keyframe(time,JointTransform.translation(new Vec3f(0,0,-profile.distanceAt(move,time)))));
        }
        addProperty(ActionAnimationProperty.COORD,new TransformSheet(coord));
        addProperty(ActionAnimationProperty.MOVE_ON_LINK,false);
        addProperty(ActionAnimationProperty.MOVE_VERTICAL,false);
        addProperty(ActionAnimationProperty.STOP_MOVEMENT,true);
        addProperty(ActionAnimationProperty.MOVE_TIME,TimePairList.create(0,(move.duration-1)/20F));
    }
    @Override public void loadAnimation() {
        // Keep our own registry/resource identity. No foreign properties, events, colliders or skills are loaded.
        super.loadAnimation();
        String key=profile.source();
        var source=AnimationManager.byKey(key);
        if(source==null){
            // Optional WOM absent: keep the complete authored clip loaded above.
            // Its fitted fist paths remain safer than applying WOM corrections to an unrelated motion.
            if(key.startsWith("wom:")){
                BlacklightNeoMod.LOGGER.info("Adapted weapon {}: WOM absent, using authored fallback",move.id);
                return;
            }
            throw new IllegalStateException("Required Epic Fight motion missing: "+key);
        }
        AnimationClip original=source.get().getAnimationClip();
        float in=profile.contactIn(),out=profile.contactOut();
        // Read only contact timestamps, never execute the dependency's attack phase.
        // This retains its actual wind-up/strike boundary when mapping into our server window.
        if(source.get() instanceof yesman.epicfight.api.animation.types.AttackAnimation attack&&attack.phases.length>0){
            var phase=attack.phases[0];
            if(phase.preDelay>0&&phase.contact>phase.preDelay&&phase.contact<=original.getClipTime()){
                in=phase.preDelay;out=phase.contact;
            }
        }
        var adapted=new AnimationClip();
        int samples=(move.duration-1)*3;
        for(var entry:original.getJointTransforms().entrySet()) {
            var frames=new ArrayList<Keyframe>();
            for(int i=0;i<=samples;i++) {
                float time=i/60F;
                float sourceTime=WeaponMotionProfile.sourceTime(move,time,original.getClipTime(),in,out);
                JointTransform jt=entry.getValue().getInterpolatedTransform(sourceTime).copy();
                fitArm(entry.getKey(),jt);
                if(entry.getKey().startsWith("Elbow_"))
                    jt.rotation().set(new Quaternionf().slerp(jt.rotation(),profile.elbowWeight()));
                if(entry.getKey().equals("Hand_R")&&move.form==PowerForm.BLADE)
                    jt.rotation().rotateY((float)Math.toRadians(profile.wristRoll()+(move==WeaponMove.BLADE_CUT?30:0))).normalize();
                frames.add(new Keyframe(time,jt));
            }
            adapted.addJointTransform(entry.getKey(),new TransformSheet(frames));
        }
        adapted.setClipTime((move.duration-1)/20F);
        adapted.bakeKeyframes();
        this.animationClip=adapted;
        BlacklightNeoMod.LOGGER.info("Adapted weapon {} from {} (motion only, {} ticks; source contact {}..{})",move.id,key,move.duration,in,out);
    }
    private record ArmFit(float pitch,float roll,float forearmWeight){}
    private void fitArm(String joint,JointTransform transform){
        boolean right=joint.endsWith("_R");
        // Per-arm corrections measured against the current weapon meshes at 60 samples/second.
        // Hand is the forearm in EF's rig; Elbow is its separate skinning helper.
        ArmFit fit=switch(move){
            case BLADE_RETURN -> right?new ArmFit(15,15,1):new ArmFit(0,0,1);
            case HAMMER_JAB -> right?new ArmFit(0,-45,1):new ArmFit(0,-30,1);
            case HAMMER_HOOK -> right?new ArmFit(0,0,1):new ArmFit(0,15,.9F);
            case MUSCLE_UPPERCUT -> right?new ArmFit(15,0,.8F):new ArmFit(15,-15,1);
            case MUSCLE_AIR -> right?new ArmFit(0,0,1):new ArmFit(0,0,.9F);
            default -> new ArmFit(0,0,1);
        };
        if(joint.startsWith("Arm_"))transform.rotation().rotateX((float)Math.toRadians(fit.pitch)).rotateZ((float)Math.toRadians(fit.roll)).normalize();
        if(joint.startsWith("Hand_"))transform.rotation().set(new Quaternionf().slerp(transform.rotation(),fit.forearmWeight));
    }
    @Override public void modifyPose(DynamicAnimation animation,Pose pose,LivingEntityPatch<?> patch,float time,float partialTicks) {
        super.modifyPose(animation,pose,patch,time,partialTicks);
        // Source root translation is in armature coordinates; use EF's world-aware correction.
        // Retain crouch/bank/hip rotation while COORD supplies the sole collision-aware displacement.
        correctRootJoint(animation,pose,patch,time,partialTicks);
    }
}
