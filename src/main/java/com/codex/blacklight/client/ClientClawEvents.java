package com.codex.blacklight.client;

import com.codex.blacklight.BlacklightNeoMod;
import com.codex.blacklight.client.renderer.GroundSpikeRenderer;
import com.codex.blacklight.client.renderer.BlacklightPlayerRenderer;
import com.codex.blacklight.client.screen.ClawTunerScreen;
import com.codex.blacklight.client.screen.AnimationTunerScreen;
import com.codex.blacklight.client.screen.UpgradeScreen;
import com.codex.blacklight.client.model.ClawArmModel;
import com.codex.blacklight.client.tuning.AnimationPoseSettings;
import com.codex.blacklight.entity.GroundSpikeEntity;
import com.codex.blacklight.integration.epicfight.BlacklightEpicFightAnimations;
import com.codex.blacklight.integration.epicfight.GroundSpikeActionAnimation;
import com.codex.blacklight.network.ClawActionPayload;
import com.codex.blacklight.network.PrototypeModePayload;
import com.codex.blacklight.mode.PrototypeSurvivalLock;
import com.codex.blacklight.power.PowerForm;
import com.codex.blacklight.registry.ModAttachments;
import com.codex.blacklight.registry.ModEntities;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.client.camera.EpicFightCameraAPI;
import yesman.epicfight.api.client.event.EpicFightClientEventHooks;
import yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

@EventBusSubscriber(modid = BlacklightNeoMod.MOD_ID, value = Dist.CLIENT)
public final class ClientClawEvents {
    private static final String CATEGORY = "key.categories.blacklight";
    private static final KeyMapping CYCLE_FORM = new KeyMapping("key.blacklight.cycle_form", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, CATEGORY);
    private static final KeyMapping GRAB = new KeyMapping(
            "key.blacklight.grab", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_E, CATEGORY);
    private static final KeyMapping TOGGLE_CLAWS = new KeyMapping(
            "key.blacklight.toggle_claws", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY);
    private static final KeyMapping PROTOTYPE_MODE = new KeyMapping(
            "key.blacklight.prototype_mode", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY);
    private static final KeyMapping CLAW_TUNER = new KeyMapping(
            "key.blacklight.claw_tuner", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORY);
    private static final KeyMapping ANIMATION_TUNER = new KeyMapping(
            "key.blacklight.animation_tuner", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, CATEGORY);
    private static final KeyMapping TARGET_LOCK = new KeyMapping(
            "key.blacklight.target_lock", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_TAB, CATEGORY);
    private static final KeyMapping UPGRADE_PANEL = new KeyMapping(
            "key.blacklight.upgrades", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_U, CATEGORY);
    /** Grounded left-clicks held this long become a dashing slice instead of a combo hit. */
    private static final int DASHING_SLICE_HOLD_TICKS = 4;
    private static boolean layerHookRegistered;
    private static boolean automaticTunerOpened;
    private static boolean lastGrabLeft,lastGrabRight;
    private static boolean leftAttackHeld;
    private static boolean leftAttackConsumed;
    private static boolean leftAttackTapSent;
    private static boolean lastPhysicalLeftDown;
    private static int leftAttackHoldTicks;
    private static boolean groundSpikeHeld;
    private static boolean groundSpikePosePaused;
    private static boolean groundSpikeAutoReleased;
    private static boolean groundSpikeReversing;
    private static int groundSpikeEruptionTicks;
    private static int groundSpikeStartupWaitTicks;
    // The authored insertion reaches contact after 11 ticks. Capture within half
    // a tick so we do not skip the last visible downward motion before holding.
    // The same 0.55 s insertion is reversed to withdraw the claw.
    private static final float GROUND_SPIKE_PLANT_TIME = GroundSpikeActionAnimation.PLANT_TIME;
    private static final float GROUND_SPIKE_END_CAPTURE_WINDOW = 0.025F;

    private ClientClawEvents() {
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(GRAB);
        event.register(CYCLE_FORM);
        event.register(TOGGLE_CLAWS);
        event.register(PROTOTYPE_MODE);
        event.register(CLAW_TUNER);
        event.register(ANIMATION_TUNER);
        event.register(TARGET_LOCK);
        event.register(UPGRADE_PANEL);
        registerEpicFightLayerHook();
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.GROUND_SPIKE.get(), GroundSpikeRenderer::new);
        event.registerEntityRenderer(ModEntities.BLACKLIGHT_HUNTER.get(), com.codex.blacklight.client.renderer.BlacklightHunterRenderer::new);
        event.registerEntityRenderer(ModEntities.BLACKLIGHT_VIRUS_VIAL.get(), ThrownItemRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ClawArmModel.LAYER, ClawArmModel::createLayer);
    }

    @SubscribeEvent
    public static void lockConsumptionMovement(net.neoforged.neoforge.client.event.MovementInputUpdateEvent event) {
        if (!event.getEntity().getData(ModAttachments.CONSUMING)
                || event.getEntity().getData(ModAttachments.GRAB_STAGE)==com.codex.blacklight.combat.ConsumeVisual.HOLD) return;
        var input = event.getInput();
        input.forwardImpulse = 0; input.leftImpulse = 0; input.jumping = false;
        input.up = false; input.down = false; input.left = false; input.right = false;
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getAction() != GLFW.GLFW_PRESS || minecraft.player == null
                || minecraft.getConnection() == null) return;

        var state = minecraft.player.getData(ModAttachments.PLAYER_STATE);
        if (minecraft.screen == null && PROTOTYPE_MODE.matches(event.getKey(), event.getScanCode())) {
            PacketDistributor.sendToServer(new PrototypeModePayload(!state.isPrototypeMode()));
            return;
        }
        if (minecraft.screen == null && UPGRADE_PANEL.matches(event.getKey(), event.getScanCode())) {
            if (state.isInfected()) minecraft.setScreen(new UpgradeScreen());
            return;
        }
        if (!state.isPrototypeMode()) return;
        if (minecraft.screen == null && CYCLE_FORM.matches(event.getKey(), event.getScanCode())) {
            PacketDistributor.sendToServer(new ClawActionPayload(ClawActionPayload.Action.CYCLE_FORM)); return;
        }
        if (minecraft.screen == null && GRAB.matches(event.getKey(), event.getScanCode())) {
            PacketDistributor.sendToServer(new ClawActionPayload(ClawActionPayload.Action.GRAB));
            minecraft.options.keyInventory.setDown(false);
            while(minecraft.options.keyInventory.consumeClick()) {}
            return;
        }

        if (minecraft.screen == null && CLAW_TUNER.matches(event.getKey(), event.getScanCode())) {
            minecraft.setScreen(new ClawTunerScreen());
            return;
        }
        if (minecraft.screen == null && ANIMATION_TUNER.matches(event.getKey(), event.getScanCode())) {
            minecraft.setScreen(new AnimationTunerScreen());
            return;
        }
        if (minecraft.screen != null) return;

        if (TARGET_LOCK.matches(event.getKey(), event.getScanCode())) {
            toggleTargetLock(minecraft);
            return;
        }

        if (TOGGLE_CLAWS.matches(event.getKey(), event.getScanCode())) {
            PacketDistributor.sendToServer(new ClawActionPayload(ClawActionPayload.Action.TOGGLE_CLAWS));
        }
    }

    /** Opens the timeline once for repeatable development screenshots and nothing else. */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        suppressPrototypePlayerList();
        tickGrabButtons();
        tickPhysicalLeftAttack();
        holdGroundSpikePlantPose();
        if (automaticTunerOpened || !Boolean.getBoolean("blacklight.test.openAnimationTuner")) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) return;
        automaticTunerOpened = true;
        BlacklightNeoMod.LOGGER.info("Opening animation timeline from development acceptance hook");
        minecraft.setScreen(new AnimationTunerScreen());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRawMouseButton(InputEvent.MouseButton.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) return;
        var state = minecraft.player.getData(ModAttachments.PLAYER_STATE);
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && event.getAction() == GLFW.GLFW_PRESS) {
            BlacklightNeoMod.LOGGER.debug(
                    "Observed physical left press infected={} prototype={} power={} ground={}",
                    state.isInfected(), state.isPrototypeMode(), state.getActivePower().id(),
                    minecraft.player.onGround());
        }
        if(state.isPrototypeMode() && minecraft.player.getData(ModAttachments.CONSUMING)
                && (event.getButton()==GLFW.GLFW_MOUSE_BUTTON_LEFT||event.getButton()==GLFW.GLFW_MOUSE_BUTTON_RIGHT)) {
            event.setCanceled(true);tickGrabButtons();return;
        }
        if (!state.isInfected() || !state.isPrototypeMode()
                || state.getActivePower() != PowerForm.CLAWS) return;

        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && (event.getAction() == GLFW.GLFW_PRESS
                || event.getAction() == GLFW.GLFW_RELEASE)) {
            // Only suppress the vanilla/Epic Fight attack here. Physical edges are polled
            // once per client tick below because Epic Fight can consume this callback
            // before all NeoForge mouse listeners observe it.
            event.setCanceled(true);
            if (event.getAction() == GLFW.GLFW_PRESS) {
                beginLeftAttack(minecraft);
            } else {
                finishLeftAttack(minecraft);
                lastPhysicalLeftDown = false;
            }
            return;
        }

        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT
                && (event.getAction() == GLFW.GLFW_PRESS
                || event.getAction() == GLFW.GLFW_RELEASE)) {
            // Groundspike is a one-shot planted action. RMB-down starts the authored
            // insertion and RMB-up is deliberately ignored; the server erupts exactly
            // when the hand reaches the planted marker, then the client holds that pose
            // until the spike visual completes and reverses the hand back to neutral.
            event.setCanceled(true);
            if (event.getAction() == GLFW.GLFW_PRESS) {
                if (groundSpikeHeld || groundSpikeAutoReleased || groundSpikeReversing) return;
                groundSpikeHeld = true;
                groundSpikeStartupWaitTicks = 30;
                PacketDistributor.sendToServer(new ClawActionPayload(
                        ClawActionPayload.Action.GROUND_SPIKE_CHARGE_START));
            }
            return;
        }
    }

    /** Raw callbacks and polling share the same edge latch: one click sends one intent. */
    private static void tickGrabButtons() {
        var mc=Minecraft.getInstance();if(mc.getWindow()==null)return;
        long window=mc.getWindow().getWindow();
        boolean left=GLFW.glfwGetMouseButton(window,GLFW.GLFW_MOUSE_BUTTON_LEFT)==GLFW.GLFW_PRESS;
        boolean right=GLFW.glfwGetMouseButton(window,GLFW.GLFW_MOUSE_BUTTON_RIGHT)==GLFW.GLFW_PRESS;
        if(mc.player!=null&&mc.screen==null&&mc.player.getData(ModAttachments.CONSUMING)) {
            clearLeftAttackHold();
            mc.options.keyAttack.setDown(false);mc.options.keyUse.setDown(false);
            if(mc.player.getData(ModAttachments.GRAB_STAGE)==com.codex.blacklight.combat.ConsumeVisual.HOLD) {
                if(right&&!lastGrabRight)PacketDistributor.sendToServer(new ClawActionPayload(ClawActionPayload.Action.CONSUME));
                else if(left&&!lastGrabLeft)PacketDistributor.sendToServer(new ClawActionPayload(ClawActionPayload.Action.THROW));
            }
        }
        lastGrabLeft=left;lastGrabRight=right;
    }

    private static void toggleTargetLock(Minecraft minecraft) {
        EpicFightCameraAPI.getInstance().toggleLockOn();
    }

    /** Tab belongs to lock-on while Prototype mode is active, not the player-list overlay. */
    private static void suppressPrototypePlayerList() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) return;
        var state = minecraft.player.getData(ModAttachments.PLAYER_STATE);
        if (state.isPrototypeMode() && TARGET_LOCK.isDown()) {
            minecraft.options.keyPlayerList.setDown(false);
        }
    }

    /** Used by local sprint prediction so the planted action wins immediately. */
    static boolean isGroundSpikeActionActive() {
        return groundSpikeHeld || groundSpikeAutoReleased || groundSpikeReversing;
    }

    /**
     * Polls GLFW directly so attacks remain reliable while WALK/RUN motion and Epic
     * Fight's own input engine are active. Also classifies air press, ground tap and hold.
     */
    private static void tickPhysicalLeftAttack() {
        Minecraft minecraft = Minecraft.getInstance();
        boolean physicallyDown = minecraft.getWindow() != null
                && GLFW.glfwGetMouseButton(minecraft.getWindow().getWindow(),
                GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) {
            clearLeftAttackHold();
            lastPhysicalLeftDown = physicallyDown;
            return;
        }
        var state = minecraft.player.getData(ModAttachments.PLAYER_STATE);
        if (minecraft.player.getData(ModAttachments.CONSUMING) || !state.isInfected() || !state.isPrototypeMode()
                || state.getActivePower() != PowerForm.CLAWS) {
            clearLeftAttackHold();
            lastPhysicalLeftDown = physicallyDown;
            return;
        }

        if (physicallyDown && !lastPhysicalLeftDown) {
            beginLeftAttack(minecraft);
        }

        if (physicallyDown && leftAttackHeld && !leftAttackConsumed
                && minecraft.player.onGround()
                && ++leftAttackHoldTicks >= DASHING_SLICE_HOLD_TICKS) {
            leftAttackConsumed = true;
            PacketDistributor.sendToServer(new ClawActionPayload(
                    ClawActionPayload.Action.DASHING_SLICE));
        }

        if (!physicallyDown && lastPhysicalLeftDown) {
            finishLeftAttack(minecraft);
        }
        lastPhysicalLeftDown = physicallyDown;
    }

    private static void beginLeftAttack(Minecraft minecraft) {
        if (leftAttackHeld) return;
        clearLeftAttackHold();
        leftAttackHeld = true;
        lastPhysicalLeftDown = true;
        if (!minecraft.player.onGround()) {
            leftAttackConsumed = true;
            ClientMovementEvents.requestAirDashFromAttack(minecraft);
            return;
        }
        // While sprinting, hold the tap in a short charge window instead of sending
        // it immediately: a long press then goes STRAIGHT into the dashing slice
        // with no normal swing telegraphed first, and a quick release still fires
        // the regular chain attack.
        if (ClientMovementEvents.isRunningForAttack(minecraft)) {
            leftAttackTapSent = false;
            return;
        }
        // Send the visible attack on the press edge. Long-hold recognition remains
        // active and can blend this startup into the upgraded dashing slice later.
        sendTapAttack(minecraft);
        leftAttackTapSent = true;
    }

    private static void finishLeftAttack(Minecraft minecraft) {
        if (leftAttackHeld && !leftAttackConsumed && !leftAttackTapSent) {
            sendTapAttack(minecraft);
        }
        clearLeftAttackHold();
    }

    private static void clearLeftAttackHold() {
        leftAttackHeld = false;
        leftAttackConsumed = false;
        leftAttackTapSent = false;
        leftAttackHoldTicks = 0;
    }

    private static void sendTapAttack(Minecraft minecraft) {
        // Walking and sprinting use the same full-body three-hit chain. The old
        // SPRINT_ATTACK branch selected an empty composite placeholder while running,
        // which made the accepted click look like no attack at all. Long LMB still
        // upgrades this startup into DASHING_SLICE after the hold threshold.
        PacketDistributor.sendToServer(new ClawActionPayload(
                ClawActionPayload.Action.ATTACK));
    }

    /** Freezes the local Epic Fight player exactly at the right-claw planted frame. */
    private static void holdGroundSpikePlantPose() {
        if (groundSpikeAutoReleased) {
            tickGroundSpikeEruptionHold();
            return;
        }
        if (groundSpikeReversing) {
            tickGroundSpikeReverse();
            return;
        }
        if (!groundSpikeHeld) {
            releaseGroundSpikePlantPose();
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            groundSpikeHeld = false;
            groundSpikePosePaused = false;
            return;
        }
        var state = minecraft.player.getData(ModAttachments.PLAYER_STATE);
        if (!state.isInfected() || !state.isPrototypeMode()
                || state.getActivePower() != PowerForm.CLAWS) {
            groundSpikeHeld = false;
            releaseGroundSpikePlantPose();
            return;
        }
        // Starting the action while running is allowed. The action itself arrests all
        // horizontal motion and remains planted until the reversed withdrawal ends.
        var velocity = minecraft.player.getDeltaMovement();
        minecraft.player.setDeltaMovement(0.0D, velocity.y, 0.0D);
        AnimationPlayer player = groundSpikeAnimationPlayer(minecraft);
        if (player == null) {
            if (groundSpikeStartupWaitTicks-- <= 0) {
                BlacklightNeoMod.LOGGER.debug(
                        "Groundspike start was rejected or never reached the client animator");
                clearGroundSpikePlaybackState(minecraft);
            }
            return;
        }
        groundSpikeStartupWaitTicks = 0;
        if (player.getElapsedTime()
                >= GROUND_SPIKE_PLANT_TIME - GROUND_SPIKE_END_CAPTURE_WINDOW) {
            player.setElapsedTime(GROUND_SPIKE_PLANT_TIME, GROUND_SPIKE_PLANT_TIME);
            Animator animator = groundSpikeAnimator(minecraft);
            if (animator != null) animator.setHardPause(true);
            if (!groundSpikePosePaused) {
                BlacklightNeoMod.LOGGER.debug(
                        "Groundspike reached authored final frame; auto-releasing eruption");
                groundSpikePosePaused = true;
                groundSpikeAutoReleased = true;
                groundSpikeEruptionTicks = GroundSpikeEntity.ERUPTION_DURATION_TICKS;
            }
        }
    }

    /** Holds the planted frame until the 1.1 second spike visual has fully finished. */
    private static void tickGroundSpikeEruptionHold() {
        Minecraft minecraft = Minecraft.getInstance();
        AnimationPlayer player = groundSpikeAnimationPlayer(minecraft);
        Animator animator = groundSpikeAnimator(minecraft);
        if (player == null || animator == null) {
            clearGroundSpikePlaybackState(minecraft);
            return;
        }
        player.setElapsedTime(GROUND_SPIKE_PLANT_TIME, GROUND_SPIKE_PLANT_TIME);
        animator.setHardPause(true);
        if (groundSpikeEruptionTicks-- > 0) return;

        groundSpikeAutoReleased = false;
        groundSpikeReversing = true;
        groundSpikePosePaused = false;
        player.setElapsedTime(GROUND_SPIKE_PLANT_TIME, GROUND_SPIKE_PLANT_TIME);
        player.setReversed(true);
        animator.setHardPause(false);
        BlacklightNeoMod.LOGGER.debug(
                "Groundspike eruption finished; reversing authored action to neutral");
    }

    /** Lets Epic Fight play the same accelerated timeline backward exactly once. */
    private static void tickGroundSpikeReverse() {
        Minecraft minecraft = Minecraft.getInstance();
        AnimationPlayer player = groundSpikeAnimationPlayer(minecraft);
        if (player == null) {
            clearGroundSpikePlaybackState(minecraft);
            return;
        }
        if (player.getElapsedTime() > 0.001F && !player.isEnd()) return;
        player.setReversed(false);
        groundSpikeReversing = false;
        groundSpikeHeld = false;
        BlacklightNeoMod.LOGGER.debug("Groundspike authored reverse recovery completed");
    }

    private static void releaseGroundSpikePlantPose() {
        if (!groundSpikePosePaused) return;
        Animator animator = groundSpikeAnimator(Minecraft.getInstance());
        if (animator != null) {
            animator.setHardPause(false);
            BlacklightNeoMod.LOGGER.debug(
                    "Groundspike plant-frame pause released into authored recovery");
        }
        groundSpikePosePaused = false;
    }

    private static void clearGroundSpikePlaybackState(Minecraft minecraft) {
        Animator animator = groundSpikeAnimator(minecraft);
        if (animator != null && groundSpikePosePaused) animator.setHardPause(false);
        groundSpikeHeld = false;
        groundSpikePosePaused = false;
        groundSpikeAutoReleased = false;
        groundSpikeReversing = false;
        groundSpikeEruptionTicks = 0;
        groundSpikeStartupWaitTicks = 0;
    }

    private static AnimationPlayer groundSpikeAnimationPlayer(Minecraft minecraft) {
        Animator animator = groundSpikeAnimator(minecraft);
        if (animator == null) return null;
        // getPlayerFor on a BASE_LAYER accessor unconditionally returns the base
        // layer's player, so identity must be checked against the REAL animation
        // (the same comparison Epic Fight's isPlaying uses). Comparing the raw
        // accessor against the loaded animation never matched and made the whole
        // plant-pose state machine give up after its startup grace window.
        AnimationPlayer player = animator.getPlayerFor(BlacklightEpicFightAnimations.CLAW_GROUNDSPIKE);
        if (player == null) return null;
        var real = player.getRealAnimation();
        return real != null && (real.equals(BlacklightEpicFightAnimations.CLAW_GROUNDSPIKE)
                || real.equals(BlacklightEpicFightAnimations.CLAW_GROUNDSPIKE.get()))
                ? player : null;
    }

    private static Animator groundSpikeAnimator(Minecraft minecraft) {
        if (minecraft.player == null || BlacklightEpicFightAnimations.CLAW_GROUNDSPIKE == null) {
            return null;
        }
        AbstractClientPlayerPatch<?> patch = EpicFightCapabilities.getEntityPatch(
                minecraft.player, AbstractClientPlayerPatch.class);
        return patch == null ? null : patch.getAnimator();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttack(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) return;
        var state = minecraft.player.getData(ModAttachments.PLAYER_STATE);
        if(state.isPrototypeMode()&&minecraft.player.getData(ModAttachments.CONSUMING)) {
            event.setSwingHand(false);event.setCanceled(true);tickGrabButtons();return;
        }
        if (!state.isInfected() || !state.isPrototypeMode()
                || state.getActivePower() != PowerForm.CLAWS) return;

        if (event.isUseItem()) {
            event.setSwingHand(false);
            event.setCanceled(true);
            return;
        }
        if (!event.isAttack()) return;

        event.setSwingHand(false);
        event.setCanceled(true);
        // This mapping event is the reliable fallback when Epic Fight consumes the raw
        // mouse callback over air or an entity. beginLeftAttack is edge-deduplicated.
        beginLeftAttack(minecraft);
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !PrototypeSurvivalLock.isLocked(minecraft.player)
                || !(event.getNewScreen() instanceof AbstractContainerScreen<?>)
                && !(event.getNewScreen() instanceof InventoryScreen)) return;
        event.setCanceled(true);
        if (!GRAB.isDown()) PrototypeSurvivalLock.notifyClient(minecraft.player);
    }

    private static void registerEpicFightLayerHook() {
        if (layerHookRegistered) return;
        layerHookRegistered = true;
        BlacklightNeoMod.LOGGER.info("Registering Blacklight Epic Fight player renderer");
        EpicFightClientEventHooks.Registry.ADD_PATCHED_ENTITY.registerEvent(event -> {
            event.addPatchedEntityRenderer(EntityType.PLAYER,
                    type -> new BlacklightPlayerRenderer(event.getContext(), type));
            event.addPatchedEntityRenderer(EntityType.ZOMBIE,
                    type -> new com.codex.blacklight.client.renderer.InfectedZombieRenderer(event.getContext(), type).initLayerLast(event.getContext(), type));
            BlacklightNeoMod.LOGGER.info("Replaced Epic Fight player renderer with biological limb renderer");
        });
    }
}
