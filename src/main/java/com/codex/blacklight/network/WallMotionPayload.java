package com.codex.blacklight.network;

import com.codex.blacklight.BlacklightNeoMod;
import com.codex.blacklight.movement.WallTraversal;
import net.minecraft.network.FriendlyByteBuf;
import com.codex.blacklight.forge.codec.ByteBufCodecs;
import com.codex.blacklight.forge.codec.StreamCodec;
import com.codex.blacklight.forge.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record WallMotionPayload(int entityId, int mode, float normalX, float normalZ) implements CustomPacketPayload {
    public static final Type<WallMotionPayload> TYPE=new Type<>(new ResourceLocation(BlacklightNeoMod.MOD_ID,"wall_motion"));
    public static final StreamCodec<FriendlyByteBuf,WallMotionPayload> STREAM_CODEC=StreamCodec.composite(
            ByteBufCodecs.VAR_INT,WallMotionPayload::entityId,ByteBufCodecs.VAR_INT,WallMotionPayload::mode,
            ByteBufCodecs.FLOAT,WallMotionPayload::normalX,ByteBufCodecs.FLOAT,WallMotionPayload::normalZ,WallMotionPayload::new);
    public WallTraversal.Mode wallMode() { return mode>=0 && mode<WallTraversal.Mode.values().length ? WallTraversal.Mode.values()[mode] : WallTraversal.Mode.NONE; }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
