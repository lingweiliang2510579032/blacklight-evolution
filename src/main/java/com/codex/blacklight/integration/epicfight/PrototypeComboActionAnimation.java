package com.codex.blacklight.integration.epicfight;

import com.codex.blacklight.BlacklightNeoMod;
import com.codex.blacklight.combat.ClawCombatState;
import com.codex.blacklight.combat.ClawAttackMotion;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLEnvironment;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.animation.JointTransform;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.client.animation.property.JointMask;
import yesman.epicfight.api.animation.Keyframe;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.animation.TransformSheet;
import yesman.epicfight.api.animation.property.AnimationProperty.ActionAnimationProperty;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.api.animation.types.ActionAnimation;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.animation.Layer;
import yesman.epicfight.api.client.animation.property.ClientAnimationProperties;
import yesman.epicfight.api.client.animation.property.JointMaskEntry;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.TimePairList;
import yesman.epicfight.api.utils.datastructure.ParameterizedHashMap;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/**
 * Full-body claw swing with additive Epic Fight action movement.
 *
 * <p>The visual Root remains in-place. A separate COORD sheet drives the local
 * player through Epic Fight's collision-aware action movement and its own
 * bidirectional position synchronization, while movement input stays unlocked.</p>
 */
public final class PrototypeComboActionAnimation extends ActionAnimation {
    private static final float SECONDS_PER_TICK = 0.05F;
    private final ClawCombatState.Action action;

    public PrototypeComboActionAnimation(
            float transitionTime,
            AnimationAccessor<? extends ActionAnimation> accessor,
            AssetAccessor<? extends Armature> armature,
            ClawCombatState.Action action) {
        super(transitionTime, accessor, armature);
        this.action = action;
        this.addProperty(ActionAnimationProperty.COORD, createMovementCoord(action));
        this.addProperty(ActionAnimationProperty.MOVE_TIME, TimePairList.create(
                0.0F, ClawAttackMotion.movementEndTick(action) * SECONDS_PER_TICK));
        this.addProperty(ActionAnimationProperty.MOVE_ON_LINK, false);
        this.addProperty(ActionAnimationProperty.MOVE_VERTICAL, false);
        // Stationary attacks use the complete baked stance; WALK/RUN retain their live foot cycle.
        // This is the same separation used by Epic Fight's non-stiff combo attacks:
        // the lower body retains its live foot cycle while torso/arms perform the hit.
        // COORD still belongs to this action, so the visible lunge continues to move
        // the actual player instead of becoming an in-place upper-body gesture.
        if (FMLEnvironment.dist.isClient()) {
            this.addProperty(ClientAnimationProperties.LAYER_TYPE,
                    Layer.LayerType.COMPOSITE_LAYER);
            this.addProperty(ClientAnimationProperties.PRIORITY, Layer.Priority.HIGHEST);
            this.addProperty(ClientAnimationProperties.JOINT_MASK,
                    movementAwareClawMask());
        }
    }

    /** Preserve planted full-body actions at rest and live foot cycles while moving. */
    static JointMaskEntry movementAwareClawMask() {
        return JointMaskEntry.builder()
                            .defaultMask(JointMask.JointMaskSet.of(java.util.Set.of(
                                    JointMask.of("Root"), JointMask.of("Torso"), JointMask.of("Chest"), JointMask.of("Head"),
                                    JointMask.of("Shoulder_R"), JointMask.of("Arm_R"), JointMask.of("Elbow_R"), JointMask.of("Hand_R"), JointMask.of("Tool_R"),
                                    JointMask.of("Shoulder_L"), JointMask.of("Arm_L"), JointMask.of("Elbow_L"), JointMask.of("Hand_L"), JointMask.of("Tool_L"),
                                    JointMask.of("Thigh_R"), JointMask.of("Leg_R"), JointMask.of("Knee_R"),
                                    JointMask.of("Thigh_L"), JointMask.of("Leg_L"), JointMask.of("Knee_L"))))
                            .mask(LivingMotions.WALK, JointMaskEntry.BIPED_UPPER_JOINTS_WITH_ROOT)
                            .mask(LivingMotions.RUN, JointMaskEntry.BIPED_UPPER_JOINTS_WITH_ROOT)
                            .mask(LivingMotions.SNEAK, JointMaskEntry.BIPED_UPPER_JOINTS_WITH_ROOT)
                            .create();
    }

    @Override
    public void modifyPose(DynamicAnimation animation, Pose pose,
                           LivingEntityPatch<?> entityPatch, float time, float partialTicks) {
        super.modifyPose(animation, pose, entityPatch, time, partialTicks);

        // COORD is deliberately independent from the authored Root.
        // Removing horizontal Root translation prevents the mesh from sliding twice
        // while the entity itself follows the action coordinate sheet.
        if (pose.hasTransform("Root")) {
            Vec3f rootTranslation = pose.get("Root").translation();
            rootTranslation.x = 0.0F;
            rootTranslation.z = 0.0F;
        }

    }

    @Override
    public ParameterizedHashMap<EntityState.StateFactor<?>> getStatesMap(
            LivingEntityPatch<?> entityPatch, float time) {
        ParameterizedHashMap<EntityState.StateFactor<?>> states =
                super.getStatesMap(entityPatch, time);
        // The authored attack stays on the composite layer while WASD/sprint remains
        // live. Its authored lunge is added to the player's existing locomotion.
        states.put(EntityState.MOVEMENT_LOCKED, false);
        // The attack now occupies the masked composite layer, so living motion is
        // expected to keep advancing underneath it. This preserves foot phase and
        // permits WALK <-> RUN changes without restarting the claw swing.
        states.put(EntityState.UPDATE_LIVING_MOTION, true);
        return states;
    }

    @Override
    protected Vec3 getCoordVector(LivingEntityPatch<?> entityPatch,
                                  AssetAccessor<? extends DynamicAnimation> animation) {
        Vec3 movement = super.getCoordVector(entityPatch, animation);
        AnimationPlayer player = entityPatch.getAnimator().getPlayerFor(animation);
        float elapsed = player == null ? 0.0F : player.getElapsedTime();
        if (movement.horizontalDistanceSqr() > 1.0E-8D && elapsed <= 0.11F) {
            BlacklightNeoMod.LOGGER.debug(
                    "Epic Fight claw coord {} elapsed={} movement=({}, {})",
                    action, elapsed, movement.x, movement.z);
        }
        return movement;
    }

    private static TransformSheet createMovementCoord(ClawCombatState.Action action) {
        int endTick = ClawAttackMotion.movementEndTick(action);
        List<Keyframe> frames = new ArrayList<>(Math.max(2, endTick + 1));
        frames.add(new Keyframe(0.0F, JointTransform.empty()));

        double distance = 0.0D;
        for (int tick = 1; tick < endTick; tick++) {
            distance += ClawAttackMotion.forwardStep(action, tick);
            frames.add(new Keyframe(tick * SECONDS_PER_TICK,
                    JointTransform.translation(new Vec3f(0.0F, 0.0F,
                            (float)-distance))));
        }

        // Keep a stable terminal key even if a future profile has no movement.
        frames.add(new Keyframe(endTick * SECONDS_PER_TICK,
                JointTransform.translation(new Vec3f(0.0F, 0.0F,
                        (float)-ClawAttackMotion.totalDistance(action)))));
        return new TransformSheet(frames);
    }

}
