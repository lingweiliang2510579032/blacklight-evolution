package com.codex.blacklight.forge;

import com.codex.blacklight.forge.codec.StreamCodec;
import com.mojang.serialization.Codec;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;

/** Metadata for one field in the Forge Blacklight entity capability. */
public final class AttachmentType<T> {
    private final Supplier<T> factory;
    private final Codec<T> persistentCodec;
    private final StreamCodec<FriendlyByteBuf, T> syncCodec;
    private final boolean copyOnDeath;

    private AttachmentType(Builder<T> builder) {
        factory = builder.factory;
        persistentCodec = builder.persistentCodec;
        syncCodec = builder.syncCodec;
        copyOnDeath = builder.copyOnDeath;
    }

    public T createDefault() { return factory.get(); }
    public Codec<T> persistentCodec() { return persistentCodec; }
    public StreamCodec<FriendlyByteBuf, T> syncCodec() { return syncCodec; }
    public boolean copiesOnDeath() { return copyOnDeath; }
    public static <T> Builder<T> builder(Supplier<T> factory) { return new Builder<>(factory); }

    public static final class Builder<T> {
        private final Supplier<T> factory;
        private Codec<T> persistentCodec;
        private StreamCodec<FriendlyByteBuf, T> syncCodec;
        private boolean copyOnDeath;

        private Builder(Supplier<T> factory) { this.factory = factory; }
        public Builder<T> serialize(Codec<T> codec) { persistentCodec = codec; return this; }
        public Builder<T> copyOnDeath() { copyOnDeath = true; return this; }
        public Builder<T> sync(StreamCodec<FriendlyByteBuf, T> codec) { syncCodec = codec; return this; }
        public AttachmentType<T> build() { return new AttachmentType<>(this); }
    }
}
