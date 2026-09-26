package com.codex.blacklight.client;

import com.codex.blacklight.BlacklightNeoMod;
import com.codex.blacklight.movement.LandingImpact;
import com.codex.blacklight.movement.WallTraversal;
import com.codex.blacklight.movement.LocomotionDirection;
import com.codex.blacklight.movement.LocomotionPhase;
import com.codex.blacklight.movement.LocomotionVisualState;
import com.codex.blacklight.movement.PrototypeLocomotion;
import com.codex.blacklight.movement.TerrainTraversal;
import com.codex.blacklight.movement.WaterRunning;
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
            if (!blacklight.isInfected() || !blacklight.isPrototypeMode()
                    || player.isInWater() && !WaterRunning.onSurface(player) || player.isInLava()) {
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
            LocomotionVisualState.set(player.getId(), runtime.direction, runtime.phase,runtime.sprintGait);
            // At full jump charge ClientMovementEvents owns the exact final frame.
            // Re-evaluating KNEEL here would construct/restart the living animation
            // underneath that pause and make a long hold visibly loop.
            boolean finalChargeLatched = local
                    && ClientMovementEvents.isHoldingFinalChargePose();
            AbstractClientPlayerPatch<?> patch = EpicFightCapabilities.getEntityPatch(
                    player, AbstractClientPlayerPatch.class);
            if (motionChanged) runtime.motionRefreshPending = true;
            // Native attacks own the full body. Defer locomotion refresh until their
            // recovery, and never restart the paused charge or an absorption pose.
            if (runtime.motionRefreshPending && !finalChargeLatched && patch != null
                    && player.getData(ModAttachments.WEAPON_VISUAL).spec()==null
                    && (!player.getData(ModAttachments.CONSUMING)||player.getData(ModAttachments.GRAB_STAGE)==com.codex.blacklight.combat.ConsumeVisual.HOLD)
                    && (!local||!ClientClawEvents.isGroundSpikeActionActive())
                    && (!local||!ClientMovementEvents.isChargingJump())) {
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

    /** Free locomotion faces travel independently of the camera; action owners stay authoritative. */
    public static Float movementFacing(AbstractClientPlayer player, float partial) {
        var state=STATES.get(player);
        return state==null||!state.freeFacing?null:net.minecraft.util.Mth.rotLerp(partial,state.previousFacing,state.facing);
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
        private boolean sprintGait;
        private boolean motionRefreshPending;
        private boolean freeFacing, facingInitialized;
        private float facing, previousFacing;
        private int terrainAirTicks;
        private boolean hadGroundSupport;
        private WallTraversal.Mode previousWallMode = WallTraversal.Mode.NONE;

        private boolean visualGrounded(AbstractClientPlayer player, Vec3 velocity, boolean moving) {
            if (player.onGround()) {
                terrainAirTicks = 0;
                hadGroundSupport = true;
                return true;
            }
            terrainAirTicks++;
            // Preserve the stride over a brief downward step, not a jump or a cliff.
            // This is visual only: never forge onGround, teleport or suppress gravity.
            boolean followsTerrain = hadGroundSupport && moving && terrainAirTicks <= 6
                    && velocity.y <= .035 && velocity.y >= -.5
                    && !player.isFallFlying() && !player.getAbilities().flying
                    && TerrainTraversal.supportBelow(player, 1.05);
            if (!followsTerrain) hadGroundSupport = false;
            return followsTerrain;
        }

        private boolean update(AbstractClientPlayer player, boolean charging,
                               boolean movingIntent, boolean sprintIntent,
                               WallTraversal.Mode wallMode) {
            LocomotionPhase previousPhase = phase;
            Vec3 velocity = player.getDeltaMovement();
            double horizontalSqr = velocity.x * velocity.x + velocity.z * velocity.z;
            boolean nowMoving = movingIntent || horizontalSqr >= MOVING_SPEED_SQR;
            boolean grounded = visualGrounded(player, velocity, nowMoving);
            boolean ontoPlatform = previousWallMode != WallTraversal.Mode.NONE
                    && wallMode == WallTraversal.Mode.NONE && grounded;
            previousWallMode = wallMode;
            var patch=EpicFightCapabilities.getEntityPatch(player,AbstractClientPlayerPatch.class);
            var weapon=player.getData(ModAttachments.WEAPON_VISUAL);
            var minecraft=Minecraft.getInstance();
            boolean local=player==minecraft.player;
            var target=patch==null?null:patch.getTarget();
            // getTarget() on LocalPlayerPatch is camera focus, also populated without lock-on.
            boolean locked=target!=null&&target.isAlive()&&!target.isRemoved()
                    &&(!local||yesman.epicfight.api.client.camera.EpicFightCameraAPI.getInstance().isLockingOnTarget());
            freeFacing=grounded&&!charging&&wallMode==WallTraversal.Mode.NONE
                    &&!player.isPassenger()&&!player.isSleeping()&&!player.isFallFlying()&&!player.getAbilities().flying
                    &&weapon.spec()==null&&!player.getData(ModAttachments.CONSUMING)
                    &&!locked;
            if(!facingInitialized){facing=player.yBodyRot;facingInitialized=true;}
            previousFacing=facing;
            if(!freeFacing)facing=weapon.spec()==null?player.yBodyRot:weapon.yaw();
            else {
                Float intent=local?ClientMovementEvents.movementHeading(minecraft):null;
                // Remote players don't reliably expose travel velocity; use their interpolated step.
                double dx=local?velocity.x:player.getX()-player.xo;
                double dz=local?velocity.z:player.getZ()-player.zo;
                if(intent!=null||dx*dx+dz*dz>=MOVING_SPEED_SQR) {
                    float desired=intent!=null?intent:(float)Math.toDegrees(Math.atan2(-dx,dz));
                    facing+=Math.clamp(net.minecraft.util.Mth.wrapDegrees(desired-facing),-24F,24F);
                }
            }
            double yaw = Math.toRadians(freeFacing?facing:player.yBodyRot);
            double forwardX = -Math.sin(yaw);
            double forwardZ = Math.cos(yaw);
            double rightX = Math.cos(yaw);
            double rightZ = Math.sin(yaw);
            double localForward = velocity.x * forwardX + velocity.z * forwardZ;
            double localRight = velocity.x * rightX + velocity.z * rightZ;
            LocomotionDirection previousDirection = direction;
            if (nowMoving) direction = directionStabilizer.update(localForward, localRight);
            // Free-facing already turns the entire model. Playing the native 180-degree
            // root turn on top would rotate it a second time and briefly face backwards.
            boolean hardTurn = !freeFacing && nowMoving && moving
                    && LocomotionDirection.angularDistance(
                    previousDirection.degrees(), direction.degrees()) >= 90.0D;
            if(freeFacing&&transitionPhase==LocomotionPhase.TURN)transitionTicks=0;

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
                LocomotionPhase resolved = LocomotionPhase.resolve(charging, grounded,
                        velocity.y, nowMoving, running, moving, hardTurn,
                        LandingImpact.Strength.NONE);
                // A fresh direction must leave braking immediately, including while
                // residual velocity still makes nowMoving true. Do not latch airborne phases.
                if(charging||!grounded||transitionPhase==LocomotionPhase.BRAKE&&nowMoving)
                    transitionTicks=0;
                if (resolved == LocomotionPhase.START) {
                    transitionTicks = START_TICKS;
                    transitionPhase = LocomotionPhase.START;
                } else if (resolved == LocomotionPhase.BRAKE) {
                    transitionTicks = sprintGait?17:14;
                    transitionPhase = LocomotionPhase.BRAKE;
                } else if (resolved == LocomotionPhase.TURN) {
                    transitionTicks = 8;
                    transitionPhase = LocomotionPhase.TURN;
                }

                if (transitionTicks > 0 && !charging && grounded) {
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
            // A confirmed wall-to-ground handoff already supplied the transition.
            // Do not insert the gait filter's old airborne IDLE for another 3 ticks.
            if (ontoPlatform) gaitStabilizer.reset();
            gait = gaitStabilizer.update(desiredGait);
            if (phase == LocomotionPhase.WALK || phase == LocomotionPhase.RUN) {
                phase = switch (gait) {
                    case RUN -> LocomotionPhase.RUN;
                    case WALK -> LocomotionPhase.WALK;
                    default -> LocomotionPhase.IDLE;
                };
            }
            moving = nowMoving;
            if(phase==LocomotionPhase.RUN)sprintGait=true;
            else if(phase==LocomotionPhase.IDLE
                    ||phase==LocomotionPhase.WALK&&movingIntent&&!sprintIntent)sprintGait=false;
            if (gait != previousGait) {
                BlacklightNeoMod.LOGGER.debug("Prototype controller gait {} -> {} for {}",
                        previousGait, gait, player.getName().getString());
            }
            return gait != previousGait || phase != previousPhase;
        }
    }
}
