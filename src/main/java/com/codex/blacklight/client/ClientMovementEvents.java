package com.codex.blacklight.client;

import com.codex.blacklight.BlacklightNeoMod;
import com.codex.blacklight.client.screen.MovementTunerScreen;
import com.codex.blacklight.client.tuning.AnimationPoseSettings;
import com.codex.blacklight.integration.epicfight.BlacklightEpicFightAnimations;
import com.codex.blacklight.movement.JumpInputState;
import com.codex.blacklight.movement.MovementTuningSettings;
import com.codex.blacklight.movement.PrototypeLocomotion;
import com.codex.blacklight.movement.WallTraversal;
import com.codex.blacklight.movement.WallContacts;
import com.codex.blacklight.movement.WaterRunning;
import com.codex.blacklight.network.MovementInputPayload;
import com.codex.blacklight.power.MovementUpgrade;
import com.codex.blacklight.registry.ModAttachments;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.client.animation.ClientAnimator;
import yesman.epicfight.api.client.event.EpicFightClientEventHooks;
import yesman.epicfight.api.client.event.types.entity.ModifyPlayerLivingMotionEvent;
import yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

@EventBusSubscriber(modid = BlacklightNeoMod.MOD_ID, value = Dist.CLIENT)
public final class ClientMovementEvents {
    private static final String CATEGORY = "key.categories.blacklight";
    private static final KeyMapping ENGINE_TUNER = new KeyMapping(
            "key.blacklight.engine_tuner", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F8, CATEGORY);
    private static boolean lastJumpHeld;
    private static boolean lastForwardHeld;
    private static boolean lastMovingHeld;
    private static boolean lastSprinting;
    private static boolean lastChargingAnimation;
    private static int clientChargeTicks;
    private static boolean runningCharge;
    private static int clientOffGroundTicks;
    private static final JumpInputState JUMP_INPUT = new JumpInputState();
    private static LivingMotion lastForcedMotion;
    private static boolean wasPrototype;
    private static boolean wasHoldingFinalChargePose;
    private static boolean chargeDriverEngaged;
    private static final WallTraversal CLIENT_WALL = new WallTraversal();
    private static int lastForwardInput, lastSidewaysInput;
    private static boolean lastDetach, lastSprintKey;
    private static float lastCameraYaw;
    private static net.minecraft.world.entity.player.Player weaponMovementOwner;
    private static com.codex.blacklight.combat.WeaponVisual ownedWeaponVisual;
    private static net.minecraft.world.entity.player.Player pendingWeaponOwner;
    private static int pendingWeaponUntil;
    private static net.minecraft.client.player.LocalPlayer inputOwner;


    static {
        // Epic Fight events use their own EventHook system; they are not NeoForge
        // Event subclasses and therefore must never be annotated with @SubscribeEvent.
        EpicFightClientEventHooks.Entity.MODIFY_PLAYER_LIVING_MOTION_BASE.registerEvent(
                ClientMovementEvents::onLivingMotion, "blacklight:prototype_movement", -1000);
    }

    private ClientMovementEvents() { }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(ENGINE_TUNER);
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    public static void lockWeaponMovement(net.neoforged.neoforge.client.event.MovementInputUpdateEvent event) {
        beginWeaponMovementOwnership(event.getEntity());
        if (event.getEntity().getData(ModAttachments.WEAPON_VISUAL).spec() == null
                && !pendingWeaponMovement(event.getEntity())) return;
        // Suppress vanilla walking/jumping as well as our traversal prediction.
        // Physical KeyMappings remain readable so held inputs resume after the action.
        clearWeaponTravelInput(event.getInput());
    }

    private static void clearWeaponTravelInput(net.minecraft.client.player.Input input) {
        input.forwardImpulse = 0;
        input.leftImpulse = 0;
        input.jumping = false;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
    }

    static void sendAttackDirection(Minecraft minecraft) {
        if(minecraft.player==null||minecraft.getConnection()==null||minecraft.screen!=null)return;
        int forward=(mappingDown(minecraft,minecraft.options.keyUp)?1:0)-(mappingDown(minecraft,minecraft.options.keyDown)?1:0);
        int sideways=(mappingDown(minecraft,minecraft.options.keyRight)?1:0)-(mappingDown(minecraft,minecraft.options.keyLeft)?1:0);
        // Deliver the direction before the attack packet, even when both keys
        // were pressed between ticks. Preserve traversal's own jump/sprint edges.
        PacketDistributor.sendToServer(new MovementInputPayload(lastJumpHeld,false,forward,sideways,lastSprinting,lastDetach,cameraYaw(minecraft)));
    }

    /** Camera-relative intent survives Epic Fight consuming vanilla movement mappings. */
    static Float movementHeading(Minecraft minecraft) {
        if(minecraft.player==null||minecraft.screen!=null)return null;
        int forward=(mappingDown(minecraft,minecraft.options.keyUp)?1:0)-(mappingDown(minecraft,minecraft.options.keyDown)?1:0);
        int right=(mappingDown(minecraft,minecraft.options.keyRight)?1:0)-(mappingDown(minecraft,minecraft.options.keyLeft)?1:0);
        return forward==0&&right==0?null:cameraYaw(minecraft)+(float)Math.toDegrees(Math.atan2(right,forward));
    }

    private static float cameraYaw(Minecraft mc) {
        if(com.codex.blacklight.client.camera.PrototypeChaseCamera.ownsView())
            return com.codex.blacklight.client.camera.PrototypeChaseCamera.controlYaw();
        var api=yesman.epicfight.api.client.camera.EpicFightCameraAPI.getInstance();
        return api.isTPSMode()?api.getCameraYRot():mc.player.getYRot();
    }

    static boolean prepareGroundedWeaponMovement(Minecraft minecraft) {
        var player=minecraft.player;
        if(player==null||pendingWeaponMovement(player))return false;
        // Cover the short interval before the authoritative visual arrives. A
        // rejected request expires instead of trapping the player in a local lock.
        pendingWeaponOwner=player;pendingWeaponUntil=player.tickCount+6;
        player.setDeltaMovement(0,player.getDeltaMovement().y,0);
        player.xxa=0;player.zza=0;
        clearWeaponTravelInput(player.input);
        return true;
    }

    private static boolean pendingWeaponMovement(net.minecraft.world.entity.player.Player player) {
        if(pendingWeaponOwner!=player)return false;
        if(player.tickCount>=pendingWeaponUntil||!player.isAlive()
                ||!player.getData(ModAttachments.PLAYER_STATE).isPrototypeMode()
                ||player.getData(ModAttachments.PLAYER_STATE).getActivePower()!=com.codex.blacklight.power.PowerForm.BLADE
                ||player.getData(ModAttachments.WEAPON_VISUAL).spec()!=null){
            pendingWeaponOwner=null;
            return false;
        }
        return true;
    }

    private static void beginWeaponMovementOwnership(net.minecraft.world.entity.player.Player player) {
        var visual=player.getData(ModAttachments.WEAPON_VISUAL);
        var move=visual.spec();
        if(move==null){weaponMovementOwner=null;ownedWeaponVisual=null;return;}
        pendingWeaponOwner=null;
        if(weaponMovementOwner==player&&visual.equals(ownedWeaponVisual))return;
        boolean sameAction=weaponMovementOwner==player&&ownedWeaponVisual!=null
                &&visual.move()==ownedWeaponVisual.move()&&visual.started()==ownedWeaponVisual.started()
                &&visual.charging()==ownedWeaponVisual.charging()&&visual.clipOffset()==ownedWeaponVisual.clipOffset();
        weaponMovementOwner=player;ownedWeaponVisual=visual;
        // Render/contact yaw belongs to the action. Never feed its updates back
        // into the camera, or mouse steering fights the authoritative body yaw.
        player.yBodyRot=visual.yaw();
        if(!sameAction&&player.onGround()&&(com.codex.blacklight.combat.LocalWeaponMotion.find(move.id).isPresent()
                ||com.codex.blacklight.combat.WeaponMotionProfile.forMove(move)!=null)){
            // Claim pre-action running inertia once, before vanilla travel. Later
            // knockback and airborne actions keep their own deliberate velocity.
            player.setDeltaMovement(0,player.getDeltaMovement().y,0);
            player.xxa=0;player.zza=0;
        }
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getAction() != GLFW.GLFW_PRESS || minecraft.player == null) return;
        if (ENGINE_TUNER.matches(event.getKey(), event.getScanCode())) {
            if (!isPrototype(minecraft)) return;
            if (minecraft.screen == null && canTuneMovement(minecraft)) {
                minecraft.setScreen(new MovementTunerScreen());
            } else if (minecraft.screen == null) {
                minecraft.player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable(
                                "message.blacklight.tuner_singleplayer_only"), true);
            }
            return;
        }
    }

    /** Same collision probes and wall-local physics as the authoritative controller. */
    private static boolean predictWallRun(Minecraft minecraft, int forward, int sideways,
                                          boolean sprinting, boolean jump, boolean detach) {
        var player=minecraft.player;
        if (player==null || minecraft.level==null || player.isPassenger() || player.isFallFlying()
                || player.getAbilities().flying || player.isSpectator() || !player.isAlive()
                || player.isInWater() || player.isInLava() || ClientClawEvents.isGroundSpikeActionActive()) {
            resetClientWallRun();return false;
        }
        var frame=CLIENT_WALL.tick(new WallTraversal.Input(forward,sideways,sprinting,jump,detach),
                WallContacts.sample(player,CLIENT_WALL),player.getY(),player.getDeltaMovement().horizontalDistance(),
                MovementTuningSettings.wallClimbVelocity(),MovementUpgrade.wallMultiplier(
                        player.getData(ModAttachments.PLAYER_STATE).getMovementUpgradeMask()));
        com.codex.blacklight.movement.WallPoseState.set(player,CLIENT_WALL.mode(),CLIENT_WALL.normalX(),CLIENT_WALL.normalZ());
        if (!frame.handled()) return false;
        if (jump) JUMP_INPUT.claimCharge();
        player.setDeltaMovement(frame.x(),frame.y(),frame.z());player.setOnGround(false);player.resetFallDistance();
        player.setSprinting(sprinting && (frame.mode()==WallTraversal.Mode.RUN
                || frame.mode()==WallTraversal.Mode.LEAP || WallTraversal.crossingTop(frame.mode())));
        if (lastChargingAnimation) {
            lastChargingAnimation=false;clientChargeTicks=0;releaseFinalChargedJumpPose(minecraft);
        }
        return true;
    }

    static WallTraversal.Mode wallMode() { return CLIENT_WALL.mode(); }
    static boolean isWallRunning() { return CLIENT_WALL.attached(); }
    private static void resetClientWallRun() {
        var player=Minecraft.getInstance().player;
        if(player!=null)com.codex.blacklight.movement.WallPoseState.set(player,WallTraversal.Mode.NONE,0,0);
        CLIENT_WALL.reset();lastForwardInput=lastSidewaysInput=0;lastDetach=lastSprintKey=false;
    }

    /** Airborne claw left-click entry point owned by ClientClawEvents. */
    static void requestAirDashFromAttack(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.screen != null
                || minecraft.player.onGround() || !isPrototype(minecraft)
                || weaponOwnsMovement(minecraft)) return;
        PacketDistributor.sendToServer(new MovementInputPayload(
                mappingDown(minecraft, minecraft.options.keyJump), true,
                lastForwardInput, lastSidewaysInput, lastSprinting, lastDetach,cameraYaw(minecraft)));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        // Respawn can replace LocalPlayer without a tick in which player is null.
        // Charge/wall latches belong to the old entity, never the next life.
        if (inputOwner != minecraft.player || minecraft.player == null || minecraft.getConnection() == null) {
            if(inputOwner!=null){
                var oldPatch=EpicFightCapabilities.getEntityPatch(inputOwner,AbstractClientPlayerPatch.class);
                if(oldPatch!=null)oldPatch.getAnimator().setHardPause(false);
            }
            inputOwner=minecraft.player;
            weaponMovementOwner=null;ownedWeaponVisual=null;pendingWeaponOwner=null;
            lastJumpHeld = false;
            lastForwardHeld = false;
            lastMovingHeld = false;
            lastSprinting = false;
            lastChargingAnimation = false;
            clientChargeTicks = 0;
            clientOffGroundTicks = 0;
            JUMP_INPUT.reset();
            PrototypeAnimationController.tickLevel(minecraft, false, false, false);
            lastForcedMotion = null;
            wasPrototype = false;
            wasHoldingFinalChargePose = false;
            chargeDriverEngaged = false;
            runningCharge = false;
            lastCameraYaw = Float.NaN;
            resetClientWallRun();
            if (minecraft.player == null || minecraft.getConnection() == null) return;
        }
        // Read the hardware state rather than KeyMapping.isDown(). Epic Fight's control
        // engine is allowed to consume/reset mappings before NeoForge's post tick.
        if (minecraft.player.getData(ModAttachments.CONSUMING)&&com.codex.blacklight.combat.ConsumeVisual.blocksInput(minecraft.player.getData(ModAttachments.GRAB_STAGE))) {
            lastJumpHeld=false;lastForwardHeld=false;lastMovingHeld=false;lastSprinting=false;
            lastChargingAnimation=false;clientChargeTicks=0;
            clientOffGroundTicks=0;JUMP_INPUT.reset();
            resetClientWallRun();
            if(com.codex.blacklight.combat.ConsumeVisual.blocksTravel(minecraft.player.getData(ModAttachments.GRAB_STAGE)))
                minecraft.player.setDeltaMovement(0, minecraft.player.getDeltaMovement().y, 0);
            return;
        }
        boolean prototype = isPrototype(minecraft);
        boolean jumpHeld = minecraft.screen == null && mappingDown(minecraft, minecraft.options.keyJump)&&minecraft.player.getData(ModAttachments.GRAB_STAGE)!=com.codex.blacklight.combat.ConsumeVisual.THROW;
        int forwardInput=minecraft.screen==null ? (mappingDown(minecraft,minecraft.options.keyUp)?1:0)-(mappingDown(minecraft,minecraft.options.keyDown)?1:0) : 0;
        int sidewaysInput=minecraft.screen==null ? (mappingDown(minecraft,minecraft.options.keyRight)?1:0)-(mappingDown(minecraft,minecraft.options.keyLeft)?1:0) : 0;
        boolean forwardHeld=forwardInput>0;
        boolean movingHeld=forwardInput!=0 || sidewaysInput!=0;
        boolean sprintKey=minecraft.screen==null && mappingDown(minecraft,minecraft.options.keySprint);
        boolean detach=minecraft.screen==null && mappingDown(minecraft,minecraft.options.keyShift);
        // Double-tap momentum can enter the wall. Releasing W stops it; releasing
        // an explicitly held sprint key also returns to the slower climbing mode.
        boolean sprinting=minecraft.screen==null && !detach && (CLIENT_WALL.attached()
                ? movingHeld && !detach && (sprintKey || (lastSprinting && !lastSprintKey))
                : physicalSprint(minecraft) || (forwardHeld && lastSprinting));
        // The server also suspends traversal in fluids. Cancel the local charge,
        // including its frozen pose, and resend held inputs when leaving the water.
        if (!prototype || minecraft.player.isInWater() && !WaterRunning.onSurface(minecraft.player) || minecraft.player.isInLava()) {
            releaseFinalChargedJumpPose(minecraft);
            lastJumpHeld = false;
            lastForwardHeld = false;
            lastMovingHeld = false;
            lastSprinting = false;
            lastChargingAnimation = false;
            clientChargeTicks = 0;
            chargeDriverEngaged = false;
            runningCharge = false;
            clientOffGroundTicks = 0;
            JUMP_INPUT.reset();
            PrototypeAnimationController.tickLevel(minecraft, false, false, false);
            lastForcedMotion = null;
            wasPrototype = false;
            resetClientWallRun();
            var movement=minecraft.player.getData(ModAttachments.MOVEMENT_STATE);
            movement.clearSprintDrive();
            if(prototype&&minecraft.player.isInWater()&&!minecraft.player.isInLava()) {
                float yaw=cameraYaw(minecraft);
                movement.setCameraYaw(yaw);
                movement.acceptInput(false,false,forwardInput,sidewaysInput,sprinting,detach);
                PacketDistributor.sendToServer(new MovementInputPayload(false,false,
                        forwardInput,sidewaysInput,sprinting,detach,yaw));
                WaterRunning.riseToSurface(minecraft.player);
            }
            return;
        }
        if (minecraft.player.onGround()) clientOffGroundTicks = 0;
        else clientOffGroundTicks++;
        JUMP_INPUT.acceptInput(jumpHeld, minecraft.player, CLIENT_WALL.attached());
        JUMP_INPUT.observeGround(minecraft.player.onGround());
        boolean weaponAction = weaponOwnsMovement(minecraft);
        if (weaponAction) {
            JUMP_INPUT.cancelPendingCharge();
            beginWeaponMovementOwnership(minecraft.player);
            lastChargingAnimation = false;
            clientChargeTicks = 0;
            chargeDriverEngaged = false;
            releaseFinalChargedJumpPose(minecraft);
        }
        // Latch the pose at the moment charging begins. The server's movement and
        // animation synchronization can make onGround() flicker for a frame, but a
        // held charge must remain visually stable until release/full charge. The
        // crouch action itself is started by the server (playAnimationSynchronized)
        // and arrives via Epic Fight's normal animation sync.
        if (!weaponAction && !lastChargingAnimation && !CLIENT_WALL.attached()
                && !minecraft.player.getAbilities().flying
                && JUMP_INPUT.consumeChargeRequest(minecraft.player)) {
            lastChargingAnimation = true;
            runningCharge=sprinting&&forwardHeld;
            clientChargeTicks = 0;
            chargeDriverEngaged = false;
        }
        if (lastChargingAnimation) {
            if (clientOffGroundTicks > PrototypeLocomotion.CHARGE_GROUND_GRACE_TICKS) {
                lastChargingAnimation = false;
                clientChargeTicks = 0;
                releaseFinalChargedJumpPose(minecraft);
            } else if (!jumpHeld) {
                lastChargingAnimation = false;
                clientChargeTicks = 0;
                // Instant local launch feedback; the server-synchronized BIPED_JUMP
                // (same borrowed clip) replaces it without a visible snap.
                playLocalLaunchAnimation(minecraft);
            } else if (minecraft.player.onGround()) {
                clientChargeTicks = Math.min(MovementTuningSettings.jumpChargeTicks(),
                        clientChargeTicks + 1);
            }
        }
        float viewYaw=cameraYaw(minecraft);
        minecraft.player.getData(ModAttachments.MOVEMENT_STATE).setCameraYaw(viewYaw);
        minecraft.player.getData(ModAttachments.MOVEMENT_STATE).acceptInput(jumpHeld,false,
                forwardInput,sidewaysInput,sprinting,detach);
        boolean inputChanged = jumpHeld != lastJumpHeld || forwardInput != lastForwardInput
                || sidewaysInput != lastSidewaysInput || detach != lastDetach || sprinting != lastSprinting;
        boolean keysChanged=inputChanged;
        inputChanged|=Math.abs(net.minecraft.util.Mth.wrapDegrees(viewYaw-lastCameraYaw))>.1F||minecraft.player.tickCount%10==0;
        if (!wasPrototype || inputChanged) {
            PacketDistributor.sendToServer(new MovementInputPayload(
                    jumpHeld, false, forwardInput, sidewaysInput, sprinting, detach,viewYaw));
            if(keysChanged||!wasPrototype)BlacklightNeoMod.LOGGER.debug(
                    "Movement input sent jump={} forward={} sprint={}",
                    jumpHeld, forwardHeld, sprinting);
        }

        lastCameraYaw=viewYaw;lastForwardInput=forwardInput;lastSidewaysInput=sidewaysInput;lastDetach=detach;lastSprintKey=sprintKey;
        lastJumpHeld = jumpHeld;
        lastForwardHeld = forwardHeld;
        lastMovingHeld = movingHeld;
        lastSprinting = sprinting;
        wasPrototype = true;

        if (weaponAction) {
            // The input latches above continue tracking releases and sprint intent.
            // Avoid resetClientWallRun(): it would erase those input latches every
            // tick and manufacture a new edge when the weapon action finishes.
            CLIENT_WALL.reset();
            com.codex.blacklight.movement.WallPoseState.set(minecraft.player,WallTraversal.Mode.NONE,0,0);
            PrototypeAnimationController.tickLevel(minecraft, false, lastMovingHeld, lastSprinting);
            return;
        }

        // Predict wall attachment before Epic Fight resolves the living motion. This
        // lets the same tick select CLIMB/WALL_RUN instead of briefly choosing JUMP.
        boolean wallHandled = predictWallRun(minecraft, forwardInput, sidewaysInput, sprinting, jumpHeld, detach);

        // Hard-pause/unpause before asking Epic Fight to re-evaluate the living
        // motion. On key release this guarantees the newly selected motion is not
        // born inside an animator that is still frozen by the charged-jump hold.
        updateFinalChargedJumpPose(minecraft);

        // Resolve once per tick and confirm WALK/RUN/IDLE before changing Epic Fight's
        // base layer. Velocity correction may flicker for one frame, but can no longer
        // restart the transition and visibly snap the whole body.
        PrototypeAnimationController.tickLevel(minecraft, lastChargingAnimation&&!runningCharge,
                lastMovingHeld, lastSprinting);

        // Match the server acceleration locally.  Without this prediction the integrated
        // server repeatedly corrects the vanilla-speed client, which looks like a ghost
        // copy or stuttering trail behind the player.
        if (!wallHandled && (minecraft.player.onGround() || WaterRunning.onSurface(minecraft.player)) && forwardHeld && sprinting
                && !ClientClawEvents.isGroundSpikeActionActive()) {
            minecraft.player.setSprinting(true);
            var forward = com.codex.blacklight.movement.TraversalSteering.forward(minecraft.player);
            if (forward.lengthSqr() > 1.0E-5D) {
                forward = forward.normalize();
                var velocity = minecraft.player.getDeltaMovement();
                double max = MovementTuningSettings.sprintMaxSpeed()
                        * MovementUpgrade.sprintMultiplier(minecraft.player
                        .getData(ModAttachments.PLAYER_STATE).getMovementUpgradeMask());
                if (com.codex.blacklight.power.UtilityPowerSystem.active(minecraft.player,
                        com.codex.blacklight.power.PowerForm.ARMOR)) max *= .75;
                minecraft.player.setDeltaMovement(com.codex.blacklight.movement.TraversalSteering.sprint(minecraft.player,velocity,max,
                        MovementTuningSettings.sprintAcceleration(),forwardInput,sidewaysInput));
            }
        }
    }

    /**
     * Immediate local launch pose on key release, using the same authored
     * JUMP_RISE clip that the server synchronizes.
     */
    private static void playLocalLaunchAnimation(Minecraft minecraft) {
        if (BlacklightEpicFightAnimations.JUMP_RISE == null) return;
        AbstractClientPlayerPatch<?> patch = EpicFightCapabilities.getEntityPatch(
                minecraft.player, AbstractClientPlayerPatch.class);
        if (patch != null) {
            patch.getAnimator().playAnimation(BlacklightEpicFightAnimations.JUMP_RISE, 0.08F);
        }
    }

    private static void updateFinalChargedJumpPose(Minecraft minecraft) {
        int maximum = MovementTuningSettings.jumpChargeTicks();
        if (!lastChargingAnimation || runningCharge || BlacklightEpicFightAnimations.CHARGED_JUMP == null) {
            releaseFinalChargedJumpPose(minecraft);
            return;
        }
        AbstractClientPlayerPatch<?> patch = EpicFightCapabilities.getEntityPatch(
                minecraft.player, AbstractClientPlayerPatch.class);
        if (patch == null) return;
        Animator rawAnimator = patch.getAnimator();
        if (!(rawAnimator instanceof ClientAnimator animator)) return;
        // getPlayerFor() deliberately falls back to the base player even when its
        // accessor does not match, so the real animation is compared explicitly
        // (same identity pattern as the groundspike plant-pose hold).
        AnimationPlayer player = animator.getPlayerFor(
                BlacklightEpicFightAnimations.CHARGED_JUMP);
        var real = player == null ? null : player.getRealAnimation();
        if (real == null
                || !(real.equals(BlacklightEpicFightAnimations.CHARGED_JUMP)
                || real.equals(BlacklightEpicFightAnimations.CHARGED_JUMP.get()))) {
            // The server-synchronized charge ACTION has not arrived yet (or already
            // ended). Unlike the old base-layer static clip, Epic Fight never steals
            // an ActionAnimation for IDLE, so there is nothing to re-assert here.
            return;
        }
        if (!chargeDriverEngaged) {
            chargeDriverEngaged = true;
            BlacklightNeoMod.LOGGER.debug("Charged jump driver engaged on the action player");
        }
        // Drive this non-looping action from the actual charge ratio so the crouch
        // deepens with the held time, lands on its final frame at full charge and
        // holds there until the player releases the jump key
        // (releaseFinalChargedJumpPose unfreezes and the server-side launch follows).
        // Drive the action from the charge ratio every tick, but only hard-pause at
        // FULL charge. Pausing during the ramp freezes the action's blend-in weight
        // at ~0 (the action plays with a 0.08 s transition), which made the crouch
        // completely invisible; the groundspike only pauses after its plant frame
        // for the same reason. While unpaused the animator nudges the clip forward
        // one tick before our next setElapsedTime pulls it back — imperceptible —
        // and the epsilon clamp keeps it from ever tripping isEnd().
        float chargeRatio = Math.min(1.0F, clientChargeTicks / (float) maximum);
        float totalTime = BlacklightEpicFightAnimations.CHARGED_JUMP.get().getTotalTime();
        float authoredTime = Math.min(totalTime * chargeRatio, totalTime - 0.001F);
        player.setElapsedTime(authoredTime, authoredTime);
        boolean fullyCharged = clientChargeTicks >= maximum;
        animator.setHardPause(fullyCharged);
        if (fullyCharged && !wasHoldingFinalChargePose) {
            BlacklightNeoMod.LOGGER.debug(
                    "Charged jump reached authored final frame and is hard-paused");
            wasHoldingFinalChargePose = true;
        }
    }

    private static void releaseFinalChargedJumpPose(Minecraft minecraft) {
        if (minecraft.player == null) return;
        AbstractClientPlayerPatch<?> patch = EpicFightCapabilities.getEntityPatch(
                minecraft.player, AbstractClientPlayerPatch.class);
        if (patch != null) {
            // Idempotent: unpausing every tick while not charging is harmless and
            // guarantees the release always happens when the jump key comes up,
            // even for a partial charge that never latched the final frame.
            patch.getAnimator().setHardPause(false);
        }
        wasHoldingFinalChargePose = false;
    }

    /** Prevents the locomotion controller from replacing the latched final charge pose. */
    static boolean isHoldingFinalChargePose() {
        return wasHoldingFinalChargePose;
    }

    /** True while the local player is charging a jump, including the held full charge. */
    static boolean isChargingJump() {
        return lastChargingAnimation&&!runningCharge;
    }
    public static int visualJumpCharge(){return CLIENT_WALL.attached()?CLIENT_WALL.visualChargeTicks():lastChargingAnimation?clientChargeTicks:0;}

    /**
     * Keeps traversal poses on Epic Fight's base layer instead of firing a one-shot
     * animation that is replaced again on the following client tick.
     */
    public static void onLivingMotion(ModifyPlayerLivingMotionEvent.BaseLayer event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        // While the planted groundspike action occupies the base layer, living-motion
        // updates must not replace it; otherwise the insertion animation is swapped
        // back to RUN/WALK and the client never sees the authored plant pose.
        if (ClientClawEvents.isGroundSpikeActionActive()) return;
        var original = event.getPlayerPatch().getOriginal();
        if (!com.codex.blacklight.mode.PrototypeModeSystem.isActive(original)) return;
        // Do not replace Epic Fight's swimming motion with stale land/wall/glide poses.
        if (original.isInWater() && !WaterRunning.onSurface(original) || original.isInLava()) return;
        if (original.getData(ModAttachments.WEAPON_VISUAL).spec() != null) return;
        // A fully charged jump is frozen on its final crouch frame by
        // updateFinalChargedJumpPose. Re-asserting KNEEL here every tick would make
        // Epic Fight restart the non-looping crouch clip, so the charge visibly
        // replays instead of holding.
        if (original == minecraft.player && isHoldingFinalChargePose()) return;
        // The charge clip is played directly on the base layer by
        // startChargedJumpAnimation and driven frame-by-frame by
        // updateFinalChargedJumpPose. Asserting KNEEL here every tick would
        // restart the non-looping clip, so motion updates stop for the whole
        // charge (this is also what prevents the visible looping).
        if (original==minecraft.player && lastChargingAnimation&&!runningCharge) return;
        var wallMode=ClientWallMotion.modeFor(original);
        if (wallMode!=WallTraversal.Mode.NONE) {
            event.setMotion(WallTraversal.crossingTop(wallMode)
                    ? LivingMotions.JUMP : LivingMotions.CLIMB);
            return;
        }
        // Gliding gets its own looping clip on the FLY slot instead of the generic
        // falling pose. The predicate mirrors the server glide physics (airborne,
        // jump held, slow descent); FLY is otherwise unreachable in Prototype mode.
        if (original == minecraft.player && isGliding(minecraft)) {
            event.setMotion(LivingMotions.FLY);
            return;
        }
        var state = original.getData(ModAttachments.PLAYER_STATE);
        if (!state.isInfected() || !state.isPrototypeMode()) return;

        LivingMotion forced = switch (PrototypeAnimationController.phaseFor(
                event.getPlayerPatch().getOriginal())) {
            case CHARGING -> LivingMotions.KNEEL;
            case START, WALK, BRAKE, TURN -> LivingMotions.WALK;
            case RUN -> LivingMotions.RUN;
            case JUMP_LAUNCH, RISE -> LivingMotions.JUMP;
            case FALL -> LivingMotions.FALL;
            case WALL_RUN, WALL_CLIMB, WALL_LEAP -> LivingMotions.CLIMB;
            case LAND_LIGHT, LAND_HEAVY, LAND_DEVASTATING -> LivingMotions.LANDING_RECOVERY;
            default -> null;
        };

        if (forced != null) event.setMotion(forced);
        if (original == minecraft.player && forced != lastForcedMotion) {
            BlacklightNeoMod.LOGGER.debug("Prototype client base motion: {} -> {}",
                    lastForcedMotion == null ? "default" : lastForcedMotion,
                    forced == null ? "default" : forced);
            lastForcedMotion = forced;
        }
    }

    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || event.getPlayer() != minecraft.player || !isPrototype(minecraft)) return;
        if(com.codex.blacklight.client.camera.PrototypeChaseCamera.ownsView()) {
            // Source camera owns optical zoom. MC movement attributes must not add a second speed FOV.
            event.setNewFovModifier(1);return;
        }
        double horizontalSpeed = minecraft.player.getDeltaMovement().horizontalDistance();
        double ratio = PrototypeLocomotion.cinematicSpeedRatio(horizontalSpeed,
                MovementTuningSettings.sprintMaxSpeed()
                        * MovementUpgrade.sprintMultiplier(minecraft.player
                        .getData(ModAttachments.PLAYER_STATE).getMovementUpgradeMask()));
        event.setNewFovModifier((float)(event.getFovModifier()
                * (1.0D + MovementTuningSettings.fovBoost() * ratio)));
    }

    /** Uses the same input ownership and terrain clearance as the server. */
    private static boolean isGliding(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.player.onGround()
                || weaponOwnsMovement(minecraft)
                || com.codex.blacklight.power.UtilityPowerSystem.active(minecraft.player,com.codex.blacklight.power.PowerForm.ARMOR)
                || lastChargingAnimation || CLIENT_WALL.attached()
                || ClientClawEvents.isGroundSpikeActionActive()) return false;
        return JUMP_INPUT.canGlide(minecraft.player);
    }

    private static boolean isPrototype(Minecraft minecraft) {
        return minecraft.player != null
                && minecraft.player.getData(ModAttachments.PLAYER_STATE).isInfected()
                && minecraft.player.getData(ModAttachments.PLAYER_STATE).isPrototypeMode();
    }

    private static boolean weaponOwnsMovement(Minecraft minecraft) {
        return minecraft.player != null
                && (minecraft.player.getData(ModAttachments.WEAPON_VISUAL).spec() != null
                ||pendingWeaponMovement(minecraft.player));
    }

    /** Stable sprint intent used to choose the moving attack on mouse release. */
    static boolean isRunningForAttack(Minecraft minecraft) {
        if (minecraft.player == null || !minecraft.player.onGround()) return false;
        return (lastMovingHeld && lastSprinting)
                || minecraft.player.getDeltaMovement().horizontalDistance()
                >= PrototypeLocomotion.RUN_ANIMATION_SPEED;
    }

    private static boolean canTuneMovement(Minecraft minecraft) {
        return minecraft.hasSingleplayerServer() && minecraft.getSingleplayerServer() != null
                && !minecraft.getSingleplayerServer().isPublished();
    }

    private static boolean physicalSprint(Minecraft minecraft) {
        return minecraft.player != null && (minecraft.player.isSprinting()
                || mappingDown(minecraft, minecraft.options.keySprint));
    }

    private static boolean mappingDown(Minecraft minecraft, KeyMapping mapping) {
        if (mapping.isDown()) return true;
        InputConstants.Key key = mapping.getKey();
        if (key.getType() == InputConstants.Type.KEYSYM) {
            return InputConstants.isKeyDown(minecraft.getWindow().getWindow(), key.getValue());
        }
        if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(minecraft.getWindow().getWindow(), key.getValue())
                    == GLFW.GLFW_PRESS;
        }
        return false;
    }
}
