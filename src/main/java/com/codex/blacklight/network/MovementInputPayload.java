package com.codex.blacklight.network;

import com.codex.blacklight.BlacklightNeoMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MovementInputPayload(boolean jumpHeld, boolean dashPressed,
                                   int forward, int sideways, boolean sprinting, boolean detach) implements CustomPacketPayload {
    public MovementInputPayload {
        forward = Integer.signum(forward); sideways = Integer.signum(sideways);
    }
    public static final Type<MovementInputPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BlacklightNeoMod.MOD_ID, "movement_input"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MovementInputPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, MovementInputPayload::jumpHeld,
            ByteBufCodecs.BOOL, MovementInputPayload::dashPressed,
            ByteBufCodecs.VAR_INT, MovementInputPayload::forward,
            ByteBufCodecs.VAR_INT, MovementInputPayload::sideways,
            ByteBufCodecs.BOOL, MovementInputPayload::sprinting,
            ByteBufCodecs.BOOL, MovementInputPayload::detach,
            MovementInputPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
