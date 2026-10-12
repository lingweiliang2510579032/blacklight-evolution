package com.codex.blacklight.network;

import net.minecraft.network.FriendlyByteBuf;
import com.codex.blacklight.forge.codec.*;
import com.codex.blacklight.forge.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record WeaponAnimationPayload(int move,boolean charging,float clipOffset) implements CustomPacketPayload {
    public WeaponAnimationPayload(int move){this(move,false,0);}
    public static final Type<WeaponAnimationPayload> TYPE=new Type<>(new ResourceLocation("blacklight","weapon_animation"));
    public static final StreamCodec<FriendlyByteBuf,WeaponAnimationPayload> STREAM_CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,WeaponAnimationPayload::move,ByteBufCodecs.BOOL,WeaponAnimationPayload::charging,ByteBufCodecs.FLOAT,WeaponAnimationPayload::clipOffset,WeaponAnimationPayload::new);
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
