package com.codex.blacklight.forge;

import net.minecraft.resources.ResourceLocation;

/** Domain packet identity; Forge SimpleChannel owns registration and delivery. */
public interface CustomPacketPayload {
    Type<? extends CustomPacketPayload> type();

    record Type<T extends CustomPacketPayload>(ResourceLocation id) {}
}
