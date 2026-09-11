package com.codex.blacklight.network;

import com.codex.blacklight.BlacklightNeoMod;
import com.codex.blacklight.movement.MovementTuningSnapshot;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server-to-client snapshot that keeps movement prediction aligned with server physics. */
public record MovementTuningPayload(MovementTuningSnapshot snapshot) implements CustomPacketPayload {
    public static final Type<MovementTuningPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BlacklightNeoMod.MOD_ID, "movement_tuning"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MovementTuningPayload> STREAM_CODEC =
            StreamCodec.of(MovementTuningPayload::encode, MovementTuningPayload::decode);

    private static void encode(RegistryFriendlyByteBuf buffer, MovementTuningPayload payload) {
        MovementTuningSnapshot value = payload.snapshot;
        buffer.writeDouble(value.sprintAcceleration());
        buffer.writeDouble(value.sprintMaxSpeed());
        buffer.writeDouble(value.jumpMinVelocity());
        buffer.writeDouble(value.jumpMaxVelocity());
        buffer.writeDouble(value.jumpChargeTicks());
        buffer.writeDouble(value.wallDetectDistance());
        buffer.writeDouble(value.wallClimbVelocity());
        buffer.writeDouble(value.glideFallSpeed());
        buffer.writeDouble(value.glideMaxSpeed());
        buffer.writeDouble(value.airDashSpeed());
        buffer.writeDouble(value.airDashTicks());
        buffer.writeDouble(value.fovBoost());
    }

    private static MovementTuningPayload decode(RegistryFriendlyByteBuf buffer) {
        return new MovementTuningPayload(new MovementTuningSnapshot(
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble()));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
