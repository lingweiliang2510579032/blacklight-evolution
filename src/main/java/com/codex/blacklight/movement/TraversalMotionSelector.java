package com.codex.blacklight.movement;

/** Visual routing only: it never moves the actor or changes traversal collision rules. */
public final class TraversalMotionSelector {
    private TraversalMotionSelector() {}
    /** Minecraft yaw zero faces +Z: the actor's right is -X. */
    public static double rightward(double x,double z,double yawDegrees){
        double yaw=Math.toRadians(yawDegrees);
        return -x*Math.cos(yaw)-z*Math.sin(yaw);
    }
    public static String ground(LocomotionPhase phase,boolean sprint,double speed){
        if(phase==LocomotionPhase.BRAKE)return sprint?"basic_sprint_brake":"basic_brake";
        if(phase==LocomotionPhase.TURN)return "basic_turn";
        if(!sprint)return speed>.22?"basic_run":"basic_walk";
        return speed>1.1?"basic_sprint_fastest":speed>.8?"basic_sprint_fast":"basic_sprint";
    }
    public static String wall(boolean running,double up,double right){
        if(!running){
            if(Math.abs(right)>Math.abs(up)*1.25)return right>0?"basic_wall_climb_right":"basic_wall_climb_left";
            return up<-.025?"basic_wall_climb_down":"basic_wall_climb_up";
        }
        if(Math.abs(right)<.06)return "basic_wall_run_up";
        if(up>Math.abs(right)*.55)return right>0?"basic_wall_run_diag_right":"basic_wall_run_diag_left";
        return right>0?"basic_wall_run_right":"basic_wall_run_left";
    }
    public static String glide(double forward,double right){
        if(Math.abs(right)>Math.abs(forward)+.08)return right>0?"basic_glide_right":"basic_glide_left";
        return forward<-.08?"basic_glide_backward":"basic_glide_forward";
    }
    public static float cadence(double speed,double nominal){
        if(!Double.isFinite(speed)||!Double.isFinite(nominal)||nominal<=0)return 1;
        return (float)Math.clamp(speed/nominal,.55,1.8);
    }
}
