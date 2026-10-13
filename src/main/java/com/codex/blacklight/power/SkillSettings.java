package com.codex.blacklight.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.codex.blacklight.forge.codec.ByteBufCodecs;
import com.codex.blacklight.forge.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

/** Personal gameplay choices persist independently of learned skills. */
public record SkillSettings(boolean bulletdiveBlockBreaking) {
    public static final SkillSettings DEFAULT=new SkillSettings(true);
    // DFU 6 optionalFieldOf silently defaults malformed values. Preserve the
    // newer platform's strict preference decoding while allowing missing old data.
    public static final Codec<SkillSettings> CODEC=new com.mojang.serialization.MapCodec<SkillSettings>() {
        @Override public <T> com.mojang.serialization.DataResult<SkillSettings> decode(
                com.mojang.serialization.DynamicOps<T> ops,com.mojang.serialization.MapLike<T> input) {
            T value=input.get("bulletdive_block_breaking");
            if(value==null)return com.mojang.serialization.DataResult.success(DEFAULT);
            if(ops.getStringValue(value).result().isPresent())
                return com.mojang.serialization.DataResult.error(()->"Expected a boolean for bulletdive_block_breaking");
            return Codec.BOOL.parse(ops,value).map(SkillSettings::new);
        }
        @Override public <T> com.mojang.serialization.RecordBuilder<T> encode(SkillSettings value,
                com.mojang.serialization.DynamicOps<T> ops,com.mojang.serialization.RecordBuilder<T> prefix) {
            return prefix.add("bulletdive_block_breaking",ops.createBoolean(value.bulletdiveBlockBreaking()));
        }
        @Override public <T> java.util.stream.Stream<T> keys(com.mojang.serialization.DynamicOps<T> ops) {
            return java.util.stream.Stream.of(ops.createString("bulletdive_block_breaking"));
        }
    }.codec();
    public static final StreamCodec<FriendlyByteBuf,SkillSettings> STREAM_CODEC=ByteBufCodecs.BOOL
            .map(SkillSettings::new,SkillSettings::bulletdiveBlockBreaking);
}
