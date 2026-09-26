package com.codex.blacklight.network;

import com.codex.blacklight.BlacklightNeoMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Only a requested form id crosses the wire; no unlock or purchase flags. */
public record PowerSelectPayload(int formId) implements CustomPacketPayload {
    public static final Type<PowerSelectPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(BlacklightNeoMod.MOD_ID,"power_select"));
    public static final StreamCodec<RegistryFriendlyByteBuf,PowerSelectPayload> STREAM_CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,PowerSelectPayload::formId,PowerSelectPayload::new);
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
