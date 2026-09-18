package com.codex.blacklight.combat;

import com.codex.blacklight.power.PowerForm;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Transient phase snapshot; capture/holding never advance the dissolve clock. */
public record ConsumeVisual(int consumerId, long started, int duration, int form, int stage) {
    public static final int NONE=0, CAPTURE=1, HOLD=2, CONSUME=3, THROW=4, FLIGHT=5;
    public static final ConsumeVisual EMPTY = new ConsumeVisual(-1,0,46,0,NONE);
    public ConsumeVisual(int owner,long start,int duration,int form) { this(owner,start,duration,form,CONSUME); }
    public static final StreamCodec<RegistryFriendlyByteBuf, ConsumeVisual> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ConsumeVisual::consumerId,
            ByteBufCodecs.VAR_LONG, ConsumeVisual::started,
            ByteBufCodecs.VAR_INT, ConsumeVisual::duration,
            ByteBufCodecs.VAR_INT, ConsumeVisual::form,
            ByteBufCodecs.VAR_INT, ConsumeVisual::stage, ConsumeVisual::new);
    public boolean active() { return consumerId >= 0; }
    public boolean consuming() { return active() && stage==CONSUME; }
    public float phaseProgress(long now,float partial) { return Math.clamp((now-started+partial)/Math.max(1,duration),0,1); }
    public float progress(long now,float partial) { return consuming()?phaseProgress(now,partial):0; }
    public static int captureDuration(PowerForm form) { return form==PowerForm.WHIPFIST?36:12; }
    public static int duration(PowerForm form) {
        return switch(form) { case HAMMERFISTS->56; case BLADE->48; case WHIPFIST->38; default->46; };
    }
}
