package com.codex.blacklight.client.camera;

import com.codex.blacklight.combat.MotionResources;
import com.codex.blacklight.registry.ModAttachments;
import com.google.gson.JsonObject;
import java.util.*;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Original CAM channels driven by the replicated body animation clock.
 * The native actor origin is adapted to the Minecraft body's centre; distances
 * use the same retarget scale as the body. No camera transform drives gameplay.
 */
final class SourceCameraFilm {
    record Frame(Vec3 position,Vec3 look,Vec3 up,double fov,double near,double far,
                 double focus,double range,double aperture,double dof,double weight) {}
    private record Channel(double[][] keys,boolean step) {
        double[] at(double time) {
            int lo=0,hi=keys.length-1;
            while(lo<hi){int mid=(lo+hi+1)/2;if(keys[mid][0]<=time)lo=mid;else hi=mid-1;}
            double[] a=keys[lo],b=keys[Math.min(lo+1,keys.length-1)];
            double blend=step||b[0]<=a[0]?0:Mth.clamp((time-a[0])/(b[0]-a[0]),0,1);
            double[] value=new double[a.length-1];for(int i=0;i<value.length;i++)value[i]=Mth.lerp(blend,a[i+1],b[i+1]);
            return value;
        }
    }
    private record Route(Map<String,Channel> channels,double duration,double scale,double start,double end,double speed,
                         double firstHold,double lastHold,double in,double out,boolean track,boolean mirror,Vec3 offset) {
        double scalar(String name,double t,double fallback){var c=channels.get(name);return c==null?fallback:c.at(t)[0];}
        Vec3 vector(String name,double t){var v=channels.get(name).at(t);return new Vec3(v[0],v[1],v[2]);}
    }
    private static Map<String,Route> routes;
    private Frame held;
    private long started=Long.MIN_VALUE;
    private String key="";
    private double release,releaseDuration;
    private boolean running;
    private long cancelledStarted=Long.MIN_VALUE;
    private String cancelledKey="";
    void reset(){held=null;started=Long.MIN_VALUE;key="";release=0;running=false;cancelledStarted=Long.MIN_VALUE;cancelledKey="";}
    void cancelForInput(LocalPlayer player) {
        var morph=com.codex.blacklight.registry.ModAttachments.getData(player, ModAttachments.POWER_MORPH);
        cancelledStarted=morph.started();cancelledKey=morph.key();
        held=null;running=false;
    }
    private static void load() {
        if(routes!=null)return;
        Map<String,Route> loaded=new HashMap<>();
        MotionResources.read("camera-film.data",8*1024*1024).ifPresent(data->{
            if(data.get("schema").getAsInt()!=1)throw new IllegalArgumentException("Unknown camera film schema");
            var clips=data.getAsJsonObject("clips");
            for(var entry:data.getAsJsonObject("routes").entrySet()) {
                var r=entry.getValue().getAsJsonObject();var clip=clips.getAsJsonObject(r.get("clip").getAsString());
                Map<String,Channel> channels=new HashMap<>();
                for(var value:clip.getAsJsonArray("channels")) {
                    var c=value.getAsJsonObject();String name=c.get("semantic").getAsString();var keys=c.getAsJsonArray("keys");
                    if(keys.isEmpty()||keys.size()>18000)throw new IllegalArgumentException("Empty/oversized camera channel");
                    int size=Set.of("TRAN","LOOK","UP").contains(name)?4:2;
                    double[][] array=new double[keys.size()][size];double previous=-1;
                    for(int i=0;i<keys.size();i++) {
                        var row=keys.get(i).getAsJsonArray();if(row.size()!=size)throw new IllegalArgumentException("Camera channel dimensions");
                        for(int j=0;j<size;j++){double v=row.get(j).getAsDouble();if(!Double.isFinite(v)||Math.abs(v)>1000000)throw new IllegalArgumentException("Camera channel scalar");array[i][j]=v;}
                        if(array[i][0]<=previous)throw new IllegalArgumentException("Unordered camera channel");previous=array[i][0];
                    }
                    channels.put(name,new Channel(array,c.has("step")&&c.get("step").getAsBoolean()));
                }
                if(!channels.keySet().containsAll(Set.of("TRAN","LOOK","UP","FOV")))throw new IllegalArgumentException("Incomplete camera film");
                double duration=number(r,"duration"),scale=number(r,"scale"),start=number(r,"start"),end=number(r,"end"),speed=number(r,"speed");
                double in=number(r,"blendIn"),out=number(r,"blendOut"),first=number(r,"firstHold"),last=number(r,"lastHold");
                if(duration<=0||scale<=0||start<0||end<start||speed<=0||Math.min(Math.min(in,out),Math.min(first,last))<0)throw new IllegalArgumentException("Invalid camera schedule");
                var a=r.getAsJsonArray("offset");Vec3 offset=new Vec3(a.get(0).getAsDouble(),a.get(1).getAsDouble(),a.get(2).getAsDouble());
                if(!Double.isFinite(offset.lengthSqr()))throw new IllegalArgumentException("Invalid camera offset");
                loaded.put(entry.getKey(),new Route(Map.copyOf(channels),duration,scale,start,end,speed,first,last,in,out,r.get("track").getAsBoolean(),r.get("mirror").getAsBoolean(),offset));
            }
        });
        routes=Map.copyOf(loaded);
        if(!routes.isEmpty())com.codex.blacklight.BlacklightNeoMod.LOGGER.info("Loaded {} source cinematic/body routes",routes.size());
    }
    private static double number(JsonObject r,String key){double v=r.get(key).getAsDouble();if(!Double.isFinite(v)||Math.abs(v)>10000)throw new IllegalArgumentException("Camera schedule scalar");return v;}
    Frame update(LocalPlayer p,float partial,double dt) {
        load();var morph=com.codex.blacklight.registry.ModAttachments.getData(p, ModAttachments.POWER_MORPH);var route=routes.get(morph.key());
        if(morph.started()==cancelledStarted&&morph.key().equals(cancelledKey))return null;
        boolean active=route!=null&&morph.visible(p,partial)&&morph.seconds(p,partial)<route.duration;
        if(active) {
            if(started!=morph.started()||!key.equals(morph.key())){reset();started=morph.started();key=morph.key();}
            double time=morph.seconds(p,partial);
            double t=Mth.clamp(route.start+Math.max(0,time-route.firstHold)*route.speed,route.start,route.end);
            double finish=Math.min(route.duration,route.firstHold+(route.end-route.start)/route.speed+route.lastHold);
            double weight=Math.min(route.in==0?1:time/route.in,route.out==0?1:(finish-time)/route.out);
            Vec3 local=route.vector("TRAN",t).add(route.offset),look=route.vector("LOOK",t),up=route.vector("UP",t);
            if(route.mirror){local=new Vec3(-local.x,local.y,local.z);look=new Vec3(-look.x,look.y,look.z);up=new Vec3(-up.x,up.y,up.z);}
            Vec3 origin=(route.track?p.getPosition(partial):morph.origin()).add(0,p.getBbHeight()*.5,0);
            double scale=route.scale*p.getScale();
            held=new Frame(origin.add(PrototypeChaseCamera.actorVector(local,morph.facing()).scale(scale)),
                    PrototypeChaseCamera.actorVector(look,morph.facing()).normalize(),PrototypeChaseCamera.actorVector(up,morph.facing()).normalize(),
                    Mth.clamp(Math.toDegrees(route.scalar("FOV",t,Math.toRadians(70))),10,150),
                    Mth.clamp(route.scalar("NCLP",t,.05)*scale,.025,.25),route.scalar("FCLP",t,100000)*scale,
                    route.scalar("DFFD",t,2)*scale,Math.max(.1,route.scalar("DFFS",t,2)*scale),2,
                    route.scalar("DFEN",t,0),Mth.clamp(weight,0,1));
            releaseDuration=Math.max(.05,route.out);release=0;running=true;return held;
        }
        if(held==null)return null;
        if(running){running=false;release=0;}
        release+=dt;double w=held.weight*(1-Mth.clamp(release/releaseDuration,0,1));
        if(w<=0){reset();return null;}
        return new Frame(held.position,held.look,held.up,held.fov,held.near,held.far,held.focus,held.range,held.aperture,held.dof,w);
    }
}
