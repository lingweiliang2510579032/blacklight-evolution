package com.codex.blacklight.network;

import com.codex.blacklight.BlacklightNeoMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Requests an explicit switch between vanilla survival and Prototype mode. */
public record PrototypeModePayload(boolean enabled) implements CustomPacketPayload {
    public static final Type<PrototypeModePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BlacklightNeoMod.MOD_ID, "prototype_mode"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PrototypeModePayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, PrototypeModePayload::enabled,
                    PrototypeModePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
