package com.codex.blacklight.network;

import net.minecraft.network.FriendlyByteBuf;
import com.codex.blacklight.forge.codec.ByteBufCodecs;
import com.codex.blacklight.forge.codec.StreamCodec;
import com.codex.blacklight.forge.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** The canonical LivingEntity parent ID is sent for multipart targets; never a
 * client-only limb ID. -1 releases the lock; the server validates the parent. */
public record TargetLockPayload(int entityId,int requestId) implements CustomPacketPayload {
    public static final Type<TargetLockPayload> TYPE=new Type<>(new ResourceLocation("blacklight","target_lock"));
    public static final StreamCodec<FriendlyByteBuf,TargetLockPayload> STREAM_CODEC=StreamCodec.composite(
            ByteBufCodecs.VAR_INT,TargetLockPayload::entityId,ByteBufCodecs.VAR_INT,TargetLockPayload::requestId,TargetLockPayload::new);
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
