package com.codex.blacklight.movement;

/** Immutable movement settings transferred from the authoritative server to each client. */
public record MovementTuningSnapshot(
        double sprintAcceleration,
        double sprintMaxSpeed,
        double jumpMinVelocity,
        double jumpMaxVelocity,
        double jumpChargeTicks,
        double wallDetectDistance,
        double wallClimbVelocity,
        double glideFallSpeed,
        double glideMaxSpeed,
        double airDashSpeed,
        double airDashTicks,
        double fovBoost
) {
}
