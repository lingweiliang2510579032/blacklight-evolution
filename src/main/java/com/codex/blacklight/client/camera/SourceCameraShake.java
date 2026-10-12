package com.codex.blacklight.client.camera;

import java.util.ArrayList;
import net.minecraft.world.phys.Vec3;

/** CameraManager request arbitration and native oscillator/envelope equations.
 * Source: 0x1032d270, 0x1032bf60, 0x108c15d0, 0x108c0ff0.
 */
final class SourceCameraShake {
    private record Impulse(SourceCameraEvents.Oscillator oscillator,double start,double attenuation) {}
    private final ArrayList<Impulse> impulses=new ArrayList<>();
    private double time,priority;
    private int pendingSize=-1;
    private double pendingStrength,pendingAttenuation,pendingDelay;
    private int randomZ=362436069,randomW=521288629;
    private Vec3 rotation=Vec3.ZERO,translation=Vec3.ZERO;
    void reset(){impulses.clear();priority=0;time=0;pendingSize=-1;rotation=translation=Vec3.ZERO;}
    void request(int size,int range,Vec3 origin,Vec3 camera) {
        double distance=origin.distanceTo(camera),radius=SourceCameraEvents.range(range);
        if(radius<=0||distance>=radius)return;
        double attenuation=1-distance/radius,strength=SourceCameraEvents.strength(size)*attenuation;
        if(strength<priority)return;
        if(pendingSize>size||pendingSize==size&&pendingStrength>=strength)return;
        pendingSize=size;pendingStrength=strength;pendingAttenuation=attenuation;
        pendingDelay=distance*SourceCameraEvents.propagation();
    }
    void update(double dt) {
        if(dt<=0)return;
        if(pendingSize>=0) {
            for(var oscillator:SourceCameraEvents.preset(pendingSize)) {
                if(impulses.size()>=32&&!replaceWeakest(oscillator,pendingAttenuation))continue;
                impulses.add(new Impulse(oscillator,time+pendingDelay,pendingAttenuation));
            }
            priority=pendingStrength*1.5;pendingSize=-1;
        }
        time+=dt;priority=Math.max(0,priority-dt);rotation=translation=Vec3.ZERO;
        impulses.removeIf(i->time-i.start>=i.oscillator.duration());
        for(var i:impulses) {
            double age=time-i.start;if(age<0)continue;
            var o=i.oscillator;
            double sine=Math.sin(age*o.frequency()*2*Math.PI);
            double wave=sine+(random()-sine)*o.noise();
            double weight=wave*Math.exp(-SourceCameraEvents.decay()*age/o.duration())*i.attenuation;
            rotation=rotation.add(o.rotation().scale(weight));translation=translation.add(o.translation().scale(weight));
        }
    }
    private boolean replaceWeakest(SourceCameraEvents.Oscillator incoming,double attenuation) {
        // 108c1110/108c14c0: compare remaining translation magnitude and
        // Manhattan rotation amplitude, then minimize 10*translation+rotation.
        double translation=incoming.translation().length()*attenuation;
        double rotation=rotationMagnitude(incoming.rotation())*attenuation;
        int weakest=-1;double score=Double.POSITIVE_INFINITY;
        for(int n=0;n<impulses.size();n++) {
            var i=impulses.get(n);var o=i.oscillator;
            double envelope=Math.exp(-SourceCameraEvents.decay()*Math.max(0,time-i.start)/o.duration())*i.attenuation;
            double t=o.translation().length()*envelope,r=rotationMagnitude(o.rotation())*envelope;
            if(t<=translation&&r<=rotation&&10*t+r<score){score=10*t+r;weakest=n;}
        }
        if(weakest<0)return false;
        impulses.remove(weakest);return true;
    }
    private static double rotationMagnitude(Vec3 value){return Math.abs(value.x)+Math.abs(value.y)+Math.abs(value.z);}
    private double random() {
        // Same two multiply-with-carry recurrences; this mod owns its seed/state.
        randomZ=36969*(randomZ&65535)+(randomZ>>>16);randomW=18000*(randomW&65535)+(randomW>>>16);
        int bits=((randomZ<<16)+randomW)&0x7fffff;
        return (Float.intBitsToFloat(bits|0x3f800000)-1)*2-1;
    }
    Vec3 rotation(){return rotation;}
    Vec3 translation(){return translation;}
}
