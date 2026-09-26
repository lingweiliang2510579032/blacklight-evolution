package com.codex.blacklight.combat;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.*;

/** Bounded adaptation, not claimed to be the original game's hidden steering constants. */
public final class CombatPursuit {
    private CombatPursuit(){}
    public static float turnToward(float current,float desired,float rate){
        return current+Mth.clamp(Mth.wrapDegrees(desired-current),-rate,rate);
    }
    /** Input is camera-relative; positive sideways is the player's right (D). */
    public static float inputYaw(float cameraYaw,int forward,int sideways){
        return forward==0&&sideways==0?cameraYaw:cameraYaw+(float)Math.toDegrees(Math.atan2(sideways,forward));
    }
    public static float turn(float current,float initial,float desired,float rate,float limit){
        float goal=initial+Mth.clamp(Mth.wrapDegrees(desired-initial),-limit,limit);
        return current+Mth.clamp(Mth.wrapDegrees(goal-current),-rate,rate);
    }
    public static AABB interpolate(AABB before,AABB after,double alpha){
        if(before.getCenter().distanceToSqr(after.getCenter())>4)return after;
        return new AABB(Mth.lerp(alpha,before.minX,after.minX),Mth.lerp(alpha,before.minY,after.minY),Mth.lerp(alpha,before.minZ,after.minZ),
                Mth.lerp(alpha,before.maxX,after.maxX),Mth.lerp(alpha,before.maxY,after.maxY),Mth.lerp(alpha,before.maxZ,after.maxZ));
    }
}
