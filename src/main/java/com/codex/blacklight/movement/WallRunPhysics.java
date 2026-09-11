package com.codex.blacklight.movement;

/** Pure wall-run decisions shared by the authoritative controller and tests. */
public final class WallRunPhysics {
    private static final double MAX_MOMENTUM_BONUS = 0.50D;

    private WallRunPhysics() {
    }

    /** Converts some incoming sprint momentum into vertical climbing speed. */
    public static double climbVelocity(double configuredClimb, double horizontalSpeed) {
        double base = Double.isFinite(configuredClimb) ? Math.max(0.05D, configuredClimb) : 0.36D;
        double speed = Double.isFinite(horizontalSpeed) ? Math.max(0.0D, horizontalSpeed) : 0.0D;
        return Math.min(1.2D, base + Math.min(MAX_MOMENTUM_BONUS, speed * 0.20D));
    }

    /** The torso has cleared the wall while the lower body still touches its top edge. */
    public static boolean shouldMantle(boolean wasWallRunning,
                                       boolean torsoContact, boolean lowerContact) {
        return wasWallRunning && !torsoContact && lowerContact;
    }
}
