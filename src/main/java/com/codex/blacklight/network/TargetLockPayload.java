package com.codex.blacklight.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** The canonical LivingEntity parent ID is sent for multipart targets; never a
 * client-only limb ID. -1 releases the lock; the server validates the parent. */
public record TargetLockPayload(int entityId,int requestId) implements CustomPacketPayload {
    public static final Type<TargetLockPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("blacklight","target_lock"));
    public static final StreamCodec<RegistryFriendlyByteBuf,TargetLockPayload> STREAM_CODEC=StreamCodec.composite(
            ByteBufCodecs.VAR_INT,TargetLockPayload::entityId,ByteBufCodecs.VAR_INT,TargetLockPayload::requestId,TargetLockPayload::new);
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
