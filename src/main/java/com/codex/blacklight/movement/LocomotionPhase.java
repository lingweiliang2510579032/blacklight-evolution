package com.codex.blacklight.movement;

/** Visual locomotion phases. Gameplay movement remains server-authoritative. */
public enum LocomotionPhase {
    IDLE,
    START,
    WALK,
    RUN,
    BRAKE,
    TURN,
    CHARGING,
    JUMP_LAUNCH,
    RISE,
    FALL,
    WALL_RUN,
    WALL_CLIMB,
    WALL_LEAP,
    LAND_LIGHT,
    LAND_HEAVY,
    LAND_DEVASTATING;

    public static LocomotionPhase resolve(boolean charging, boolean onGround,
                                          double verticalSpeed, boolean moving,
                                          boolean running, boolean wasMoving,
                                          boolean hardTurn,
                                          LandingImpact.Strength landing) {
        if (charging) return CHARGING;
        if (landing != null && landing != LandingImpact.Strength.NONE) {
            return switch (landing) {
                case LIGHT -> LAND_LIGHT;
                case HEAVY -> LAND_HEAVY;
                case DEVASTATING -> LAND_DEVASTATING;
                default -> IDLE;
            };
        }
        if (!onGround) return verticalSpeed > 0.035D ? RISE : FALL;
        if (hardTurn && moving) return TURN;
        if (moving && !wasMoving) return START;
        if (!moving && wasMoving) return BRAKE;
        if (moving) return running ? RUN : WALK;
        return IDLE;
    }

    public boolean isGroundMovement() {
        return this == START || this == WALK || this == RUN || this == BRAKE || this == TURN;
    }

    public boolean isLanding() {
        return this == LAND_LIGHT || this == LAND_HEAVY || this == LAND_DEVASTATING;
    }
}
