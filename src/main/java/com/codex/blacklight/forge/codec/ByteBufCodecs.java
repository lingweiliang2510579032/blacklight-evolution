package com.codex.blacklight.forge.codec;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.network.FriendlyByteBuf;
import io.netty.handler.codec.DecoderException;

/** Primitive codecs use the actual Minecraft 1.20.1 wire representation. */
public final class ByteBufCodecs {
    public static final StreamCodec<FriendlyByteBuf, Boolean> BOOL = StreamCodec.of(FriendlyByteBuf::writeBoolean, FriendlyByteBuf::readBoolean);
    public static final StreamCodec<FriendlyByteBuf, Integer> VAR_INT = StreamCodec.of(FriendlyByteBuf::writeVarInt, FriendlyByteBuf::readVarInt);
    public static final StreamCodec<FriendlyByteBuf, Long> VAR_LONG = StreamCodec.of(FriendlyByteBuf::writeVarLong, FriendlyByteBuf::readVarLong);
    public static final StreamCodec<FriendlyByteBuf, Float> FLOAT = StreamCodec.of(FriendlyByteBuf::writeFloat, FriendlyByteBuf::readFloat);
    public static final StreamCodec<FriendlyByteBuf, String> STRING_UTF8 = StreamCodec.of(FriendlyByteBuf::writeUtf, FriendlyByteBuf::readUtf);
    public static final StreamCodec<FriendlyByteBuf, CompoundTag> COMPOUND_TAG = StreamCodec.of(FriendlyByteBuf::writeNbt, buffer -> {
        CompoundTag value = buffer.readNbt(new NbtAccounter(65_536));
        if (value == null) throw new DecoderException("Missing Blacklight compound tag");
        return value;
    });

    private ByteBufCodecs() {}
}
