package com.codex.blacklight.integration.epicfight;

import com.codex.blacklight.movement.PrototypeLocomotion;
import com.codex.blacklight.movement.LocomotionDirection;
import com.codex.blacklight.movement.LocomotionPhase;
import com.codex.blacklight.movement.LocomotionVisualState;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.types.MovementAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/** Movement loop whose cadence follows actual traversal speed instead of vanilla speed. */
public final class PrototypeMovementAnimation extends MovementAnimation {
    public PrototypeMovementAnimation(
            boolean repeat,
            AnimationAccessor<? extends MovementAnimation> accessor,
            AssetAccessor<? extends Armature> armature) {
        super(0.18F, repeat, accessor, armature);
    }

    @Override
    public float getPlaySpeed(LivingEntityPatch<?> entityPatch, DynamicAnimation animation) {
        if (animation.isLinkAnimation()) return 1.0F;
        double horizontalSpeed = entityPatch.getOriginal().getDeltaMovement().horizontalDistance();
        return PrototypeLocomotion.playbackSpeed(horizontalSpeed);
    }

    @Override
    public void modifyPose(DynamicAnimation animation, Pose pose,
                           LivingEntityPatch<?> entityPatch, float time, float partialTicks) {
        super.modifyPose(animation, pose, entityPatch, time, partialTicks);
        LocomotionVisualState.Snapshot snapshot = LocomotionVisualState.get(
                entityPatch.getOriginal().getId());
        LocomotionDirection direction = snapshot.direction();
        LocomotionPhase phase = snapshot.phase();
        float angle = (float)direction.radians();
        float lateral = (float)Math.sin(angle);
        float backward = Math.max(0.0F, -(float)Math.cos(angle));
        float strideYaw = Math.max(-1.05F, Math.min(1.05F, angle * 0.55F));

        // Rotate the leg chains into the travel direction while keeping Root fixed in
        // world space. The authored gait phase is untouched, so sector changes cannot
        // restart or pop the feet back to frame zero.
        rotateY(pose, "Thigh_R", strideYaw);
        rotateY(pose, "Thigh_L", strideYaw);
        rotateZ(pose, "Torso", -lateral * 0.13F);
        rotateZ(pose, "Chest", -lateral * 0.08F);
        rotateZ(pose, "Head", lateral * 0.07F);
        rotateX(pose, "Torso", backward * 0.12F);

        if (phase == LocomotionPhase.START) {
            rotateX(pose, "Torso", -0.10F);
            rotateX(pose, "Chest", -0.06F);
        } else if (phase == LocomotionPhase.BRAKE) {
            rotateX(pose, "Torso", 0.12F);
            rotateX(pose, "Chest", 0.06F);
        } else if (phase == LocomotionPhase.TURN) {
            rotateZ(pose, "Torso", -Math.signum(lateral) * 0.16F);
            rotateY(pose, "Chest", -Math.signum(lateral) * 0.12F);
        }
    }

    private static void rotateX(Pose pose, String joint, float radians) {
        if (radians != 0.0F && pose.hasTransform(joint)) {
            pose.get(joint).rotation().rotateX(radians);
        }
    }

    private static void rotateY(Pose pose, String joint, float radians) {
        if (radians != 0.0F && pose.hasTransform(joint)) {
            pose.get(joint).rotation().rotateY(radians);
        }
    }

    private static void rotateZ(Pose pose, String joint, float radians) {
        if (radians != 0.0F && pose.hasTransform(joint)) {
            pose.get(joint).rotation().rotateZ(radians);
        }
    }
}
