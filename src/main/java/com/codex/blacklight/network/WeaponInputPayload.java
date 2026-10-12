package com.codex.blacklight.network;

import net.minecraft.network.FriendlyByteBuf;
import com.codex.blacklight.forge.codec.*;
import com.codex.blacklight.forge.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Physical button state, never a client-selected move, duration, or target. */
public record WeaponInputPayload(boolean attack,boolean special,boolean action,boolean jump,boolean cancelled) implements CustomPacketPayload {
    public WeaponInputPayload(boolean attack,boolean special){this(attack,special,false,false,false);}
    public WeaponInputPayload(boolean attack,boolean special,boolean cancelled){this(attack,special,false,false,cancelled);}
    public static final Type<WeaponInputPayload> TYPE=new Type<>(new ResourceLocation("blacklight","weapon_input"));
    public static final StreamCodec<FriendlyByteBuf,WeaponInputPayload> STREAM_CODEC=StreamCodec.composite(ByteBufCodecs.BOOL,WeaponInputPayload::attack,ByteBufCodecs.BOOL,WeaponInputPayload::special,ByteBufCodecs.BOOL,WeaponInputPayload::action,ByteBufCodecs.BOOL,WeaponInputPayload::jump,ByteBufCodecs.BOOL,WeaponInputPayload::cancelled,WeaponInputPayload::new);
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
