package com.codex.blacklight.network;

import com.codex.blacklight.BlacklightNeoMod;
import net.minecraft.network.FriendlyByteBuf;
import com.codex.blacklight.forge.codec.ByteBufCodecs;
import com.codex.blacklight.forge.codec.StreamCodec;
import com.codex.blacklight.forge.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MovementInputPayload(boolean jumpHeld, boolean dashPressed,
                                   int forward, int sideways, boolean sprinting, boolean detach,float cameraYaw) implements CustomPacketPayload {
    public MovementInputPayload(boolean jump,boolean dash,int forward,int sideways,boolean sprint,boolean detach){this(jump,dash,forward,sideways,sprint,detach,Float.NaN);}
    public MovementInputPayload {
        forward = Integer.signum(forward); sideways = Integer.signum(sideways);
    }
    public static final Type<MovementInputPayload> TYPE = new Type<>(
            new ResourceLocation(BlacklightNeoMod.MOD_ID, "movement_input"));
    public static final StreamCodec<FriendlyByteBuf, MovementInputPayload> STREAM_CODEC = new StreamCodec<>() {
        public MovementInputPayload decode(FriendlyByteBuf b){return new MovementInputPayload(b.readBoolean(),b.readBoolean(),b.readVarInt(),b.readVarInt(),b.readBoolean(),b.readBoolean(),b.readFloat());}
        public void encode(FriendlyByteBuf b,MovementInputPayload p){b.writeBoolean(p.jumpHeld);b.writeBoolean(p.dashPressed);b.writeVarInt(p.forward);b.writeVarInt(p.sideways);b.writeBoolean(p.sprinting);b.writeBoolean(p.detach);b.writeFloat(p.cameraYaw);}
    };

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
