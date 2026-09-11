package com.codex.blacklight.network;

import com.codex.blacklight.BlacklightNeoMod;
import com.codex.blacklight.movement.LandingImpact;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Deterministic landing fracture visual synchronized to tracking clients. */
public record LandingVisualPayload(int entityId, LandingImpact.Strength strength,
                                   BlockPos surfacePos, float fallDistance,
                                   float radius, long seed)
        implements CustomPacketPayload {
    public static final Type<LandingVisualPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BlacklightNeoMod.MOD_ID, "landing_visual"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LandingVisualPayload> STREAM_CODEC =
            StreamCodec.of(LandingVisualPayload::encode, LandingVisualPayload::decode);

    private static void encode(RegistryFriendlyByteBuf buffer, LandingVisualPayload payload) {
        buffer.writeVarInt(payload.entityId);
        buffer.writeVarInt(payload.strength.ordinal());
        buffer.writeBlockPos(payload.surfacePos);
        buffer.writeFloat(payload.fallDistance);
        buffer.writeFloat(payload.radius);
        buffer.writeLong(payload.seed);
    }

    private static LandingVisualPayload decode(RegistryFriendlyByteBuf buffer) {
        int entityId = buffer.readVarInt();
        int id = buffer.readVarInt();
        LandingImpact.Strength[] values = LandingImpact.Strength.values();
        LandingImpact.Strength strength = id >= 0 && id < values.length
                ? values[id] : LandingImpact.Strength.NONE;
        return new LandingVisualPayload(entityId, strength,
                buffer.readBlockPos(), buffer.readFloat(), buffer.readFloat(),
                buffer.readLong());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
