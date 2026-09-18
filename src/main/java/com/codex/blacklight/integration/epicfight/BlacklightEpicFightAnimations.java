package com.codex.blacklight.integration.epicfight;

import com.codex.blacklight.BlacklightNeoMod;
import com.codex.blacklight.client.tuning.AnimationPoseSettings;
import com.codex.blacklight.combat.ClawCombatState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.ActionAnimation;
import yesman.epicfight.api.animation.types.MovementAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.client.animation.Layer;
import yesman.epicfight.api.client.animation.property.ClientAnimationProperties;
import yesman.epicfight.gameasset.Armatures;


/**
 * Epic Fight owns the player armature and action timing. GeckoLib is deliberately kept out
 * of this class and renders only the biological form overlay.
 */
@EventBusSubscriber(modid = BlacklightNeoMod.MOD_ID)
public final class BlacklightEpicFightAnimations {
    public static AnimationAccessor<MovementAnimation> CLAW_IDLE;
    public static AnimationAccessor<MovementAnimation> CLAW_WALK;
    public static AnimationAccessor<MovementAnimation> CLAW_SPRINT;
    public static AnimationAccessor<ActionAnimation> CHARGED_JUMP;
    public static AnimationAccessor<StaticAnimation> JUMP_RISE;
    public static AnimationAccessor<StaticAnimation> JUMP_FALL;
    public static AnimationAccessor<MovementAnimation> WALL_RUN;
    public static AnimationAccessor<MovementAnimation> WALL_CLIMB;
    public static AnimationAccessor<MovementAnimation> WALL_CLING;
    public static AnimationAccessor<MovementAnimation> WALL_LEAP;
    public static AnimationAccessor<MovementAnimation> GLIDE;
    public static AnimationAccessor<ActionAnimation> TRANSFORM_CLAWS;
    public static AnimationAccessor<ActionAnimation> CLAW_AUTO_1;
    public static AnimationAccessor<ActionAnimation> CLAW_AUTO_2;
    public static AnimationAccessor<ActionAnimation> CLAW_AUTO_3;
    public static AnimationAccessor<ActionAnimation> CLAW_GROUNDSPIKE;
    public static AnimationAccessor<ActionAnimation> CLAW_SPRINT_ATTACK;
    public static AnimationAccessor<ActionAnimation> CLAW_DASHING_SLICE;
    public static AnimationAccessor<ActionAnimation> CONSUME;
    public static AnimationAccessor<ActionAnimation> CONSUME_NORMAL, CONSUME_HAMMER, CONSUME_BLADE, CONSUME_WHIP;
    public static AnimationAccessor<ActionAnimation> consume(com.codex.blacklight.power.PowerForm form) {
        return switch (form) {
            case CLAWS -> CONSUME; case HAMMERFISTS -> CONSUME_HAMMER;
            case BLADE -> CONSUME_BLADE; case WHIPFIST -> CONSUME_WHIP; default -> CONSUME_NORMAL;
        };
    }
    public static AnimationAccessor<ActionAnimation> AIR_DASH;
    public static AnimationAccessor<ActionAnimation> LAND_LIGHT;
    public static AnimationAccessor<ActionAnimation> LAND_HEAVY;
    public static AnimationAccessor<ActionAnimation> LAND_DEVASTATING;

    private BlacklightEpicFightAnimations() {
    }

    @SubscribeEvent
    public static void registerAnimations(AnimationManager.AnimationRegistryEvent event) {
        event.newBuilder(BlacklightNeoMod.MOD_ID, BlacklightEpicFightAnimations::build);
    }

    private static void build(AnimationManager.AnimationBuilder builder) {
        CLAW_IDLE = builder.nextAccessor("biped/living/claw_idle",
                accessor -> new MovementAnimation(0.18F, true, accessor, Armatures.BIPED));
        CLAW_WALK = builder.nextAccessor("biped/living/claw_walk",
                accessor -> new PrototypeMovementAnimation(true, accessor, Armatures.BIPED));
        CLAW_SPRINT = builder.nextAccessor("biped/living/claw_sprint",
                accessor -> new PrototypeMovementAnimation(true, accessor, Armatures.BIPED));
        // The charged-jump crouch is a real ActionAnimation, not a base-layer static
        // clip: Epic Fight's locomotion tick leaves actions alone, so the held crouch
        // can no longer be stolen by IDLE the way the old StaticAnimation was.
        CHARGED_JUMP = builder.nextAccessor("biped/living/prototype_jump_charge",
                accessor -> new TunableActionAnimation(0.10F, accessor, Armatures.BIPED,
                        AnimationPoseSettings.Action.CHARGED_JUMP));
        JUMP_RISE = builder.nextAccessor("biped/living/prototype_jump_rise",
                accessor -> new StaticAnimation(0.08F, false, accessor, Armatures.BIPED));
        JUMP_FALL = builder.nextAccessor("biped/living/prototype_jump_fall",
                accessor -> new StaticAnimation(0.08F, false, accessor, Armatures.BIPED));
        WALL_RUN = builder.nextAccessor("biped/living/wall_run",
                accessor -> new WallMovementAnimation(true,.55,accessor));
        WALL_CLIMB = builder.nextAccessor("biped/living/wall_climb",
                accessor -> new WallMovementAnimation(true,.14,accessor));
        WALL_CLING = builder.nextAccessor("biped/living/wall_cling",
                accessor -> new WallMovementAnimation(true,0,accessor));
        WALL_LEAP = builder.nextAccessor("biped/living/wall_leap",
                accessor -> new WallMovementAnimation(false,0,accessor));
        // The glide is a sustained state, so it lives on the living-motion system as
        // a looping clip (FLY is unreachable in Prototype mode: elytra use is locked).
        GLIDE = builder.nextAccessor("biped/living/prototype_glide",
                accessor -> new MovementAnimation(0.14F, true, accessor, Armatures.BIPED));
        TRANSFORM_CLAWS = builder.nextAccessor("biped/action/transform_claws",
                accessor -> new MorphActionAnimation(accessor));

        // Damage is resolved exactly once by ClawCombatSystem from the tick timing
        // contract. These are visual actions, preventing Epic Fight from applying a
        // second collider-driven hit with client-dependent timing. Normal, walking and
        // running attacks use the authored claw pose. PrototypeComboActionAnimation
        // supplies a separate Epic Fight COORD trajectory, leaving the visual Root in place.
        CLAW_AUTO_1 = builder.nextAccessor("biped/combat/claw_auto1",
                accessor -> authoredCombo(0.06F, accessor,
                        ClawCombatState.Action.AUTO_1));
        CLAW_AUTO_2 = builder.nextAccessor("biped/combat/claw_auto2",
                accessor -> authoredCombo(0.06F, accessor,
                        ClawCombatState.Action.AUTO_2));
        CLAW_AUTO_3 = builder.nextAccessor("biped/combat/claw_auto3",
                accessor -> authoredCombo(0.08F, accessor,
                        ClawCombatState.Action.AUTO_3));
        CLAW_GROUNDSPIKE = builder.nextAccessor("biped/combat/claw_groundspike",
                accessor -> new GroundSpikeActionAnimation(0.12F, accessor, Armatures.BIPED));
        CLAW_SPRINT_ATTACK = builder.nextAccessor("biped/combat/claw_sprint_attack",
                accessor -> compositeAction(0.10F, accessor));
        CLAW_DASHING_SLICE = builder.nextAccessor("biped/combat/claw_dashing_slice",
                accessor -> new PrototypeFreeActionAnimation(0.06F, accessor, Armatures.BIPED));

        CONSUME = builder.nextAccessor("biped/action/consume",
                accessor -> new ActionAnimation(0.16F, accessor, Armatures.BIPED));
        CONSUME_NORMAL = builder.nextAccessor("biped/action/consume_normal", accessor -> new ActionAnimation(.12F, accessor, Armatures.BIPED));
        CONSUME_HAMMER = builder.nextAccessor("biped/action/consume_hammer", accessor -> new ActionAnimation(.12F, accessor, Armatures.BIPED));
        CONSUME_BLADE = builder.nextAccessor("biped/action/consume_blade", accessor -> new ActionAnimation(.10F, accessor, Armatures.BIPED));
        CONSUME_WHIP = builder.nextAccessor("biped/action/consume_whip", accessor -> new ActionAnimation(.10F, accessor, Armatures.BIPED));
        AIR_DASH = builder.nextAccessor("biped/action/air_dash",
                accessor -> new PrototypeFreeActionAnimation(0.06F, accessor, Armatures.BIPED));
        LAND_LIGHT = builder.nextAccessor("biped/living/landing_light",
                accessor -> new ActionAnimation(0.06F, accessor, Armatures.BIPED));
        LAND_HEAVY = builder.nextAccessor("biped/living/landing_heavy",
                accessor -> new ActionAnimation(0.06F, accessor, Armatures.BIPED));
        LAND_DEVASTATING = builder.nextAccessor("biped/living/landing_devastating",
                accessor -> new ActionAnimation(0.06F, accessor, Armatures.BIPED));
    }

    private static ActionAnimation compositeAction(
            float transitionTime,
            AnimationAccessor<? extends ActionAnimation> accessor) {
        ActionAnimation animation = new PrototypeFreeActionAnimation(transitionTime, accessor, Armatures.BIPED);
        if (FMLEnvironment.dist.isClient()) {
            animation.addProperty(ClientAnimationProperties.LAYER_TYPE,
                            Layer.LayerType.COMPOSITE_LAYER)
                    .addProperty(ClientAnimationProperties.PRIORITY, Layer.Priority.HIGHEST)
                    .addProperty(ClientAnimationProperties.JOINT_MASK,
                            PrototypeComboActionAnimation.movementAwareClawMask());
        }
        return animation;
    }

    private static ActionAnimation authoredCombo(
            float transitionTime,
            AnimationAccessor<? extends ActionAnimation> accessor,
            ClawCombatState.Action action) {
        return new PrototypeComboActionAnimation(
                transitionTime, accessor, Armatures.BIPED, action);
    }
}
