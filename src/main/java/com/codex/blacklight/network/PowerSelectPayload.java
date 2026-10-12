package com.codex.blacklight.network;

import com.codex.blacklight.BlacklightNeoMod;
import net.minecraft.network.FriendlyByteBuf;
import com.codex.blacklight.forge.codec.*;
import com.codex.blacklight.forge.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Only a requested form id crosses the wire; no unlock or purchase flags. */
public record PowerSelectPayload(int formId,boolean longTransform) implements CustomPacketPayload {
    public PowerSelectPayload(int formId){this(formId,false);}
    public static final Type<PowerSelectPayload> TYPE=new Type<>(new ResourceLocation(BlacklightNeoMod.MOD_ID,"power_select"));
    public static final StreamCodec<FriendlyByteBuf,PowerSelectPayload> STREAM_CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,PowerSelectPayload::formId,ByteBufCodecs.BOOL,PowerSelectPayload::longTransform,PowerSelectPayload::new);
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
