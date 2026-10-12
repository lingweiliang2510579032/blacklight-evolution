package com.codex.blacklight.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import com.codex.blacklight.forge.codec.StreamCodec;

/** Shield durability survives toggling and reconnecting; visual timestamps are server time. */
public record UtilityState(float shield, long recoverAt, long changedAt, int previousDefense, long hitAt) {
    public static final float SHIELD_CAPACITY = 250;
    // ShieldPower: health=250, healingTime=10s, undamagedTimeBeforeHealing=1s,
    // autoHealFromBrokenTime=.25s. See docs/RC29-utility-source.md.
    public static final int REBUILD_TICKS = 205;
    public static final int HEAL_DELAY_TICKS = 20;
    public static final float HEAL_PER_TICK = SHIELD_CAPACITY / 200F;
    public static final UtilityState EMPTY = new UtilityState(SHIELD_CAPACITY, 0, -1000, 0, -1000);
    public UtilityState { shield = Float.isFinite(shield) ? com.codex.blacklight.util.MathCompat.clamp(shield, 0, SHIELD_CAPACITY) : 0; }
    public static final Codec<UtilityState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.optionalFieldOf("shield", SHIELD_CAPACITY).forGetter(UtilityState::shield),
            Codec.LONG.optionalFieldOf("recoverAt", 0L).forGetter(UtilityState::recoverAt),
            Codec.LONG.optionalFieldOf("hitAt", -1000L).forGetter(UtilityState::hitAt)
    ).apply(i, (hp, at, hit) -> new UtilityState(hp, at, -1000, 0, hit)));
    public static final StreamCodec<FriendlyByteBuf, UtilityState> STREAM_CODEC = new StreamCodec<>() {
        public UtilityState decode(FriendlyByteBuf b) { return new UtilityState(b.readFloat(), b.readLong(), b.readLong(), b.readVarInt(), b.readLong()); }
        public void encode(FriendlyByteBuf b, UtilityState s) { b.writeFloat(s.shield); b.writeLong(s.recoverAt); b.writeLong(s.changedAt); b.writeVarInt(s.previousDefense); b.writeLong(s.hitAt); }
    };
    public boolean broken() { return shield <= 0; }
    /** Native FIG shield mesh states at 60% and 30%; zero is the broken root. */
    public int damageStage() { return broken() ? 3 : shield > SHIELD_CAPACITY * .6F ? 0 : shield > SHIELD_CAPACITY * .3F ? 1 : 2; }
    public float rebuildProgress(double now) { return broken() ? com.codex.blacklight.util.MathCompat.clamp((float)(1 - (recoverAt - now) / 200D), 0, 1) : 1; }
}
