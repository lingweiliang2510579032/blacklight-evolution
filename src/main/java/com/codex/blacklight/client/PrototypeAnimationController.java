package com.codex.blacklight.client;

import com.codex.blacklight.BlacklightNeoMod;
import com.codex.blacklight.movement.LandingImpact;
import com.codex.blacklight.movement.WallTraversal;
import com.codex.blacklight.movement.LocomotionDirection;
import com.codex.blacklight.movement.LocomotionPhase;
import com.codex.blacklight.movement.LocomotionVisualState;
import com.codex.blacklight.movement.PrototypeLocomotion;
import com.codex.blacklight.registry.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Per-player visual state shared by living-motion selection and procedural gait posing.
 * No world movement is performed here.
 */
public final class PrototypeAnimationController {
    private static final double MOVING_SPEED_SQR = 0.018D * 0.018D;
    private static final double RUN_SPEED = PrototypeLocomotion.RUN_ANIMATION_SPEED;
    private static final int START_TICKS = 3;
    private static final int BRAKE_TICKS = 4;
    private static final int TURN_TICKS = 4;
    private static final Map<AbstractClientPlayer, RuntimeState> STATES = new WeakHashMap<>();

    private PrototypeAnimationController() {
    }

    public static void tickLevel(Minecraft minecraft, boolean localCharging,
                                 boolean localMovingIntent, boolean localSprintIntent) {
        if (minecraft.level == null) {
            STATES.clear();
            LocomotionVisualState.clear();
            return;
        }
        STATES.keySet().removeIf(player -> {
            boolean remove = player.isRemoved() || player.level() != minecraft.level;
            if (remove) LocomotionVisualState.remove(player.getId());
            return remove;
        });
        for (AbstractClientPlayer player : minecraft.level.players()) {
            var blacklight = player.getData(ModAttachments.PLAYER_STATE);
            if (!blacklight.isInfected() || !blacklight.isPrototypeMode()) {
                STATES.remove(player);
                LocomotionVisualState.remove(player.getId());
                continue;
            }
            boolean local = player == minecraft.player;
            RuntimeState runtime = STATES.computeIfAbsent(player, ignored -> new RuntimeState());
            WallTraversal.Mode wallMode=ClientWallMotion.modeFor(player);
            boolean motionChanged = runtime.update(player,
                    local && localCharging,
                    local && localMovingIntent,
                    local && localSprintIntent,
                    wallMode);
            LocomotionVisualState.set(player.getId(), runtime.direction, runtime.phase);
            // At full jump charge ClientMovementEvents owns the exact final frame.
            // Re-evaluating KNEEL here would construct/restart the living animation
            // underneath that pause and make a long hold visibly loop.
            boolean finalChargeLatched = local
                    && ClientMovementEvents.isHoldingFinalChargePose();
            AbstractClientPlayerPatch<?> patch = EpicFightCapabilities.getEntityPatch(
                    player, AbstractClientPlayerPatch.class);
            if (motionChanged) runtime.motionRefreshPending = true;
            // Normal claw combos live on Epic Fight's masked composite layer. Updating
            // WALK/RUN here therefore changes only the underlying locomotion and can no
            // longer replace or restart the attack player. During the planted groundspike
            // action every motion refresh must be suppressed entirely so the base layer
            // keeps playing the insertion animation. The same applies while a charged
            // jump is being held: the KNEEL clip plays once and must not be restarted.
            if (runtime.motionRefreshPending && !finalChargeLatched && patch != null
                    && !ClientClawEvents.isGroundSpikeActionActive()
                    && !ClientMovementEvents.isChargingJump()) {
                patch.updateMotion(true);
                runtime.motionRefreshPending = false;
            }
        }
    }

    public static LocomotionDirection directionFor(Entity entity) {
        if (entity instanceof AbstractClientPlayer player) {
            RuntimeState state = STATES.get(player);
            if (state != null) return state.direction;
        }
        return LocomotionDirection.FRONT;
    }

    public static LocomotionPhase phaseFor(Entity entity) {
        if (entity instanceof AbstractClientPlayer player) {
            RuntimeState state = STATES.get(player);
            if (state != null) return state.phase;
        }
        return LocomotionPhase.IDLE;
    }

    public static PrototypeLocomotion.Gait gaitFor(Entity entity) {
        LocomotionPhase phase = phaseFor(entity);
        return switch (phase) {
            case CHARGING -> PrototypeLocomotion.Gait.CHARGING;
            case RUN -> PrototypeLocomotion.Gait.RUN;
            case WALL_RUN -> PrototypeLocomotion.Gait.RUN;
            case START, WALK, BRAKE, TURN -> PrototypeLocomotion.Gait.WALK;
            default -> PrototypeLocomotion.Gait.IDLE;
        };
    }

    public static void acceptLanding(int entityId, LandingImpact.Strength strength) {
        if (strength == null || strength == LandingImpact.Strength.NONE) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        Entity entity = minecraft.level.getEntity(entityId);
        if (!(entity instanceof AbstractClientPlayer player)) return;
        RuntimeState runtime = STATES.computeIfAbsent(player, ignored -> new RuntimeState());
        runtime.landing = strength;
        runtime.landingTicks = switch (strength) {
            case LIGHT -> 4;
            case HEAVY -> 7;
            case DEVASTATING -> 10;
            default -> 0;
        };
        runtime.phase = landingPhase(strength);
        AbstractClientPlayerPatch<?> patch = EpicFightCapabilities.getEntityPatch(
                player, AbstractClientPlayerPatch.class);
        if (patch != null) patch.updateMotion(true);
    }

    private static LocomotionPhase landingPhase(LandingImpact.Strength strength) {
        return switch (strength) {
            case LIGHT -> LocomotionPhase.LAND_LIGHT;
            case HEAVY -> LocomotionPhase.LAND_HEAVY;
            case DEVASTATING -> LocomotionPhase.LAND_DEVASTATING;
            default -> LocomotionPhase.IDLE;
        };
    }

    private static final class RuntimeState {
        private final LocomotionDirection.Stabilizer directionStabilizer =
                new LocomotionDirection.Stabilizer();
        private final PrototypeLocomotion.Stabilizer gaitStabilizer =
                new PrototypeLocomotion.Stabilizer();
        private LocomotionDirection direction = LocomotionDirection.FRONT;
        private LocomotionPhase phase = LocomotionPhase.IDLE;
        private PrototypeLocomotion.Gait gait = PrototypeLocomotion.Gait.IDLE;
        private LandingImpact.Strength landing = LandingImpact.Strength.NONE;
        private int landingTicks;
        private int transitionTicks;
        private LocomotionPhase transitionPhase = LocomotionPhase.IDLE;
        private boolean moving;
        private boolean motionRefreshPending;

        private boolean update(AbstractClientPlayer player, boolean charging,
                               boolean movingIntent, boolean sprintIntent,
                               WallTraversal.Mode wallMode) {
            LocomotionPhase previousPhase = phase;
            Vec3 velocity = player.getDeltaMovement();
            double horizontalSqr = velocity.x * velocity.x + velocity.z * velocity.z;
            boolean nowMoving = movingIntent || horizontalSqr >= MOVING_SPEED_SQR;
            double yaw = Math.toRadians(player.yBodyRot);
            double forwardX = -Math.sin(yaw);
            double forwardZ = Math.cos(yaw);
            double rightX = Math.cos(yaw);
            double rightZ = Math.sin(yaw);
            double localForward = velocity.x * forwardX + velocity.z * forwardZ;
            double localRight = velocity.x * rightX + velocity.z * rightZ;
            LocomotionDirection previousDirection = direction;
            if (nowMoving) direction = directionStabilizer.update(localForward, localRight);
            boolean hardTurn = nowMoving && moving
                    && LocomotionDirection.angularDistance(
                    previousDirection.degrees(), direction.degrees()) >= 90.0D;

            if (wallMode!=WallTraversal.Mode.NONE) {
                landingTicks = 0;
                landing = LandingImpact.Strength.NONE;
                transitionTicks = 0;
                phase = switch (wallMode) {
                    case RUN -> LocomotionPhase.WALL_RUN;
                    case CLING, CLIMB -> LocomotionPhase.WALL_CLIMB;
                    default -> LocomotionPhase.WALL_LEAP;
                };
            } else if (landingTicks > 0) {
                landingTicks--;
                phase = landingPhase(landing);
                if (landingTicks == 0) landing = LandingImpact.Strength.NONE;
            } else {
                boolean running = nowMoving && (sprintIntent || player.isSprinting()
                        || Math.sqrt(horizontalSqr) >= RUN_SPEED);
                LocomotionPhase resolved = LocomotionPhase.resolve(charging, player.onGround(),
                        velocity.y, nowMoving, running, moving, hardTurn,
                        LandingImpact.Strength.NONE);
                if (resolved == LocomotionPhase.START) {
                    transitionTicks = START_TICKS;
                    transitionPhase = LocomotionPhase.START;
                } else if (resolved == LocomotionPhase.BRAKE) {
                    transitionTicks = BRAKE_TICKS;
                    transitionPhase = LocomotionPhase.BRAKE;
                } else if (resolved == LocomotionPhase.TURN) {
                    transitionTicks = TURN_TICKS;
                    transitionPhase = LocomotionPhase.TURN;
                }

                if (transitionTicks > 0 && !charging && player.onGround()) {
                    transitionTicks--;
                    phase = transitionPhase;
                } else {
                    phase = resolved;
                }
            }

            PrototypeLocomotion.Gait desiredGait = switch (phase) {
                case CHARGING -> PrototypeLocomotion.Gait.CHARGING;
                case RUN -> PrototypeLocomotion.Gait.RUN;
                case WALL_RUN -> PrototypeLocomotion.Gait.RUN;
                case START, WALK, BRAKE, TURN -> PrototypeLocomotion.Gait.WALK;
                default -> PrototypeLocomotion.Gait.IDLE;
            };
            PrototypeLocomotion.Gait previousGait = gait;
            gait = gaitStabilizer.update(desiredGait);
            if (phase == LocomotionPhase.WALK || phase == LocomotionPhase.RUN) {
                phase = switch (gait) {
                    case RUN -> LocomotionPhase.RUN;
                    case WALK -> LocomotionPhase.WALK;
                    default -> LocomotionPhase.IDLE;
                };
            }
            moving = nowMoving;
            if (gait != previousGait) {
                BlacklightNeoMod.LOGGER.debug("Prototype controller gait {} -> {} for {}",
                        previousGait, gait, player.getName().getString());
            }
            return gait != previousGait || phase != previousPhase;
        }
    }
}
