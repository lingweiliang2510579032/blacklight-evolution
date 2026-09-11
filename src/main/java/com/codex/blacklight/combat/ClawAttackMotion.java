package com.codex.blacklight.combat;

/** Pure timing curve shared by claw-combo animation coordinates and tests. */
public final class ClawAttackMotion {
    private ClawAttackMotion() {
    }

    public static double forwardStep(ClawCombatState.Action action, int elapsedTick) {
        Profile profile = profile(action);
        if (profile == null || elapsedTick < profile.startTick
                || elapsedTick >= profile.endTick) return 0.0D;

        int span = profile.endTick - profile.startTick;
        double before = (elapsedTick - profile.startTick) / (double) span;
        double after = (elapsedTick - profile.startTick + 1) / (double) span;
        return profile.distance * (smoothStep(after) - smoothStep(before));
    }

    public static double totalDistance(ClawCombatState.Action action) {
        Profile profile = profile(action);
        return profile == null ? 0.0D : profile.distance;
    }

    public static int movementStartTick(ClawCombatState.Action action) {
        Profile profile = profile(action);
        return profile == null ? 0 : profile.startTick;
    }

    public static int movementEndTick(ClawCombatState.Action action) {
        Profile profile = profile(action);
        return profile == null ? 0 : profile.endTick;
    }

    private static double smoothStep(double value) {
        double t = Math.max(0.0D, Math.min(1.0D, value));
        return t * t * (3.0D - 2.0D * t);
    }

    private static Profile profile(ClawCombatState.Action action) {
        return switch (action) {
            case AUTO_1 -> new Profile(1, 7, 0.90D);
            case AUTO_2 -> new Profile(1, 9, 1.05D);
            case AUTO_3 -> new Profile(2, 12, 1.35D);
            default -> null;
        };
    }

    private record Profile(int startTick, int endTick, double distance) {
    }
}
