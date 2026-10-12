package com.codex.blacklight.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Physical button state, never a client-selected move, duration, or target. */
public record WeaponInputPayload(boolean attack,boolean special,boolean action,boolean jump,boolean cancelled) implements CustomPacketPayload {
    public WeaponInputPayload(boolean attack,boolean special){this(attack,special,false,false,false);}
    public WeaponInputPayload(boolean attack,boolean special,boolean cancelled){this(attack,special,false,false,cancelled);}
    public static final Type<WeaponInputPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("blacklight","weapon_input"));
    public static final StreamCodec<RegistryFriendlyByteBuf,WeaponInputPayload> STREAM_CODEC=StreamCodec.composite(ByteBufCodecs.BOOL,WeaponInputPayload::attack,ByteBufCodecs.BOOL,WeaponInputPayload::special,ByteBufCodecs.BOOL,WeaponInputPayload::action,ByteBufCodecs.BOOL,WeaponInputPayload::jump,ByteBufCodecs.BOOL,WeaponInputPayload::cancelled,WeaponInputPayload::new);
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
