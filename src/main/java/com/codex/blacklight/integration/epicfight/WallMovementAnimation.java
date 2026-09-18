package com.codex.blacklight.integration.epicfight;

import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.types.MovementAnimation;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/** Vertical/tangential wall speed controls cadence; ground gait yaw must not twist wall feet. */
public final class WallMovementAnimation extends MovementAnimation {
    private final double nominalSpeed;
    private final net.minecraft.resources.ResourceLocation authoredLocation;
    private yesman.epicfight.api.animation.AnimationClip clawArms;
    public WallMovementAnimation(boolean repeat, double nominalSpeed, AnimationAccessor<? extends MovementAnimation> accessor) {
        super(.10F,repeat,accessor,Armatures.BIPED);this.nominalSpeed=nominalSpeed;this.authoredLocation=getLocation();
        // Read the independently installed addon's original clip. No third-party asset
        // is copied into this jar, and no ParCool root-motion/action code is executed.
        if (nominalSpeed == .55 && net.neoforged.fml.ModList.get().isLoaded("epicparcool")) {
            setResourceLocation("epicparcool", "biped/wall_run_vertical");
        }
    }
    @Override public void loadAnimation() {
        var selected=getLocation();
        if(!selected.equals(authoredLocation)) {
            resourceLocation=authoredLocation;super.loadAnimation();clawArms=animationClip;
            resourceLocation=selected;
        } else clawArms=null;
        super.loadAnimation();
    }
    @Override public yesman.epicfight.api.animation.Pose getPoseByTime(LivingEntityPatch<?> patch,float time,float partial) {
        var pose=super.getPoseByTime(patch,time,partial);
        if(clawArms!=null) {
            float duration=Math.max(.001F,getTotalTime());
            var arms=clawArms.getPoseInTime(((time%duration+duration)%duration)/duration*clawArms.getClipTime());
            // ParCool was authored for bare hands. Its high wrist swing puts our
            // extended talons across the face: retain our claw-safe upper body.
            for(String joint:new String[]{"Torso","Chest","Head","Shoulder_R","Arm_R","Hand_R","Tool_R","Shoulder_L","Arm_L","Hand_L","Tool_L"})
                if(arms.hasTransform(joint))pose.putJointData(joint,arms.get(joint).copy());
        }
        return pose;
    }
    @Override public float getPlaySpeed(LivingEntityPatch<?> patch, DynamicAnimation animation) {
        if(nominalSpeed<=0 || animation.isLinkAnimation())return 1;
        return (float)Math.clamp(patch.getOriginal().getDeltaMovement().length()/nominalSpeed,.55,1.8);
    }
}
