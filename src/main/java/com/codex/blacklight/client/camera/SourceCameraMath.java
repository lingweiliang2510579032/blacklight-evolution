package com.codex.blacklight.client.camera;

import java.util.ArrayDeque;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Recovered scalar solver (0x10336570) and filter kernel (0x10335280, mode 2).
 * Engine addresses are evidence locations, never calls into a native game binary.
 */
public final class SourceCameraMath {
    private SourceCameraMath() {}

    public static double approach(double current,double target,double rate,double dt,double slow) {
        double delta=target-current;
        if(delta==0||rate<=0||dt<=0)return current;
        double outside=Math.abs(delta)-slow;
        if(outside>0) {
            double used=Math.min(dt,outside/rate);
            double step=Math.copySign(rate*used,delta);
            current+=step;delta-=step;dt-=used;
        }
        if(slow>0&&dt>0) {
            if(Math.abs(delta)<=slow*.001)current=target;
            else current+=delta*-Math.expm1(-rate*dt/slow);
        }
        return Math.abs(current)<=.00001?0:current;
    }

    public static double angle(double current,double target,double rate,double dt,double slow) {
        return approach(current,current+Mth.wrapDegrees(target-current),rate,dt,slow);
    }

    /** Native window changes linearly at changeRate; its kernel is 1.5*(1-x*x).
     * Simpson integration is exact for a linear position segment times this quadratic.
     * MC uses interpolated render samples rather than the native 64-slot sample ring.
     */
    public static final class FollowFilter {
        private record Sample(double time,Vec3 point) {}
        private final ArrayDeque<Sample> samples=new ArrayDeque<>();
        private double time,width;
        public void reset(Vec3 point,double initialWidth) {
            samples.clear();time=0;width=initialWidth;
            samples.add(new Sample(-1,point));samples.add(new Sample(0,point));
        }
        public Vec3 update(Vec3 point,double targetWidth,double changeRate,double dt) {
            width+=Mth.clamp(targetWidth-width,-changeRate*dt,changeRate*dt);
            time+=dt;
            if(dt>0)samples.add(new Sample(time,point));
            while(samples.size()>2) {
                var it=samples.iterator();it.next();
                if(it.next().time()>time-1&&samples.size()<=1024)break;
                samples.removeFirst();
            }
            if(width<.00001)return point;
            Vec3 area=Vec3.ZERO;double total=0;Sample previous=null;
            for(var sample:samples) {
                if(previous!=null&&sample.time()>previous.time()) {
                    double from=Math.max(time-width,previous.time()),to=sample.time();
                    if(to>from) {
                        double mid=(from+to)*.5,span=sample.time()-previous.time();
                        double a=weight(from),b=weight(mid),c=weight(to),scale=(to-from)/6;
                        Vec3 pa=previous.point().lerp(sample.point(),(from-previous.time())/span);
                        Vec3 pb=previous.point().lerp(sample.point(),(mid-previous.time())/span);
                        area=area.add(pa.scale(a*scale)).add(pb.scale(4*b*scale)).add(sample.point().scale(c*scale));
                        total+=(a+4*b+c)*scale;
                    }
                }
                previous=sample;
            }
            return total>1e-12?area.scale(1/total):point;
        }
        private double weight(double t) {
            double x=(t-time+width*.5)/(width*.5);
            return Math.max(0,1.5*(1-x*x));
        }
    }
}
