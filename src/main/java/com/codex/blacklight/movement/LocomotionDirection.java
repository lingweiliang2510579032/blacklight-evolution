package com.codex.blacklight.movement;

/** Eight-way local-space movement direction with boundary hysteresis. */
public enum LocomotionDirection {
    FRONT(0.0D),
    FRONT_RIGHT(45.0D),
    RIGHT(90.0D),
    BACK_RIGHT(135.0D),
    BACK(180.0D),
    BACK_LEFT(-135.0D),
    LEFT(-90.0D),
    FRONT_LEFT(-45.0D);

    public static final double SECTOR_DEGREES = 45.0D;
    public static final double HYSTERESIS_DEGREES = 10.0D;
    public static final int CONFIRM_TICKS = 2;

    private final double degrees;

    LocomotionDirection(double degrees) {
        this.degrees = degrees;
    }

    public double degrees() {
        return degrees;
    }

    public double radians() {
        return Math.toRadians(degrees);
    }

    /** Classifies a vector where forward is +Y and right is +X. */
    public static LocomotionDirection classify(double localForward, double localRight) {
        if (!Double.isFinite(localForward) || !Double.isFinite(localRight)
                || localForward * localForward + localRight * localRight < 1.0E-8D) {
            return FRONT;
        }
        double angle = Math.toDegrees(Math.atan2(localRight, localForward));
        LocomotionDirection nearest = FRONT;
        double nearestDistance = Double.MAX_VALUE;
        for (LocomotionDirection direction : values()) {
            double distance = angularDistance(angle, direction.degrees);
            if (distance < nearestDistance) {
                nearest = direction;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    public static double angularDistance(double first, double second) {
        double delta = Math.abs(wrapDegrees(first - second));
        return Math.min(delta, 360.0D - delta);
    }

    private static double wrapDegrees(double degrees) {
        double wrapped = degrees % 360.0D;
        if (wrapped >= 180.0D) wrapped -= 360.0D;
        if (wrapped < -180.0D) wrapped += 360.0D;
        return wrapped;
    }

    /** Keeps sector boundaries stable and requires two consecutive candidate ticks. */
    public static final class Stabilizer {
        private LocomotionDirection current = FRONT;
        private LocomotionDirection pending = FRONT;
        private int pendingTicks;

        public LocomotionDirection update(double localForward, double localRight) {
            if (!Double.isFinite(localForward) || !Double.isFinite(localRight)
                    || localForward * localForward + localRight * localRight < 1.0E-8D) {
                pending = current;
                pendingTicks = 0;
                return current;
            }
            double angle = Math.toDegrees(Math.atan2(localRight, localForward));
            if (angularDistance(angle, current.degrees)
                    <= SECTOR_DEGREES * 0.5D + HYSTERESIS_DEGREES) {
                pending = current;
                pendingTicks = 0;
                return current;
            }
            LocomotionDirection candidate = classify(localForward, localRight);
            if (candidate == current) return current;
            if (candidate != pending) {
                pending = candidate;
                pendingTicks = 1;
            } else if (++pendingTicks >= CONFIRM_TICKS) {
                current = candidate;
                pendingTicks = 0;
            }
            return current;
        }

        public LocomotionDirection current() {
            return current;
        }
    }
}
