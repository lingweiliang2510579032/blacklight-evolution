package com.codex.blacklight.animation;

/** One committed gait interval per game tick; render queries only interpolate it. */
public final class LocomotionClock {
    private String key;
    private float duration;
    private boolean looping;
    private long tick=Long.MIN_VALUE;
    private double previous,current;
    public String key(){return key;}
    public long tick(){return tick;}
    public boolean looping(){return looping;}
    public float duration(){return duration;}
    public double current(){return current;}
    public boolean commit(long nextTick,String nextKey,float nextDuration,boolean nextLoop,
                          boolean preserveCycle,double distance,double nominalBlocksPerTick) {
        if(nextTick<=tick)return false;
        if(nextKey==null||nextDuration<=0||!Float.isFinite(nextDuration)
                ||!Double.isFinite(distance)||distance<0||!Double.isFinite(nominalBlocksPerTick)||nominalBlocksPerTick<0)
            throw new IllegalArgumentException("Invalid committed gait interval");
        boolean fresh=key==null,changed=!nextKey.equals(key);
        double ticks=fresh?0:nextTick-tick;
        if(changed){
            current=!fresh&&preserveCycle&&looping&&nextLoop
                    ?(current%duration)/duration*nextDuration:0;
            key=nextKey;duration=nextDuration;looping=nextLoop;
        }
        previous=current;
        double advance=looping&&nominalBlocksPerTick>0?distance/(nominalBlocksPerTick*20):ticks/20;
        // Reject correction-sized phase leaps while leaving normal upgraded speed exact.
        if(!fresh)current+=Math.min(advance,ticks/20*8);
        tick=nextTick;
        return changed;
    }
    public double seconds(float partial) {
        double alpha=Math.clamp(partial,0,1);
        return previous+(current-previous)*alpha;
    }
    public float sampleTime(float partial) {
        double seconds=seconds(partial);
        return looping?(float)(seconds%duration):(float)Math.min(seconds,duration);
    }
}
