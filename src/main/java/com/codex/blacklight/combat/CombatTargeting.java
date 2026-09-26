package com.codex.blacklight.combat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.entity.player.Player;
import com.codex.blacklight.registry.ModAttachments;
import java.util.Map;
import java.util.WeakHashMap;

/** Selection is independent of camera pitch when the player explicitly locks a target. */
public final class CombatTargeting {
    private static final Map<ServerPlayer, LivingEntity> LOCKS = new WeakHashMap<>();
    private CombatTargeting() {}
    /** Search loaded, visible actors. This does not enlarge a weapon's contact volume. */
    public static double viewRange(ServerPlayer player) {
        return Math.clamp(player.serverLevel().getServer().getPlayerList().getViewDistance()*16.0,32,256);
    }
    public static boolean inFrontView(ServerPlayer player,LivingEntity target) {
        return !target.isInvisibleTo(player)&&aimScore(player,target)>.5;
    }
    public static LivingEntity locked(ServerPlayer player) {
        var target=LOCKS.get(player);
        if (!com.codex.blacklight.power.PrototypeMetabolism.active(player)
                || !selectable(player,target) || !inReach(player,target,viewRange(player))) {
            LOCKS.remove(player); return null;
        }
        return target;
    }
    /** Explicit camera lock only. EF's ordinary crosshair focus is never authoritative. */
    public static void requestLock(ServerPlayer player, int entityId) {
        if(entityId<0){LOCKS.remove(player);return;}
        var entity=player.serverLevel().getEntity(entityId);
        if(com.codex.blacklight.power.PrototypeMetabolism.active(player)
                && entity instanceof LivingEntity target && selectable(player,target)
                && inReach(player,target,viewRange(player)) && visible(player,target)) LOCKS.put(player,target);
        else LOCKS.remove(player);
    }
    public static boolean damageable(Player player, LivingEntity target) {
        return target!=null && target!=player && target.level()==player.level()
                && target.isAlive() && !target.isRemoved() && !target.isSpectator() && !target.isInvulnerable()
                && !player.isAlliedTo(target) && !target.getData(ModAttachments.CONSUME_VISUAL).active()
                && (!(target instanceof Player other) || !other.getAbilities().invulnerable && player.canHarmPlayer(other)
                    && (!(player instanceof ServerPlayer server) || server.server.isPvpAllowed()));
    }
    public static boolean selectable(Player player, LivingEntity target) {
        return damageable(player,target) && !target.isInvisibleTo(player);
    }
    public static java.util.Comparator<LivingEntity> contactPriority(ServerPlayer player) {
        var target=locked(player);
        return java.util.Comparator.comparingInt((LivingEntity v)->v==target?0:1)
                .thenComparingDouble(player::distanceToSqr).thenComparingInt(LivingEntity::getId);
    }
    public static double distanceSquared(Vec3 point,AABB box) {
        double x=Math.clamp(point.x,box.minX,box.maxX),y=Math.clamp(point.y,box.minY,box.maxY),z=Math.clamp(point.z,box.minZ,box.maxZ);
        return point.distanceToSqr(new Vec3(x,y,z));
    }
    public static boolean inReach(ServerPlayer player,LivingEntity target,double range) {
        return player.level()==target.level()&&distanceSquared(player.getEyePosition(),target.getBoundingBox())<=range*range+1e-6;
    }
    public static boolean visible(Player player,LivingEntity target) {
        return target!=null && target.level()==player.level() && player.level().clip(new ClipContext(player.getEyePosition(),target.getBoundingBox().getCenter(),
                ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player)).getType()==HitResult.Type.MISS;
    }
    public static double aimScore(ServerPlayer player,LivingEntity target) {
        var delta=target.getBoundingBox().getCenter().subtract(player.getEyePosition());
        return player.getLookAngle().dot(delta.normalize());
    }
}
