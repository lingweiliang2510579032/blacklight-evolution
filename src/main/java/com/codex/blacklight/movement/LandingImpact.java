package com.codex.blacklight.movement;

/** Pure landing-energy calculations shared by server effects and regression tests. */
public final class LandingImpact {
    public enum Strength { NONE, LIGHT, HEAVY, DEVASTATING }

    public static final double LIGHT_THRESHOLD = 0.42D;
    public static final double HEAVY_THRESHOLD = 0.78D;
    public static final double DEVASTATING_THRESHOLD = 1.18D;
    public static final double MINIMUM_IMPACT_DISTANCE = 3.5D;
    public static final double HEAVY_DISTANCE = 8.0D;
    public static final double DEVASTATING_DISTANCE = 16.0D;

    public record Observation(double downwardSpeed, double fallDistance) {
        public static final Observation NONE = new Observation(0.0D, 0.0D);
    }

    public record Profile(Strength strength, double fallDistance, double downwardSpeed,
                          float damage, double radius, double knockback, float intensity) {
        public static final Profile NONE = new Profile(
                Strength.NONE, 0.0D, 0.0D, 0.0F, 0.0D, 0.0D, 0.0F);
    }

    private LandingImpact() {
    }

    public static int recoveryTicks(Strength strength) {
        if (strength == null) return 0;
        return switch (strength) {
            case HEAVY -> 3;
            case DEVASTATING -> 6;
            default -> 0;
        };
    }

    public static Strength classify(double downwardSpeed) {
        if (!Double.isFinite(downwardSpeed) || downwardSpeed < LIGHT_THRESHOLD) {
            return Strength.NONE;
        }
        if (downwardSpeed < HEAVY_THRESHOLD) return Strength.LIGHT;
        if (downwardSpeed < DEVASTATING_THRESHOLD) return Strength.HEAVY;
        return Strength.DEVASTATING;
    }

    public static Strength classifyDistance(double fallDistance) {
        if (!Double.isFinite(fallDistance) || fallDistance < MINIMUM_IMPACT_DISTANCE) {
            return Strength.NONE;
        }
        if (fallDistance < HEAVY_DISTANCE) return Strength.LIGHT;
        if (fallDistance < DEVASTATING_DISTANCE) return Strength.HEAVY;
        return Strength.DEVASTATING;
    }

    /**
     * Height supplies the potential energy while impact speed limits how much of it
     * reaches the ground. A long, controlled glide therefore cannot create a false
     * devastating impact.
     */
    public static Profile profile(double fallDistance, double downwardSpeed) {
        double safeDistance = Double.isFinite(fallDistance)
                ? Math.max(0.0D, fallDistance) : 0.0D;
        double safeSpeed = Double.isFinite(downwardSpeed)
                ? Math.max(0.0D, downwardSpeed) : 0.0D;
        Strength strength = weaker(classifyDistance(safeDistance), classify(safeSpeed));
        if (strength == Strength.NONE) return Profile.NONE;

        double energyDistance = switch (strength) {
            case LIGHT -> Math.min(safeDistance, HEAVY_DISTANCE);
            case HEAVY -> Math.min(safeDistance, DEVASTATING_DISTANCE);
            case DEVASTATING -> safeDistance;
            default -> 0.0D;
        };
        float damage = (float)clamp((energyDistance - 3.0D) * 0.9D, 0.0D, 30.0D);
        double radius = clamp(1.1D + (energyDistance - 3.0D) * 0.22D, 1.1D, 7.0D);
        double knockback = clamp(0.30D + (energyDistance - 3.0D) * 0.035D,
                0.30D, 1.25D);
        float intensity = (float)clamp(0.38D + (energyDistance - 3.0D) * 0.055D,
                0.38D, 1.70D);
        return new Profile(strength, safeDistance, safeSpeed,
                damage, radius, knockback, intensity);
    }

    private static Strength weaker(Strength first, Strength second) {
        return first.ordinal() <= second.ordinal() ? first : second;
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
