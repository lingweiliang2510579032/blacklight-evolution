package com.codex.blacklight.util;

/** Java 17 counterpart to the bounded numeric operation used by upstream. */
public final class MathCompat {
    private MathCompat() {}
    public static int clamp(long value,int min,int max) { if(min>max)throw new IllegalArgumentException("min > max");return (int)Math.min(max,Math.max(min,value)); }
    public static long clamp(long value,long min,long max) { if(min>max)throw new IllegalArgumentException("min > max");return Math.min(max,Math.max(min,value)); }
    public static float clamp(float value,float min,float max) { if(!(min<=max)||Float.compare(min,max)>0)throw new IllegalArgumentException("Invalid bounds");return Math.min(max,Math.max(min,value)); }
    public static double clamp(double value,double min,double max) { if(!(min<=max)||Double.compare(min,max)>0)throw new IllegalArgumentException("Invalid bounds");return Math.min(max,Math.max(min,value)); }
}
