package com.codex.blacklight.combat;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;

/** Short, synchronized response to a confirmed hit; never an attack-time cosmetic. */
public record HitReaction(long started,int duration,int kind,float yaw,int motion) {
    public static final int STAGGER=0,HEAVY=1,LAUNCH=2,SLAM=3,LASH=4,DOWN=5,GETUP=6,DEATH=7;
    public HitReaction(long started,int duration,int kind,float yaw){this(started,duration,kind,yaw,ImpactProfile.BODY);}
    public static final HitReaction EMPTY=new HitReaction(0,0,0,0);
    public static final StreamCodec<RegistryFriendlyByteBuf,HitReaction> CODEC=StreamCodec.composite(
            ByteBufCodecs.VAR_LONG,HitReaction::started,ByteBufCodecs.VAR_INT,HitReaction::duration,
            ByteBufCodecs.VAR_INT,HitReaction::kind,ByteBufCodecs.FLOAT,HitReaction::yaw,
            ByteBufCodecs.VAR_INT,HitReaction::motion,HitReaction::new);
    public float progress(long now,float partial){return duration<=0?1:Math.clamp((now-started+partial)/duration,0,1);}
    public float weight(long now,float partial) {
        float age=now-started+partial;
        if(duration<=0||age<0||age>=duration)return 0;
        if(kind==DOWN||kind==GETUP||kind==DEATH)return 1;
        float attack=Math.clamp((age+1)/2,0,1),release=Math.clamp((duration-age)/Math.clamp(duration*.25F,3,5),0,1);
        return attack*release;
    }
}
