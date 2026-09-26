package com.codex.blacklight.combat;

/** Motion-only dependency references. Times are seconds in the source clip, never damage events. */
public record WeaponMotionProfile(String source, float contactIn, float contactOut, float step, float elbowWeight,
                                  float wristRoll) {
    public static WeaponMotionProfile forMove(WeaponMove move) {
        if(move.fallback()!=move)return forMove(move.fallback());
        return switch (move) {
            // Grounded single-hand slashes preserve the free arm as a counterweight.
            case BLADE_CUT -> epic("sword_auto1", .10F, .18F, .30F, .88F, -65);
            case BLADE_RETURN -> epic("sword_auto2", .10F, .1833F, .28F, .88F, -65);
            // Single advancing strike when no explicit local source motion is installed.
            case BLADE_RUSH -> epic("sword_dash", .15F, .30F, 2.4F, .88F, -65);
            case HAMMER_JAB -> wom("torment_auto_1", .20F, .35F, .40F, .72F);
            case HAMMER_HOOK -> wom("torment_auto_2", .15F, .30F, .45F, .72F);
            case MUSCLE_JAB -> epic("fist_auto1", .05F, .1333F, .24F, 1, 0);
            case MUSCLE_CROSS -> epic("fist_auto2", .05F, .1333F, .28F, 1, 0);
            case MUSCLE_UPPERCUT -> wom("torment_auto_3", .40F, .4833F, .34F, .90F);
            case MUSCLE_RUSH -> epic("fist_dash", .10F, .25F, 1.10F, 1, 0);
            case MUSCLE_AIR -> epic("fist_airslash", .0667F, .2167F, 0, 1, 0);
            default -> null;
        };
    }
    private static WeaponMotionProfile epic(String clip,float in,float out,float step,float elbow,float roll) {
        String key="epicfight:biped/combat/"+clip;
        return new WeaponMotionProfile(key,in,out,step,elbow,roll);
    }
    private static WeaponMotionProfile wom(String clip,float in,float out,float step,float elbow) {
        return new WeaponMotionProfile("wom:biped/combat/"+clip,in,out,step,elbow,0);
    }
    /** Account for the one-tick blend-in, then align source contact and recovery to server time. */
    public static float sourceTime(WeaponMove move,float target,float sourceLength,float in,float out) {
        float begin=(move.start-1)/20F, end=(move.end-2)/20F, finish=(move.duration-1)/20F;
        target=Math.clamp(target,0,finish);
        if(target<=begin)return in*target/begin;
        if(target<=end)return in+(out-in)*(target-begin)/(end-begin);
        return out+(sourceLength-out)*(target-end)/(finish-end);
    }
    public float distanceAt(WeaponMove move,float time) {
        float begin=Math.max(0,(move.start-4)/20F),end=(move.end-1)/20F;
        float t=Math.clamp((time-begin)/(end-begin),0,1);
        return step*t*t*(3-2*t);
    }
}
