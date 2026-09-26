package com.codex.blacklight.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server-authored impact location. A purely visual decal, never terrain damage. */
public record CombatImpactPayload(double x,double y,double z,float radius,boolean ground,boolean slash,float dx,float dy,float dz) implements CustomPacketPayload {
    public CombatImpactPayload(double x,double y,double z,float radius,boolean ground){this(x,y,z,radius,ground,false,0,1,0);}
    public static final Type<CombatImpactPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("blacklight","combat_impact"));
    public static final StreamCodec<RegistryFriendlyByteBuf,CombatImpactPayload> STREAM_CODEC=StreamCodec.of(
        (b,p)->{b.writeDouble(p.x);b.writeDouble(p.y);b.writeDouble(p.z);b.writeFloat(p.radius);b.writeBoolean(p.ground);b.writeBoolean(p.slash);b.writeFloat(p.dx);b.writeFloat(p.dy);b.writeFloat(p.dz);},
        b->new CombatImpactPayload(b.readDouble(),b.readDouble(),b.readDouble(),b.readFloat(),b.readBoolean(),b.readBoolean(),b.readFloat(),b.readFloat(),b.readFloat()));
    public CombatImpactPayload {
        radius=Float.isFinite(radius)?Math.clamp(radius,.1F,12F):.1F;
        if(!Float.isFinite(dx)||!Float.isFinite(dy)||!Float.isFinite(dz)){dx=0;dy=1;dz=0;}
    }
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
