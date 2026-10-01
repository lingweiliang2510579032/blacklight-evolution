package com.codex.blacklight.client.camera;

import com.codex.blacklight.combat.MotionResources;
import java.util.Map;
import java.util.LinkedHashMap;

/** Optional private source settings; public builds keep the dependency camera. */
public final class ChaseCameraProfile {
    public record Settings(float filterWidth,float changeRate,float elevation,float elevationRate,float elevationSlow,
                           float distance,float distanceRate,float slowingDistance,
                           float yaw,float yawMin,float yawMax,float yawRate,float yawSlow,
                           float pitchUp,float pitchUpMin,float pitchUpMax,float pitchDown,float pitchDownMin,
                           float pitchDownMax,float pitchRate,float pitchSlow) {}
    private static Map<String,Settings> profiles;
    private ChaseCameraProfile() {}

    public static Settings get(String state) {
        if(profiles==null) {
            var loaded=new LinkedHashMap<String,Settings>();
            MotionResources.read("camera-profile.data",64*1024).ifPresent(json -> {
                if(json.get("schema").getAsInt()!=1) throw new IllegalArgumentException("Unsupported camera profile");
                json.getAsJsonObject("profiles").entrySet().forEach(entry -> {
                    var p=entry.getValue().getAsJsonObject();
                    float filter=p.get("filterWidth").getAsFloat(), distance=p.get("cameraDistance").getAsFloat();
                    float rate=p.get("cameraDistanceChangeRate").getAsFloat(), slow=p.get("cameraDistanceSlowingDistance").getAsFloat();
                    if(!Float.isFinite(filter)||!Float.isFinite(distance)||!Float.isFinite(rate)||!Float.isFinite(slow)
                            ||filter<0||filter>1||distance<0.5F||distance>12||rate<=0||slow<0)
                        throw new IllegalArgumentException("Invalid camera state: "+entry.getKey());
                    loaded.put(entry.getKey(),new Settings(filter,value(p,"changeRate"),value(p,"elevation"),
                            value(p,"elevationChangeRate"),value(p,"elevationSlowingDistance"),distance,rate,slow,
                            value(p,"maxLookAtYaw"),value(p,"minLookAtYawCharacterSpeed"),value(p,"maxLookAtYawCharacterSpeed"),
                            value(p,"maxLookAtYawChangeRate"),value(p,"maxLookAtYawSlowingDistance"),value(p,"maxLookAtPitchUp"),
                            value(p,"minLookAtPitchUpCharacterSpeed"),value(p,"maxLookAtPitchUpCharacterSpeed"),
                            value(p,"maxLookAtPitchDown"),value(p,"minLookAtPitchDownCharacterSpeed"),
                            value(p,"maxLookAtPitchDownCharacterSpeed"),value(p,"maxLookAtPitchChangeRate"),value(p,"maxLookAtPitchSlowingDistance")));
                });
                if(loaded.size()!=12||!loaded.containsKey("Ground")) throw new IllegalArgumentException("Incomplete camera profile");
            });
            profiles=Map.copyOf(loaded);
            if(!profiles.isEmpty()) com.codex.blacklight.BlacklightNeoMod.LOGGER.info(
                    "Loaded {} private source chase-camera states",profiles.size());
        }
        return profiles.getOrDefault(state,profiles.get("Ground"));
    }

    private static float value(com.google.gson.JsonObject p,String name) {
        float v=p.get(name).getAsFloat();
        if(!Float.isFinite(v)||Math.abs(v)>1000)throw new IllegalArgumentException("Invalid camera field "+name);
        return v;
    }
}
