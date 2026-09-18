package com.codex.blacklight.network;

import com.codex.blacklight.client.ClientPhysicalFeedback;
import com.codex.blacklight.client.ClientClawAnimation;
import com.codex.blacklight.client.ClientLandingVisuals;
import com.codex.blacklight.combat.ClawCombatSystem;
import com.codex.blacklight.movement.PrototypeMovementSystem;
import com.codex.blacklight.movement.MovementTuningSettings;
import com.codex.blacklight.mode.PrototypeModeSystem;
import com.codex.blacklight.power.UpgradeSystem;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class ModNetwork {
    private ModNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("9")
                .playToServer(ClawActionPayload.TYPE, ClawActionPayload.STREAM_CODEC, (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player && canAct(player)) {
                        context.enqueueWork(() -> ClawCombatSystem.handle(player, payload.action()));
                    }
                })
                .playToServer(MovementInputPayload.TYPE, MovementInputPayload.STREAM_CODEC, (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player && canAct(player)) {
                        context.enqueueWork(() -> {
                            if (!PrototypeModeSystem.isActive(player)) return;
                            PrototypeMovementSystem.acceptInput(player, payload.jumpHeld(),
                                    payload.dashPressed(), payload.forward(), payload.sideways(), payload.sprinting(), payload.detach());
                        });
                    }
                })
                .playToServer(PrototypeModePayload.TYPE, PrototypeModePayload.STREAM_CODEC, (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player && canAct(player)) {
                        context.enqueueWork(() -> PrototypeModeSystem.handle(player, payload.enabled()));
                    }
                })
                .playToServer(UpgradePurchasePayload.TYPE, UpgradePurchasePayload.STREAM_CODEC, (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player && canAct(player)) {
                        context.enqueueWork(() -> UpgradeSystem.purchase(player, payload.upgradeId()));
                    }
                })
                .playToClient(MovementTuningPayload.TYPE, MovementTuningPayload.STREAM_CODEC, (payload, context) -> {
                    context.enqueueWork(() -> MovementTuningSettings.applyServerSnapshot(payload.snapshot()));
                })
                .playToClient(PhysicalFeedbackPayload.TYPE, PhysicalFeedbackPayload.STREAM_CODEC, (payload, context) -> {
                    context.enqueueWork(() -> ClientPhysicalFeedback.accept(payload));
                })
                .playToClient(ClawAnimationPayload.TYPE, ClawAnimationPayload.STREAM_CODEC, (payload, context) -> {
                    context.enqueueWork(() -> ClientClawAnimation.accept(payload));
                })
                .playToClient(WallMotionPayload.TYPE, WallMotionPayload.STREAM_CODEC, (payload, context) -> {
                    context.enqueueWork(() -> com.codex.blacklight.client.ClientWallMotion.accept(payload));
                })
                .playToClient(LandingVisualPayload.TYPE, LandingVisualPayload.STREAM_CODEC, (payload, context) -> {
                    context.enqueueWork(() -> ClientLandingVisuals.accept(payload));
                });
    }

    private static boolean canAct(ServerPlayer player) {
        return player != null && player.isAlive() && !player.isSpectator() && !player.isDeadOrDying();
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player,
                    new MovementTuningPayload(MovementTuningSettings.snapshot()));
        }
    }
}
