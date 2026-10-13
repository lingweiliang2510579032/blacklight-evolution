package com.codex.blacklight.network;

import com.codex.blacklight.BlacklightNeoMod;
import net.minecraft.network.FriendlyByteBuf;
import com.codex.blacklight.forge.codec.StreamCodec;
import com.codex.blacklight.forge.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Changes only the sending player's preferences, not their unlocks or power. */
public record SkillSettingsPayload(boolean bulletdiveBlockBreaking) implements CustomPacketPayload {
    public static final Type<SkillSettingsPayload> TYPE=new Type<>(
            new ResourceLocation(BlacklightNeoMod.MOD_ID,"skill_settings"));
    public static final StreamCodec<FriendlyByteBuf,SkillSettingsPayload> STREAM_CODEC=
            StreamCodec.of((buffer,payload)->buffer.writeBoolean(payload.bulletdiveBlockBreaking),
                    buffer->new SkillSettingsPayload(buffer.readBoolean()));
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
