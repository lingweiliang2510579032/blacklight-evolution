package com.codex.blacklight.combat;

import com.codex.blacklight.entity.BlacklightHunter;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import java.util.*;

/** Private original receiver tracks. Used for both the server's reaction clock and rendering. */
public final class ReceiverMotions {
    public static final String[] BONES={"root","hips","torso","head","jaw","right_arm","left_arm","right_forearm","left_forearm","right_thigh","left_thigh","right_shin","left_shin"};
    public record Clip(float seconds,List<float[]> frames) {}
    private static Map<String,Clip> clips;
    private ReceiverMotions() {}
    private static synchronized Map<String,Clip> data(){
        if(clips!=null)return clips;
        var resource=MotionResources.read("receiver-poses.data",8*1024*1024);
        if(resource.isEmpty())return clips=Map.of();
        try{
            var root=resource.orElseThrow();
            if(root.get("schema").getAsInt()!=1)throw new IllegalArgumentException("Receiver schema");
            Map<String,Clip> result=new HashMap<>();
            for(var entry:root.getAsJsonObject("clips").entrySet()){
                var object=entry.getValue().getAsJsonObject();float seconds=object.get("seconds").getAsFloat();
                if(!Float.isFinite(seconds)||seconds<=0||seconds>30)throw new IllegalArgumentException("Receiver duration");
                var rows=object.getAsJsonArray("frames");if(rows.size()<2||rows.size()>2000)throw new IllegalArgumentException("Receiver frames");
                var frames=new ArrayList<float[]>();
                for(var row:rows){var a=row.getAsJsonArray();if(a.size()!=3+BONES.length*4)throw new IllegalArgumentException("Receiver frame width");
                    float[] f=new float[a.size()];for(int i=0;i<f.length;i++){f[i]=a.get(i).getAsFloat();if(!Float.isFinite(f[i])||Math.abs(f[i])>10)throw new IllegalArgumentException("Receiver transform");}frames.add(f);}
                result.put(entry.getKey(),new Clip(seconds,List.copyOf(frames)));
            }
            return clips=Map.copyOf(result);
        }catch(Exception e){throw new IllegalStateException("Cannot load original receiver poses",e);}
    }
    public static Clip clip(String key){return data().get(key);}
    public static String hitKey(LivingEntity entity,int kind,int motion){
        boolean hunter=entity instanceof BlacklightHunter;
        String prefix=hunter?"hunter_":"human_";
        if(kind==HitReaction.DEATH)return hunter?"hunter_death":"human_fall";
        if(kind==HitReaction.DOWN)return prefix+"down";
        if(kind==HitReaction.GETUP)return prefix+"rise";
        if(hunter&&motion==ImpactProfile.CLAW_RIP)return "hunter_claws";
        String part=switch(motion){case ImpactProfile.LEFT->"left";case ImpactProfile.RIGHT->"right";case ImpactProfile.BACKHAND,ImpactProfile.CLAW_RIP->"backhand";case ImpactProfile.UP->"up";case ImpactProfile.FALL->"fall";default->"body";};
        if(hunter&&kind==HitReaction.HEAVY){if(part.equals("left")||part.equals("right"))part+="_heavy";else if(part.equals("body"))part="heavy";}
        return prefix+part;
    }
    public static float[] sample(String key,float progress){
        Clip clip=clip(key);if(clip==null)return null;
        float at=com.codex.blacklight.util.MathCompat.clamp(progress,0,1)*(clip.frames.size()-1);int index=(int)at;float weight=at-index;
        float[] a=clip.frames.get(index),b=clip.frames.get(Math.min(index+1,clip.frames.size()-1)),out=new float[a.length];
        for(int i=0;i<3;i++)out[i]=a[i]+(b[i]-a[i])*weight;
        for(int i=3;i<a.length;i+=4){Quaternionf q=new Quaternionf(a[i],a[i+1],a[i+2],a[i+3]).slerp(new Quaternionf(b[i],b[i+1],b[i+2],b[i+3]),weight).normalize();out[i]=q.x;out[i+1]=q.y;out[i+2]=q.z;out[i+3]=q.w;}
        return out;
    }
}
