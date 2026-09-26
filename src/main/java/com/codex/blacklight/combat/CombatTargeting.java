package com.codex.blacklight.combat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.*;
import net.minecraft.world.level.ClipContext;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

/** Selection is independent of camera pitch when the player explicitly locks a target. */
public final class CombatTargeting {
    private CombatTargeting() {}
    /** Search loaded, visible actors. This does not enlarge a weapon's contact volume. */
    public static double viewRange(ServerPlayer player) {
        return Math.clamp(player.serverLevel().getServer().getPlayerList().getViewDistance()*16.0,32,256);
    }
    public static boolean inFrontView(ServerPlayer player,LivingEntity target) {
        return !target.isInvisibleTo(player)&&aimScore(player,target)>.5;
    }
    public static LivingEntity locked(ServerPlayer player) {
        var patch=EpicFightCapabilities.getServerPlayerPatch(player);
        return patch==null?null:patch.getTarget();
    }
    public static double distanceSquared(Vec3 point,AABB box) {
        double x=Math.clamp(point.x,box.minX,box.maxX),y=Math.clamp(point.y,box.minY,box.maxY),z=Math.clamp(point.z,box.minZ,box.maxZ);
        return point.distanceToSqr(new Vec3(x,y,z));
    }
    public static boolean inReach(ServerPlayer player,LivingEntity target,double range) {
        return player.level()==target.level()&&distanceSquared(player.getEyePosition(),target.getBoundingBox())<=range*range+1e-6;
    }
    public static boolean visible(ServerPlayer player,LivingEntity target) {
        return player.level().clip(new ClipContext(player.getEyePosition(),target.getBoundingBox().getCenter(),
                ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player)).getType()==HitResult.Type.MISS;
    }
    public static double aimScore(ServerPlayer player,LivingEntity target) {
        var delta=target.getBoundingBox().getCenter().subtract(player.getEyePosition());
        return player.getLookAngle().dot(delta.normalize());
    }
}
