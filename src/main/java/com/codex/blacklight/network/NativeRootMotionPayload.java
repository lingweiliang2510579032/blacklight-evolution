package com.codex.blacklight.network;

import net.minecraft.network.FriendlyByteBuf;
import com.codex.blacklight.forge.codec.StreamCodec;
import com.codex.blacklight.forge.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** The actor receives the actual server collision result without a camera rotation teleport. */
public record NativeRootMotionPayload(long gameTime, long revision, double x, double y, double z)
        implements CustomPacketPayload {
    public static final Type<NativeRootMotionPayload> TYPE = new Type<>(
            new ResourceLocation("blacklight", "root_motion"));
    public static final StreamCodec<FriendlyByteBuf, NativeRootMotionPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public NativeRootMotionPayload decode(FriendlyByteBuf b) {
            return new NativeRootMotionPayload(b.readVarLong(), b.readVarLong(), b.readDouble(), b.readDouble(), b.readDouble());
        }
        @Override public void encode(FriendlyByteBuf b, NativeRootMotionPayload p) {
            b.writeVarLong(p.gameTime); b.writeVarLong(p.revision); b.writeDouble(p.x); b.writeDouble(p.y); b.writeDouble(p.z);
        }
    };
    public NativeRootMotionPayload {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z))
            throw new IllegalArgumentException("Non-finite root motion position");
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
