package com.codex.blacklight.animation;

import net.minecraft.network.FriendlyByteBuf;
import com.codex.blacklight.forge.codec.StreamCodec;

/** Transient server-owned action clock, synchronized to the actor and tracking clients. */
public record NativeActionState(String clipId, long started, float clipOffset, float transition,
                                float duration, float yaw, boolean looping, float heldTime, long revision) {
    public static final NativeActionState EMPTY = new NativeActionState("", 0, 0, 0, 0, 0, false, -1, 0);
    public static final StreamCodec<FriendlyByteBuf, NativeActionState> CODEC = new StreamCodec<>() {
        @Override public NativeActionState decode(FriendlyByteBuf buffer) {
            return new NativeActionState(buffer.readUtf(128), buffer.readVarLong(), buffer.readFloat(),
                    buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readBoolean(),
                    buffer.readFloat(), buffer.readVarLong());
        }
        @Override public void encode(FriendlyByteBuf buffer, NativeActionState state) {
            buffer.writeUtf(state.clipId, 128); buffer.writeVarLong(state.started);
            buffer.writeFloat(state.clipOffset); buffer.writeFloat(state.transition);
            buffer.writeFloat(state.duration); buffer.writeFloat(state.yaw);
            buffer.writeBoolean(state.looping); buffer.writeFloat(state.heldTime); buffer.writeVarLong(state.revision);
        }
    };

    public NativeActionState {
        if (clipId == null || clipId.length() > 128 || !clipId.isEmpty() && !clipId.matches("[a-z0-9_]{1,80}")
                || !Float.isFinite(clipOffset) || !Float.isFinite(transition) || !Float.isFinite(duration)
                || !Float.isFinite(yaw) || !Float.isFinite(heldTime)
                || clipOffset < 0 || transition < 0 || duration < 0 || heldTime < -1
                || !clipId.isEmpty() && (duration <= 0 || clipOffset > duration || heldTime > duration))
            throw new IllegalArgumentException("Invalid player action state");
    }

    /** Uses double world ticks so animation remains accurate in long-running worlds. */
    public float seconds(double worldTicks) {
        if (clipId.isEmpty()) return 0;
        if (heldTime >= 0) return heldTime;
        double seconds = clipOffset + Math.max(0, (worldTicks - started) / 20.0 - transition);
        return looping ? (float)(seconds % duration) : (float)Math.min(duration, seconds);
    }

    public boolean active(double worldTicks) {
        if (clipId.isEmpty()) return false;
        return looping || heldTime >= 0 || (worldTicks - started) / 20.0 < transition + duration - clipOffset;
    }
}
