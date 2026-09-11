package com.codex.blacklight.movement;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Identical collision probes on authority and prediction; no client-only dependencies. */
public final class WallContacts {
    private WallContacts() { }
    public static WallTraversal.Surface sample(Player player, WallTraversal wall) {
        Vec3 look=player.getLookAngle().multiply(1,0,1);
        if (look.lengthSqr()<1e-5) look=new Vec3(-Math.sin(Math.toRadians(player.getYRot())),0,Math.cos(Math.toRadians(player.getYRot())));
        look=look.normalize();
        Vec3 into=wall.attached() ? new Vec3(-wall.normalX(),0,-wall.normalZ()) : look;
        BlockHitResult torso=hit(player,into,.78),lower=hit(player,into,.16);
        // A fast upward leap can cross both original probes in one tick. Sweep
        // below the feet by the previous ascent so the roof edge is not skipped.
        if(torso==null && lower==null && wall.attached() && player.getDeltaMovement().y>0) {
            double ascent=Math.min(1.5,player.getDeltaMovement().y);
            for(double height=-.15;height>=-ascent-.15;height-=.20) {
                lower=hit(player,into,height);
                if(lower!=null)break;
            }
        }
        BlockHitResult contact=torso!=null?torso:lower;
        Vec3 normal=contact==null?Vec3.ZERO:Vec3.atLowerCornerOf(contact.getDirection().getNormal());
        // Initial latching needs actual proximity, not a long ray through empty air.
        if (!wall.attached() && torso!=null && torso.getLocation().subtract(player.position()).horizontalDistance()>player.getBbWidth()*.5+.22) torso=null;
        boolean ceiling=!player.level().noCollision(player,player.getBoundingBox().move(0,.12,0));
        return new WallTraversal.Surface(torso!=null,lower!=null,ceiling,normal.x,normal.z,look.dot(normal));
    }
    private static BlockHitResult hit(Player player, Vec3 direction, double height) {
        Vec3 start=player.position().add(0,height,0);
        BlockHitResult hit=player.level().clip(new ClipContext(start,start.add(direction.scale(MovementTuningSettings.wallDetectDistance())),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));
        return hit.getType()==HitResult.Type.BLOCK && hit.getDirection().getAxis()!=Direction.Axis.Y?hit:null;
    }
}
