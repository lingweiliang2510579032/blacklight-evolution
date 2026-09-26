package com.codex.blacklight.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** -1 releases the explicit lock; the server validates all requested targets. */
public record TargetLockPayload(int entityId) implements CustomPacketPayload {
    public static final Type<TargetLockPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("blacklight","target_lock"));
    public static final StreamCodec<RegistryFriendlyByteBuf,TargetLockPayload> STREAM_CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,TargetLockPayload::entityId,TargetLockPayload::new);
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
