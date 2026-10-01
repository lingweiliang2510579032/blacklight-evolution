package com.codex.blacklight.client.camera;

import com.codex.blacklight.combat.MotionResources;
import com.google.gson.JsonObject;
import java.util.*;
import net.minecraft.world.phys.Vec3;

/** Private FIG events, already mapped from source frames to each shipped body clip. */
public final class SourceCameraEvents {
    public record Event(String kind,double start,double end,double inTime,double outTime,
                        double dolly,double zoom,boolean reAim,Vec3 offset,int size,int range,String hint) {}
    public record Oscillator(double duration,double frequency,double noise,Vec3 translation,Vec3 rotation) {}
    private static Map<String,List<Event>> routes;
    private static List<List<Oscillator>> presets=List.of();
    private static double[] ranges,strengths;
    private static double propagation,decay;
    private SourceCameraEvents() {}
    private static void load() {
        if(routes!=null)return;
        Map<String,List<Event>> loaded=new LinkedHashMap<>();
        MotionResources.read("camera-events.data",16*1024*1024).ifPresent(root->{
            if(root.get("schema").getAsInt()!=1)throw new IllegalArgumentException("Unsupported camera events");
            root.getAsJsonObject("routes").entrySet().forEach(entry->{
                var list=new ArrayList<Event>();double previous=-1;
                for(var value:entry.getValue().getAsJsonArray()) {
                    var e=value.getAsJsonObject();String kind=e.get("kind").getAsString();
                    double start=number(e,"start",0),end=number(e,"end",start);
                    if(start<previous||end<start)throw new IllegalArgumentException("Unordered camera event: "+entry.getKey());
                    previous=start;
                    double in=number(e,"inTime",0),out=number(e,"outTime",0),dolly=number(e,"dolly",1),zoom=number(e,"zoom",1);
                    if(in<0||out<0||dolly<.5||zoom<.5||in+out>end-start+.001)throw new IllegalArgumentException("Invalid punch-in envelope");
                    Vec3 offset=Vec3.ZERO;
                    if(e.has("offset")){var a=e.getAsJsonArray("offset");offset=new Vec3(a.get(0).getAsDouble(),a.get(1).getAsDouble(),a.get(2).getAsDouble());}
                    int size=(int)number(e,"size",0),range=(int)number(e,"range",0);
                    if(size<0||size>2||range<0||range>2||!Double.isFinite(offset.lengthSqr()))throw new IllegalArgumentException("Invalid camera event");
                    list.add(new Event(kind,start,end,in,out,dolly,zoom,e.has("reAim")&&e.get("reAim").getAsBoolean(),offset,size,range,e.has("hint")?e.get("hint").getAsString():""));
                }
                loaded.put(entry.getKey(),List.copyOf(list));
            });
            var shake=root.getAsJsonObject("shake");ranges=array(shake,"range");strengths=array(shake,"strength");
            propagation=number(shake,"propagation",0);decay=number(shake,"decayExponent",0);
            var all=new ArrayList<List<Oscillator>>();
            for(var value:shake.getAsJsonArray("presets")) {
                var list=new ArrayList<Oscillator>();
                for(var row:value.getAsJsonArray()) {
                    var o=row.getAsJsonObject();double duration=number(o,"duration",0),frequency=number(o,"frequency",0),noise=number(o,"noise",0);
                    if(duration<=0||frequency<0||noise<0||noise>1)throw new IllegalArgumentException("Invalid source shake");
                    list.add(new Oscillator(duration,frequency,noise,new Vec3(number(o,"tx",0),number(o,"ty",0),number(o,"tz",0)),
                            new Vec3(number(o,"rx",0),number(o,"ry",0),number(o,"rz",0))));
                }
                all.add(List.copyOf(list));
            }
            if(all.size()!=3||ranges.length!=3||strengths.length!=3)throw new IllegalArgumentException("Incomplete shake presets");
            presets=List.copyOf(all);
        });
        routes=Map.copyOf(loaded);
        if(!routes.isEmpty())com.codex.blacklight.BlacklightNeoMod.LOGGER.info("Loaded {} source camera action routes and {} shake presets",routes.size(),presets.size());
    }
    public static List<Event> events(String key){load();return routes.getOrDefault(key,List.of());}
    public static boolean available(){load();return !routes.isEmpty();}
    public static List<Oscillator> preset(int size){load();return presets.isEmpty()?List.of():presets.get(size);}
    public static double range(int range){load();return ranges==null?0:ranges[range];}
    public static double strength(int size){load();return strengths==null?0:strengths[size];}
    public static double propagation(){load();return propagation;}
    public static double decay(){load();return decay;}
    private static double number(JsonObject o,String key,double fallback) {
        double v=o.has(key)?o.get(key).getAsDouble():fallback;
        if(!Double.isFinite(v)||Math.abs(v)>10000)throw new IllegalArgumentException("Invalid camera scalar: "+key);
        return v;
    }
    private static double[] array(JsonObject o,String key) {
        var a=o.getAsJsonArray(key);double[] out=new double[a.size()];
        for(int i=0;i<a.size();i++){out[i]=a.get(i).getAsDouble();if(!Double.isFinite(out[i])||out[i]<=0)throw new IllegalArgumentException("Invalid shake range/strength");}
        return out;
    }
}
