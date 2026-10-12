package com.codex.blacklight.combat;

import net.minecraft.world.phys.Vec3;

/** Contact contract for grab_hold: left palm, including Epic Fight's player scale. */
public final class GrabPlacement {
    public static final double LEFT = .55, FORWARD = .56, HEIGHT = 1.165;
    public static final double BLADE_RIGHT=.736, BLADE_FORWARD=.726, BLADE_HEIGHT=1.337;
    private GrabPlacement() {}
    /** Center of the body intersects the continuous right blade, not the empty left palm. */
    public static Vec3 bladeContact(Vec3 player,float yaw,float pitch,float consumeProgress) {
        double radians=Math.toRadians(com.codex.blacklight.util.MathCompat.clamp(pitch,-65,65));
        var forward=Vec3.directionFromRotation(0,yaw);
        double pull=com.codex.blacklight.util.MathCompat.clamp(consumeProgress,0,1);pull=pull*pull*(3-2*pull);
        double right=BLADE_RIGHT-.10*pull;
        return player.add(forward.scale((BLADE_FORWARD-.16*pull)*Math.cos(radians)))
                .add(-forward.z*right,BLADE_HEIGHT-Math.sin(radians)*.38,-forward.x*-right);
    }
    public static Vec3 bladeAnchor(Vec3 player,float yaw,float pitch,double height,float consumeProgress) {
        var contact=bladeContact(player,yaw,pitch,consumeProgress);
        return new Vec3(contact.x,Math.max(player.y,contact.y-height*.6),contact.z);
    }

    public static Vec3 anchor(Vec3 player, float yaw, double width, double height) {
        return anchor(player,yaw,0,width,height);
    }
    public static Vec3 palm(Vec3 player,float yaw,float pitch) {
        double radians=Math.toRadians(com.codex.blacklight.util.MathCompat.clamp(pitch,-90,90));
        var forward=Vec3.directionFromRotation(0,yaw);
        return player.add(forward.scale(FORWARD*Math.max(.55,Math.cos(radians))))
                .add(forward.z*LEFT,HEIGHT-Math.sin(radians)*.42,-forward.x*LEFT);
    }
    public static Vec3 anchor(Vec3 player,float yaw,float pitch,double width,double height) {
        var forward = Vec3.directionFromRotation(0, yaw);
        // Put the near surface of the body in the palm, rather than its feet at a
        // fixed distance. Small mobs must rise to the same hand as larger ones.
        var hand=palm(player,yaw,pitch);
        return new Vec3(hand.x,Math.max(player.y,hand.y-height*.6),hand.z).add(forward.scale(width*.4));
    }
}
