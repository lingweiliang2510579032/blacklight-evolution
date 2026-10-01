package com.codex.blacklight.client.animation;

import com.codex.blacklight.integration.epicfight.BlacklightEpicFightAnimations;
import com.codex.blacklight.power.*;
import com.codex.blacklight.registry.ModAttachments;
import net.minecraft.world.entity.player.Player;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/** Source poses over the current gait. Combat, consumption and wall contact keep priority. */
public final class NativeUtilityPose {
    public record Selection(String key, float time, float weight, boolean plated, boolean transition, boolean moving) { }
    public static Selection select(LivingEntityPatch<?> patch, float partial) {
        if (!(patch.getOriginal() instanceof Player p) || !p.onGround()
                || p.getData(ModAttachments.WEAPON_VISUAL).spec() != null
                || p.getData(ModAttachments.GRAB_STAGE) != 0
                || p.getData(ModAttachments.CONSUME_RECOVERY).active()
                || p.getData(ModAttachments.HIT_REACTION).weight(p.level().getGameTime(), partial) > 0
                || p.getData(ModAttachments.JUMP_CHARGE) > 0) return null;
        var s = p.getData(ModAttachments.UTILITY_STATE);
        float armor = UtilityPowerSystem.visualWeight(p, PowerForm.ARMOR, partial);
        float shield = UtilityPowerSystem.visualWeight(p, PowerForm.SHIELD, partial);
        if (armor <= 0 && shield <= 0) return null;
        double age = (p.level().getGameTime() - s.changedAt() + partial) / 20.0;
        double hit = (p.level().getGameTime() - s.hitAt() + partial) / 20.0;
        boolean plated = armor > shield;
        boolean moving = p.getDeltaMovement().horizontalDistance() > .04;
        String key = plated ? "armor_transform" : "shield_deploy";
        var clip = BlacklightEpicFightAnimations.utilityPose(key).get();
        float time = (float)age;
        boolean impact = !plated && hit >= 0 && hit < .367;
        boolean transition = UtilityPowerSystem.active(p, plated ? PowerForm.ARMOR : PowerForm.SHIELD)
                && !impact && age >= 0 && age < clip.getTotalTime();
        if (!transition) {
            key = plated ? "armor_idle" : impact ? "shield_hit" : p.isSprinting() ? "shield_sprint" : "shield_run";
            clip = BlacklightEpicFightAnimations.utilityPose(key).get();
            time = key.equals("shield_hit") ? (float)hit : !plated && !moving ? 0
                    : (float)(((p.level().getGameTime() + partial) / 20.0) % Math.max(.001, clip.getTotalTime()));
        }
        time = Math.clamp(time, 0, Math.max(0, clip.getTotalTime() - .001F));
        float weight = Math.max(armor, shield);
        if (transition) weight *= Math.min(1, (clip.getTotalTime() - (float)age) / .15F);
        return new Selection(key, time, weight, plated, transition, moving);
    }
    public static void apply(LivingEntityPatch<?> patch, Pose pose, float partial) {
        if(patch.getOriginal() instanceof Player p) {
            var morph=p.getData(ModAttachments.POWER_MORPH);
            var animation=BlacklightEpicFightAnimations.utilityPose(morph.key());
            if(animation!=null&&morph.visible(p,partial)) {
                float t=morph.seconds(p,partial),duration=animation.get().getTotalTime();
                float in=morph.key().equals("morph_thermal_vision")?0:.333333F;
                float out=switch(morph.key()) {
                    case "morph_claws"->.5F;case "morph_blade","morph_hammerfists"->.1F;
                    case "morph_whipfist"->.166667F;default->.333333F;
                };
                float weight=Math.clamp(Math.min(in==0?1:t/in,(duration-t)/out),0,1);
                var source=animation.get().getRawPose(Math.clamp(t,0,Math.max(0,duration-.001F)));
                pose.getJointTransformData().putAll(Pose.interpolatePose(pose,source,weight).getJointTransformData());
                return;
            }
        }
        Selection selection = select(patch, partial);
        if (selection == null) return;
        boolean plated=selection.plated(), transition=selection.transition(), moving=selection.moving();
        Pose source=BlacklightEpicFightAnimations.utilityPose(selection.key()).get().getRawPose(selection.time());
        Pose blended = Pose.interpolatePose(pose, source, selection.weight());
        String[] joints = plated ? new String[]{"Torso","Chest","Head","Shoulder_R","Arm_R","Elbow_R","Hand_R","Shoulder_L","Arm_L","Elbow_L","Hand_L"}
                : new String[]{"Shoulder_L","Arm_L","Elbow_L","Hand_L","Tool_L"};
        if (plated && transition && !moving) pose.getJointTransformData().putAll(blended.getJointTransformData());
        else if (!plated || transition || !moving)
            for (String joint : joints) pose.getJointTransformData().put(joint, blended.orElseEmpty(joint).copy());
    }
}
