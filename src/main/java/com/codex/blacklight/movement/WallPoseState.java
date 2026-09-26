package com.codex.blacklight.movement;

import net.minecraft.world.entity.LivingEntity;

/** Wall normals belong to an entity and its level, never to a camera or numeric entity ID alone. */
public final class WallPoseState {
    public record Surface(double normalX,double normalZ,long expires) {}
    // Entity equality is ID-based across the integrated server/client. Weak
    // identity keys prevent an authority reset from clearing a predicted contact.
    private static final java.util.concurrent.ConcurrentMap<LivingEntity,Surface> SURFACES=
            new com.google.common.collect.MapMaker().weakKeys().makeMap();
    private WallPoseState() {}
    public static void set(LivingEntity entity,WallTraversal.Mode mode,double x,double z){
        double length=Math.hypot(x,z);
        if(mode==WallTraversal.Mode.NONE||!Double.isFinite(length)||length<.001)SURFACES.remove(entity);
        else SURFACES.put(entity,new Surface(x/length,z/length,entity.level().getGameTime()+40));
    }
    public static Surface get(LivingEntity entity){
        var state=SURFACES.get(entity);
        return state!=null&&state.expires()>=entity.level().getGameTime()?state:null;
    }
}
