package com.codex.blacklight.integration.epicfight;

import com.codex.blacklight.BlacklightNeoMod;
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
    private static final java.util.Map<com.codex.blacklight.combat.WeaponMove,AnimationAccessor<ActionAnimation>> WEAPONS=new java.util.EnumMap<>(com.codex.blacklight.combat.WeaponMove.class);
    private static final java.util.Map<com.codex.blacklight.combat.WeaponMove,AnimationAccessor<ActionAnimation>> CHARGES=new java.util.EnumMap<>(com.codex.blacklight.combat.WeaponMove.class);
    private static final java.util.Map<Integer,AnimationAccessor<StaticAnimation>> REACTIONS=new java.util.HashMap<>();
    public static AnimationAccessor<StaticAnimation> reaction(int id){return REACTIONS.get(id==com.codex.blacklight.combat.ImpactProfile.CLAW_RIP?com.codex.blacklight.combat.ImpactProfile.BACKHAND:id);}
    public static AnimationAccessor<ActionAnimation> weapon(com.codex.blacklight.combat.WeaponMove move){return WEAPONS.get(move);}
    public static AnimationAccessor<ActionAnimation> weaponCharge(com.codex.blacklight.combat.WeaponMove move){return CHARGES.get(move);}
    public static AnimationAccessor<MovementAnimation> CLAW_IDLE;
    public static AnimationAccessor<MovementAnimation> CLAW_WALK;
    public static AnimationAccessor<MovementAnimation> CLAW_SPRINT;
    public static AnimationAccessor<ActionAnimation> CHARGED_JUMP;
    public static AnimationAccessor<MovementAnimation> JUMP_RISE;
    public static AnimationAccessor<MovementAnimation> JUMP_FALL;
    public static AnimationAccessor<MovementAnimation> WALL_RUN;
    public static AnimationAccessor<MovementAnimation> WALL_CLIMB;
    public static AnimationAccessor<MovementAnimation> WALL_CLING;
    public static AnimationAccessor<MovementAnimation> WALL_LEAP;
    public static AnimationAccessor<MovementAnimation> WALL_MANTLE,WALL_VAULT;
    public static AnimationAccessor<MovementAnimation> GLIDE;
    public static AnimationAccessor<ActionAnimation> TRANSFORM_CLAWS;
    public static AnimationAccessor<ActionAnimation> CLAW_AUTO_1;
    public static AnimationAccessor<ActionAnimation> CLAW_AUTO_2;
    public static AnimationAccessor<ActionAnimation> CLAW_AUTO_3;
    public static AnimationAccessor<ActionAnimation> CLAW_GROUNDSPIKE;
    public static AnimationAccessor<ActionAnimation> CLAW_SPRINT_ATTACK;
    public static AnimationAccessor<ActionAnimation> CLAW_DASHING_SLICE;
    public static AnimationAccessor<ActionAnimation> GRAB_REACH, GRAB_WHIP, GRAB_THROW;
    public static AnimationAccessor<ActionAnimation> GRAB_BLADE, GRAB_BLADE_THROW;
    public static AnimationAccessor<MovementAnimation> GRAB_HOLD, GRAB_BLADE_HOLD;
    public static AnimationAccessor<MovementAnimation> grabHold(com.codex.blacklight.power.PowerForm form) {
        return form==com.codex.blacklight.power.PowerForm.BLADE?GRAB_BLADE_HOLD:GRAB_HOLD;
    }
    private static final java.util.Map<String,AnimationAccessor<StaticAnimation>> CONSUME_POSES=new java.util.HashMap<>();
    public static AnimationAccessor<StaticAnimation> consumePose(String key){return CONSUME_POSES.get(key);}
    public static AnimationAccessor<ActionAnimation> CONSUME_LARGE, CONSUME_MUSCLE;
    public static AnimationAccessor<ActionAnimation> CONSUME_LARGE_HAMMER,CONSUME_LARGE_MUSCLE,CONSUME_LARGE_WHIP;
    public static AnimationAccessor<ActionAnimation> consumeLarge(com.codex.blacklight.power.PowerForm form){
        return switch(form){case HAMMERFISTS->CONSUME_LARGE_HAMMER;case MUSCLE_MASS->CONSUME_LARGE_MUSCLE;case WHIPFIST->CONSUME_LARGE_WHIP;case CLAWS->CONSUME;case BLADE->CONSUME_BLADE;default->CONSUME_LARGE;};
    }
    public static AnimationAccessor<ActionAnimation> CONSUME;
    public static AnimationAccessor<ActionAnimation> CONSUME_NORMAL, CONSUME_HAMMER, CONSUME_BLADE, CONSUME_WHIP;
    public static AnimationAccessor<ActionAnimation> consume(com.codex.blacklight.power.PowerForm form) {
        return switch (form) {
            case CLAWS -> CONSUME; case HAMMERFISTS -> CONSUME_HAMMER;
            case BLADE -> CONSUME_BLADE; case WHIPFIST -> CONSUME_WHIP; case MUSCLE_MASS -> CONSUME_MUSCLE; default -> CONSUME_NORMAL;
        };
    }
    public static AnimationAccessor<ActionAnimation> AIR_DASH;
    public static AnimationAccessor<ActionAnimation> AIR_DASH_BRAKE, LAND_MOVING;
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
        NativeTraversalLibrary.requireInstalled();
        CONSUME_POSES.clear();
        for(String form:new String[]{"normal","claws","hammer","blade","whip","muscle","large","large_hammer","large_muscle","large_whip","capture","hold","carry_walk","carry_run"})
            for(String role:new String[]{"actor","victim"}) {
                String key="consume_"+form+"_"+role;
                if(com.codex.blacklight.combat.LocalVisualMotion.find(key).isPresent())
                    CONSUME_POSES.put(key,builder.nextAccessor("biped/consume_pose/"+key,a->new NativeHitAnimation(a,key)));
            }
        REACTIONS.clear();
        for(int i=0;i<NativeHitAnimation.KEYS.length;i++){
            String key=NativeHitAnimation.KEYS[i];
            if(com.codex.blacklight.combat.LocalVisualMotion.find(key).isPresent())
                REACTIONS.put(i,builder.nextAccessor("biped/reaction/"+key,a->new NativeHitAnimation(a,key)));
        }
        CHARGES.clear();
        for (var move : com.codex.blacklight.combat.WeaponMove.values()) {
            var charge = com.codex.blacklight.combat.LocalWeaponMotion.find(move.id)
                    .flatMap(com.codex.blacklight.combat.LocalWeaponMotion.Definition::charge);
            if (charge.isPresent()) {
                // Required when declared: never show a fallback attack as a held charge.
                if (com.codex.blacklight.combat.LocalVisualMotion.find(charge.get().animation()).isEmpty())
                    throw new IllegalStateException("Missing local charge animation: " + charge.get().animation());
                CHARGES.put(move, builder.nextAccessor("biped/charge/" + move.id,
                        accessor -> new LocalWeaponChargeAnimation(accessor, move)));
            }
        }
        for(var move:com.codex.blacklight.combat.WeaponMove.values())WEAPONS.put(move,builder.nextAccessor("biped/combat/"+move.id,accessor->
                com.codex.blacklight.combat.LocalWeaponMotion.find(move.id).isPresent()?new LocalWeaponAnimation(accessor,move):
                com.codex.blacklight.combat.WeaponMotionProfile.forMove(move)!=null?new AdaptedWeaponAnimation(accessor,move):
                move==com.codex.blacklight.combat.WeaponMove.HAMMER_DROP||move==com.codex.blacklight.combat.WeaponMove.BLADE_DIVE?new AirWeaponActionAnimation(accessor,move):new ActionAnimation(.06F,accessor,Armatures.BIPED)));
        CLAW_IDLE = builder.nextAccessor("biped/living/claw_idle",
                accessor -> new NativeTraversalMovement(accessor,NativeTraversalMovement.Family.IDLE));
        CLAW_WALK = builder.nextAccessor("biped/living/claw_walk",
                accessor -> new NativeTraversalMovement(accessor,NativeTraversalMovement.Family.WALK));
        CLAW_SPRINT = builder.nextAccessor("biped/living/claw_sprint",
                accessor -> new NativeTraversalMovement(accessor,NativeTraversalMovement.Family.RUN));
        // The charged-jump crouch is a real ActionAnimation, not a base-layer static
        // clip: Epic Fight's locomotion tick leaves actions alone, so the held crouch
        // can no longer be stolen by IDLE the way the old StaticAnimation was.
        CHARGED_JUMP = builder.nextAccessor("biped/living/prototype_jump_charge",
                accessor -> new NativeTraversalAction(accessor,"basic_jump_charge"));
        JUMP_RISE = builder.nextAccessor("biped/living/prototype_jump_rise",
                accessor -> new NativeTraversalMovement(accessor,NativeTraversalMovement.Family.RISE));
        JUMP_FALL = builder.nextAccessor("biped/living/prototype_jump_fall",
                accessor -> new NativeTraversalMovement(accessor,NativeTraversalMovement.Family.FALL));
        WALL_RUN = builder.nextAccessor("biped/living/wall_run",
                accessor -> new NativeTraversalMovement(accessor,NativeTraversalMovement.Family.WALL_RUN));
        WALL_CLIMB = builder.nextAccessor("biped/living/wall_climb",
                accessor -> new NativeTraversalMovement(accessor,NativeTraversalMovement.Family.WALL_CLIMB));
        WALL_CLING = builder.nextAccessor("biped/living/wall_cling",
                accessor -> new NativeTraversalMovement(accessor,NativeTraversalMovement.Family.WALL_CLING));
        WALL_LEAP = builder.nextAccessor("biped/living/wall_leap",
                accessor -> new NativeTraversalMovement(accessor,NativeTraversalMovement.Family.WALL_LEAP));
        WALL_MANTLE=builder.nextAccessor("biped/living/wall_mantle",accessor -> new NativeTraversalMovement(accessor,NativeTraversalMovement.Family.WALL_MANTLE));
        WALL_VAULT=builder.nextAccessor("biped/living/wall_vault",accessor -> new NativeTraversalMovement(accessor,NativeTraversalMovement.Family.WALL_VAULT));
        // The glide is a sustained state, so it lives on the living-motion system as
        // a looping clip (FLY is unreachable in Prototype mode: elytra use is locked).
        GLIDE = builder.nextAccessor("biped/living/prototype_glide",
                accessor -> new NativeTraversalMovement(accessor,NativeTraversalMovement.Family.GLIDE));
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

        GRAB_BLADE = builder.nextAccessor("biped/action/grab_blade", accessor -> new ActionAnimation(.06F,accessor,Armatures.BIPED));
        GRAB_BLADE_THROW = builder.nextAccessor("biped/action/grab_blade_throw", CarryThrowAnimation::new);
        GRAB_BLADE_HOLD = builder.nextAccessor("biped/action/grab_blade_hold", accessor -> new MovementAnimation(.12F,true,accessor,Armatures.BIPED));
        GRAB_REACH = builder.nextAccessor("biped/action/grab_reach", accessor -> new ActionAnimation(.08F,accessor,Armatures.BIPED));
        GRAB_WHIP = builder.nextAccessor("biped/action/grab_whip", NativeWhipGrabAnimation::new);
        GRAB_THROW = builder.nextAccessor("biped/action/grab_throw", CarryThrowAnimation::new);
        GRAB_HOLD = builder.nextAccessor("biped/action/grab_hold", accessor -> new MovementAnimation(.12F,true,accessor,Armatures.BIPED));
        CONSUME = builder.nextAccessor("biped/action/consume",
                accessor -> new NativeConsumeAnimation(accessor,"consume_claws_actor"));
        CONSUME_NORMAL = builder.nextAccessor("biped/action/consume_normal", accessor -> new NativeConsumeAnimation(accessor,"consume_normal_actor"));
        CONSUME_HAMMER = builder.nextAccessor("biped/action/consume_hammer", accessor -> new NativeConsumeAnimation(accessor,"consume_hammer_actor"));
        CONSUME_BLADE = builder.nextAccessor("biped/action/consume_blade", accessor -> new NativeConsumeAnimation(accessor,"consume_blade_actor"));
        CONSUME_WHIP = builder.nextAccessor("biped/action/consume_whip", accessor -> new NativeConsumeAnimation(accessor,"consume_whip_actor"));
        CONSUME_MUSCLE = builder.nextAccessor("biped/action/consume_normal_muscle", accessor -> new NativeConsumeAnimation(accessor,"consume_muscle_actor"));
        CONSUME_LARGE = builder.nextAccessor("biped/action/consume_large", accessor -> new NativeConsumeAnimation(accessor,"consume_large_actor"));
        CONSUME_LARGE_HAMMER = builder.nextAccessor("biped/action/consume_large_hammer", accessor -> new NativeConsumeAnimation(accessor,"consume_large_hammer_actor"));
        CONSUME_LARGE_MUSCLE = builder.nextAccessor("biped/action/consume_large_muscle", accessor -> new NativeConsumeAnimation(accessor,"consume_large_muscle_actor"));
        CONSUME_LARGE_WHIP = builder.nextAccessor("biped/action/consume_large_whip", accessor -> new NativeConsumeAnimation(accessor,"consume_large_whip_actor"));
        AIR_DASH = builder.nextAccessor("biped/action/air_dash",
                accessor -> new NativeTraversalAction(accessor,"basic_air_dash"));
        AIR_DASH_BRAKE=builder.nextAccessor("biped/action/air_dash_brake",accessor -> new NativeTraversalAction(accessor,"basic_air_dash_brake"));
        LAND_MOVING=builder.nextAccessor("biped/living/landing_moving",accessor -> new NativeTraversalAction(accessor,"basic_land_moving"));
        LAND_LIGHT = builder.nextAccessor("biped/living/landing_light",
                accessor -> new NativeTraversalAction(accessor,"basic_land_light"));
        LAND_HEAVY = builder.nextAccessor("biped/living/landing_heavy",
                accessor -> new NativeTraversalAction(accessor,"basic_land_heavy"));
        LAND_DEVASTATING = builder.nextAccessor("biped/living/landing_devastating",
                accessor -> new NativeTraversalAction(accessor,"basic_land_devastating"));
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
