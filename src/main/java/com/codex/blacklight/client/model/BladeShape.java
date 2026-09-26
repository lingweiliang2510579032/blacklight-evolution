package com.codex.blacklight.client.model;

import net.minecraft.world.phys.Vec3;

/** Shared mesh/trail sizing in the existing elbow-local coordinate system. */
public final class BladeShape {
    public static final float WIDTH=1.16F,EDGE=1.22F,REVERSE=1.10F;
    private BladeShape(){}
    public static float lengthScale(float y){return y>.30F?EDGE:y<0?REVERSE:1;}
    public static float length(float y){return y>.30F?.30F+(y-.30F)*EDGE:y<0?y*REVERSE:y;}
    public static Vec3 point(float x,float y,float z){return new Vec3(x*WIDTH,length(y),z*WIDTH);}
}
