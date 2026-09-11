package com.codex.blacklight.movement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrototypeLocomotionTest {
    @Test
    void groundedMovementNeverFallsBackToIdleSliding() {
        assertEquals(PrototypeLocomotion.Gait.WALK,
                PrototypeLocomotion.resolve(false, true, true, false, 0.09D));
    }

    @Test
    void sprintIntentAndMomentumBothSelectRunCycle() {
        assertEquals(PrototypeLocomotion.Gait.RUN,
                PrototypeLocomotion.resolve(false, true, true, true, 0.09D));
        assertEquals(PrototypeLocomotion.Gait.RUN,
                PrototypeLocomotion.resolve(false, true, true, false, 0.30D));
    }

    @Test
    void chargedJumpPoseWinsOverLocomotion() {
        assertEquals(PrototypeLocomotion.Gait.CHARGING,
                PrototypeLocomotion.resolve(true, true, true, true, 0.30D));
    }

    @Test
    void animationCadenceTracksActualHorizontalSpeedWithinSafeBounds() {
        assertEquals(PrototypeLocomotion.MIN_PLAYBACK_SPEED,
                PrototypeLocomotion.playbackSpeed(0.0D));
        assertEquals(1.0F,
                PrototypeLocomotion.playbackSpeed(PrototypeLocomotion.RUN_ANIMATION_SPEED));
        assertEquals(PrototypeLocomotion.MAX_PLAYBACK_SPEED,
                PrototypeLocomotion.playbackSpeed(3.0D));
        assertEquals(PrototypeLocomotion.MIN_PLAYBACK_SPEED,
                PrototypeLocomotion.playbackSpeed(Double.NaN));
        assertEquals(PrototypeLocomotion.MIN_PLAYBACK_SPEED,
                PrototypeLocomotion.playbackSpeed(Double.POSITIVE_INFINITY));
    }

    @Test
    void gaitStabilizerRejectsSingleTickRunWalkOscillation() {
        PrototypeLocomotion.Stabilizer stabilizer = new PrototypeLocomotion.Stabilizer();
        assertEquals(PrototypeLocomotion.Gait.WALK,
                stabilizer.update(PrototypeLocomotion.Gait.WALK));

        assertEquals(PrototypeLocomotion.Gait.WALK,
                stabilizer.update(PrototypeLocomotion.Gait.RUN));
        assertEquals(PrototypeLocomotion.Gait.WALK,
                stabilizer.update(PrototypeLocomotion.Gait.WALK));
        assertEquals(PrototypeLocomotion.Gait.WALK,
                stabilizer.update(PrototypeLocomotion.Gait.RUN));

        for (int tick = 1; tick < PrototypeLocomotion.GAIT_CONFIRM_TICKS; tick++) {
            stabilizer.update(PrototypeLocomotion.Gait.RUN);
        }
        assertEquals(PrototypeLocomotion.Gait.RUN, stabilizer.current());
    }

    @Test
    void chargedPoseStillTakesOverImmediately() {
        PrototypeLocomotion.Stabilizer stabilizer = new PrototypeLocomotion.Stabilizer();
        stabilizer.update(PrototypeLocomotion.Gait.RUN);
        assertEquals(PrototypeLocomotion.Gait.CHARGING,
                stabilizer.update(PrototypeLocomotion.Gait.CHARGING));
    }

    @Test
    void cinematicSpeedFeedbackBuildsOnlyAcrossTheSprintRange() {
        double maximum = 1.0D;
        assertEquals(0.0F, PrototypeLocomotion.cinematicSpeedRatio(0.0D, maximum));
        assertEquals(0.0F, PrototypeLocomotion.cinematicSpeedRatio(
                PrototypeLocomotion.RUN_ANIMATION_SPEED, maximum));
        assertEquals(1.0F, PrototypeLocomotion.cinematicSpeedRatio(maximum, maximum));
        assertEquals(1.0F, PrototypeLocomotion.cinematicSpeedRatio(3.0D, maximum));
        assertEquals(0.0F, PrototypeLocomotion.cinematicSpeedRatio(Double.NaN, maximum));
    }

    @Test
    void sprintVisualsHaveAnUnmissableBaseline() {
        assertEquals(0.55F, PrototypeLocomotion.sprintVisualIntensity(0.0D, 1.0D));
        assertEquals(1.0F, PrototypeLocomotion.sprintVisualIntensity(1.0D, 1.0D));
    }
}
