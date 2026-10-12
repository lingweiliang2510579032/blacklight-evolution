package com.codex.blacklight.network;

import net.minecraft.network.FriendlyByteBuf;
import com.codex.blacklight.forge.codec.StreamCodec;
import com.codex.blacklight.forge.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client sends a switch intent only; identity and eligibility belong to the server. */
public record DisguiseTogglePayload() implements CustomPacketPayload {
    public static final Type<DisguiseTogglePayload> TYPE=new Type<>(new ResourceLocation("blacklight","disguise_toggle"));
    public static final StreamCodec<FriendlyByteBuf,DisguiseTogglePayload> CODEC=StreamCodec.unit(new DisguiseTogglePayload());
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
