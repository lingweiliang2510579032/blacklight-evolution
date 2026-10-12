package com.codex.blacklight.forge;

import com.codex.blacklight.BlacklightNeoMod;
import com.codex.blacklight.registry.ModAttachments;
import com.mojang.serialization.DataResult;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

/** Persistent and transient values live together, but only explicitly serialized fields reach disk. */
public final class BlacklightAttachments implements ICapabilitySerializable<CompoundTag> {
    public static final Capability<BlacklightAttachments> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});
    private final Map<AttachmentType<?>, Object> values = new IdentityHashMap<>();
    private LazyOptional<BlacklightAttachments> optional = LazyOptional.of(() -> this);

    @SuppressWarnings("unchecked")
    public <T> T get(AttachmentType<T> type) {
        return (T) values.computeIfAbsent(type, ignored -> type.createDefault());
    }

    public <T> T set(AttachmentType<T> type, T value) {
        T previous = get(type);
        values.put(type, Objects.requireNonNull(value));
        return previous;
    }

    public boolean has(AttachmentType<?> type) { return values.containsKey(type); }
    public void remove(AttachmentType<?> type) { values.remove(type); }
    public void invalidate() { optional.invalidate(); }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability != CAPABILITY) return LazyOptional.empty();
        // Forge reviveCaps restores access to the dispatcher, but does not revive a provider's LazyOptional.
        // The entity dispatcher gates removed entities; a restored call must expose this same stored state.
        if (!optional.isPresent()) optional = LazyOptional.of(() -> this);
        return optional.cast();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag output = new CompoundTag();
        for (var field : ModAttachments.types().entrySet()) {
            if (has(field.getValue()) && field.getValue().persistentCodec() != null)
                writeField(output, field.getKey(), field.getValue());
        }
        return output;
    }

    private <T> void writeField(CompoundTag output, String name, AttachmentType<T> type) {
        Tag tag = type.persistentCodec().encodeStart(NbtOps.INSTANCE, get(type)).getOrThrow(false,
                error -> BlacklightNeoMod.LOGGER.error("Cannot save Blacklight field {}: {}", name, error));
        // CompoundTag.CODEC may return the original mutable tag through NbtOps; own the snapshot.
        output.put(name, tag.copy());
    }

    @Override
    public void deserializeNBT(CompoundTag input) {
        values.clear();
        for (var field : ModAttachments.types().entrySet()) {
            if (field.getValue().persistentCodec() != null && input.contains(field.getKey()))
                readField(input.get(field.getKey()), field.getKey(), field.getValue());
        }
    }

    private <T> void readField(Tag input, String name, AttachmentType<T> type) {
        DataResult<T> parsed = type.persistentCodec().parse(NbtOps.INSTANCE, input);
        parsed.resultOrPartial(error -> BlacklightNeoMod.LOGGER.warn("Cannot load Blacklight field {}: {}", name, error))
                .ifPresent(value -> set(type, value));
    }

    public void copyPersistentFrom(BlacklightAttachments source, boolean death) {
        CompoundTag serialized = source.serializeNBT();
        for (var field : ModAttachments.types().entrySet()) {
            AttachmentType<?> type = field.getValue();
            // Decode into a separate object, especially the mutable BlacklightState and disguise NBT.
            if (type.persistentCodec() != null && (!death || type.copiesOnDeath()) && serialized.contains(field.getKey()))
                readField(serialized.get(field.getKey()), field.getKey(), type);
        }
    }

    public void clearTransient() {
        values.keySet().removeIf(type -> type.persistentCodec() == null);
    }
}
