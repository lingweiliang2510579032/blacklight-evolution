package com.codex.blacklight.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client sends a switch intent only; identity and eligibility belong to the server. */
public record DisguiseTogglePayload() implements CustomPacketPayload {
    public static final Type<DisguiseTogglePayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("blacklight","disguise_toggle"));
    public static final StreamCodec<RegistryFriendlyByteBuf,DisguiseTogglePayload> CODEC=StreamCodec.unit(new DisguiseTogglePayload());
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
