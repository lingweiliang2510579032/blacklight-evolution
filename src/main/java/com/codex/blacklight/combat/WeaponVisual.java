package com.codex.blacklight.combat;

import net.minecraft.network.FriendlyByteBuf;
import com.codex.blacklight.forge.codec.*;

public record WeaponVisual(int move,long started,float yaw,boolean charging,float clipOffset,int targetId,float travelPose,float attackPose) {
    public WeaponVisual(int move,long started,float yaw,boolean charging,float clipOffset,int targetId,float travelPose){this(move,started,yaw,charging,clipOffset,targetId,travelPose,-1);}
    public WeaponVisual(int move,long started,float yaw,boolean charging,float clipOffset,int targetId){this(move,started,yaw,charging,clipOffset,targetId,-1);}
    public WeaponVisual(int move,long started,float yaw,boolean charging,float clipOffset){this(move,started,yaw,charging,clipOffset,-1);}
    public WeaponVisual(int move,long started,float yaw){this(move,started,yaw,false,0);}
    public static final WeaponVisual EMPTY=new WeaponVisual(-1,0,0);
    public boolean traveling(){return travelPose>=0;}
    public static final StreamCodec<FriendlyByteBuf,WeaponVisual> CODEC=new StreamCodec<>(){
        public WeaponVisual decode(FriendlyByteBuf b){return new WeaponVisual(b.readVarInt(),b.readVarLong(),b.readFloat(),b.readBoolean(),b.readFloat(),b.readVarInt(),b.readFloat(),b.readFloat());}
        public void encode(FriendlyByteBuf b,WeaponVisual v){b.writeVarInt(v.move);b.writeVarLong(v.started);b.writeFloat(v.yaw);b.writeBoolean(v.charging);b.writeFloat(v.clipOffset);b.writeVarInt(v.targetId);b.writeFloat(v.travelPose);b.writeFloat(v.attackPose);}
    };
    public WeaponMove spec(){return WeaponMove.byId(move);}
}
