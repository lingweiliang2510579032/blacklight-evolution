package com.codex.blacklight.integration.epicfight;

import java.util.function.Supplier;
import yesman.epicfight.api.animation.AnimationClip;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.types.MovementAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/**
 * A looping Blacklight living motion backed by a known-good Epic Fight clip.
 *
 * <p>The bundled wall-run JSON remains a resource-contract fallback, but a proven Epic
 * Fight movement clip is used at runtime so every transform matches the active biped
 * armature. The only additions here are cadence and a restrained forward posture.</p>
 */
public final class BorrowedMovementAnimation extends MovementAnimation {
    private static final String[] RUNNING_ARM_JOINTS = {
            "Shoulder_R", "Arm_R", "Elbow_R", "Hand_R", "Tool_R",
            "Shoulder_L", "Arm_L", "Elbow_L", "Hand_L", "Tool_L"
    };

    private final Supplier<AnimationAccessor<? extends StaticAnimation>> legSource;
    private final Supplier<AnimationAccessor<? extends StaticAnimation>> armSource;
    private AnimationClip runningArmClip;

    public BorrowedMovementAnimation(
            float transitionTime,
            AnimationAccessor<? extends MovementAnimation> accessor,
            AssetAccessor<? extends Armature> armature,
            Supplier<AnimationAccessor<? extends StaticAnimation>> legSource,
            Supplier<AnimationAccessor<? extends StaticAnimation>> armSource) {
        super(transitionTime, true, accessor, armature);
        this.legSource = legSource;
        this.armSource = armSource;
    }

    @Override
    public void loadAnimation() {
        super.loadAnimation();
        AnimationAccessor<? extends StaticAnimation> legs = legSource.get();
        AnimationAccessor<? extends StaticAnimation> arms = armSource.get();
        if (legs != null) this.animationClip = legs.get().getAnimationClip();
        if (arms != null) this.runningArmClip = arms.get().getAnimationClip();
    }

    @Override
    public Pose getPoseByTime(LivingEntityPatch<?> entityPatch, float time, float partialTicks) {
        float legDuration = Math.max(1.0E-4F, animationClip.getClipTime());
        float legTime = positiveModulo(time, legDuration);
        Pose pose = getRawPose(legTime);

        // Climbing provides a believable alternating foot plant against a vertical
        // surface. Replace its entire arm chains with the running cycle, preventing
        // either hand from touching the wall.
        if (runningArmClip != null) {
            float armDuration = Math.max(1.0E-4F, runningArmClip.getClipTime());
            Pose runningPose = runningArmClip.getPoseInTime(
                    legTime / legDuration * armDuration);
            copyJoints(runningPose, pose, RUNNING_ARM_JOINTS);
        }

        modifyPose(this, pose, entityPatch, time, partialTicks);
        return pose;
    }

    @Override
    public float getPlaySpeed(LivingEntityPatch<?> entityPatch, DynamicAnimation animation) {
        if (animation.isLinkAnimation()) return 1.0F;

        double verticalSpeed = Math.abs(entityPatch.getOriginal().getDeltaMovement().y);
        double horizontalSpeed = entityPatch.getOriginal().getDeltaMovement().horizontalDistance();
        // Prototype's wall run is a foot-driven sprint, not a slow ladder climb.
        return (float)Math.clamp(0.95 + verticalSpeed * 2.25 + horizontalSpeed * 0.65,
                1.05, 1.85);
    }

    @Override
    public void modifyPose(DynamicAnimation animation, Pose pose,
                           LivingEntityPatch<?> entityPatch, float time, float partialTicks) {
        super.modifyPose(animation, pose, entityPatch, time, partialTicks);
        // Keep the torso close to the wall while the arms retain their running swing.
        // The limb chains remain exactly as Epic Fight authored them.
        rotateX(pose, "Torso", -0.10F);
        rotateX(pose, "Chest", -0.05F);
        rotateX(pose, "Head", 0.09F);
    }

    private static void rotateX(Pose pose, String joint, float radians) {
        if (pose.hasTransform(joint)) pose.get(joint).rotation().rotateX(radians);
    }

    private static void copyJoints(Pose source, Pose target, String[] joints) {
        for (String joint : joints) {
            if (source.hasTransform(joint)) {
                target.putJointData(joint, source.get(joint).copy());
            }
        }
    }

    private static float positiveModulo(float value, float modulus) {
        float result = value % modulus;
        return result < 0.0F ? result + modulus : result;
    }
}
