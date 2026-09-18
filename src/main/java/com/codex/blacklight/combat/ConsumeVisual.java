package com.codex.blacklight.combat;

import com.codex.blacklight.power.PowerForm;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** One transient, atomic tracking snapshot. World time keeps distant clients in phase. */
public record ConsumeVisual(int consumerId, long started, int duration, int form) {
    public static final ConsumeVisual EMPTY = new ConsumeVisual(-1, 0, 46, 0);
    public static final StreamCodec<RegistryFriendlyByteBuf, ConsumeVisual> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ConsumeVisual::consumerId,
            ByteBufCodecs.VAR_LONG, ConsumeVisual::started,
            ByteBufCodecs.VAR_INT, ConsumeVisual::duration,
            ByteBufCodecs.VAR_INT, ConsumeVisual::form, ConsumeVisual::new);
    public boolean active() { return consumerId >= 0; }
    public float progress(long now, float partial) { return Math.clamp((now - started + partial) / duration, 0, 1); }
    public static int duration(PowerForm form) {
        return switch (form) { case HAMMERFISTS -> 56; case BLADE -> 48; case WHIPFIST -> 60; default -> 46; };
    }
}
