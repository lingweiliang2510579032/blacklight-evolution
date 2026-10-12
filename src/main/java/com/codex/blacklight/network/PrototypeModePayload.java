package com.codex.blacklight.network;

import com.codex.blacklight.BlacklightNeoMod;
import net.minecraft.network.FriendlyByteBuf;
import com.codex.blacklight.forge.codec.ByteBufCodecs;
import com.codex.blacklight.forge.codec.StreamCodec;
import com.codex.blacklight.forge.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Requests an explicit switch between vanilla survival and Prototype mode. */
public record PrototypeModePayload(boolean enabled) implements CustomPacketPayload {
    public static final Type<PrototypeModePayload> TYPE = new Type<>(
            new ResourceLocation(BlacklightNeoMod.MOD_ID, "prototype_mode"));
    public static final StreamCodec<FriendlyByteBuf, PrototypeModePayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, PrototypeModePayload::enabled,
                    PrototypeModePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
