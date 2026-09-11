package com.codex.blacklight.combat;

/** Shared, deterministic groundspike timing and field geometry. */
public final class GroundSpikeRules {
    /** The borrowed slam reaches the planted pose at 0.55 seconds. */
    public static final int PLANT_TICKS = 11;
    /** Withdrawal occupies the final 0.55 seconds of the 2.20-second action. */
    public static final int WITHDRAW_TICKS = 11;
    public static final double MAX_LOCKED_TARGET_DISTANCE = 32.0D;
    public static final double BASE_CAST_DISTANCE = 14.0D;
    public static final double BASE_DAMAGE_RADIUS = 6.0D;
    public static final double BASE_VISUAL_RADIUS = 5.2D;
    /** One oversized center cluster plus six widely spaced outer clusters. */
    public static final int VISUAL_SPIKE_COUNT = 7;

    private GroundSpikeRules() {
    }

}
