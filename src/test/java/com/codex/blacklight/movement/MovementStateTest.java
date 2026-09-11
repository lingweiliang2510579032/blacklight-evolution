package com.codex.blacklight.movement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MovementStateTest {
    @Test
    void losingGroundCanCancelChargeWithoutCreatingAJump() {
        MovementState state = new MovementState();
        state.acceptInput(true, false, true, true);
        state.beginCharge();
        state.tickCharge(10);

        state.cancelCharge();

        assertEquals(0, state.chargeTicks());
        assertEquals(MovementState.Mode.NORMAL, state.mode());
        assertTrue(state.jumpHeld());
    }

    @Test
    void dashRequestIsConsumedOnlyOnce() {
        MovementState state = new MovementState();
        state.acceptInput(false, true, false, false);

        assertTrue(state.consumeDashRequest());
        assertFalse(state.consumeDashRequest());
    }

    @Test
    void transientGroundFlickerDoesNotEraseAChargedJump() {
        MovementState state = new MovementState();
        state.acceptInput(true, false, true, false);
        state.beginCharge();

        for (int tick = 0; tick < PrototypeLocomotion.CHARGE_GROUND_GRACE_TICKS; tick++) {
            state.observeChargeOffGround();
        }

        assertEquals(1, state.chargeTicks());
        state.observeChargeOnGround();
        assertEquals(0, state.chargeOffGroundTicks());
    }

    @Test
    void fullJumpChargeStaysCappedUntilExplicitRelease() {
        MovementState state = new MovementState();
        state.acceptInput(true, false, false, false);
        state.beginCharge();

        assertFalse(state.tickCharge(3));
        assertTrue(state.tickCharge(3));
        assertFalse(state.tickCharge(3));
        assertEquals(3, state.chargeTicks());
        assertTrue(state.isFullyCharged(3));

        state.acceptInput(false, false, false, false);
        assertEquals(3, state.releaseCharge());
        assertEquals(0, state.chargeTicks());
    }

    @Test
    void landingEmitsOnlyOnePeakFallSpeedEdge() {
        MovementState state = new MovementState();

        assertEquals(0.0D, state.observeGroundContact(true, 0.0D));
        assertEquals(0.0D, state.observeGroundContact(false, -0.35D));
        assertEquals(0.0D, state.observeGroundContact(false, -0.92D));
        assertEquals(0.0D, state.observeGroundContact(false, -0.61D));
        assertEquals(0.92D, state.observeGroundContact(true, 0.0D));
        assertEquals(0.0D, state.observeGroundContact(true, 0.0D));
    }

    @Test
    void landingMeasuresTheDropFromAirborneApexToGround() {
        MovementState state = new MovementState();

        state.observeLanding(false, 0.52D, 65.0D);
        state.observeLanding(false, 0.08D, 70.0D);
        state.observeLanding(false, -0.92D, 66.0D);
        LandingImpact.Observation landing = state.observeLanding(true, 0.0D, 64.0D);

        assertEquals(0.92D, landing.downwardSpeed(), 0.0001D);
        assertEquals(6.0D, landing.fallDistance(), 0.0001D);
        assertEquals(LandingImpact.Observation.NONE,
                state.observeLanding(true, 0.0D, 64.0D));
    }

    @Test
    void attackCanCancelDevastatingLandingRecovery() {
        MovementState state = new MovementState();
        state.beginLandingRecovery(LandingImpact.Strength.DEVASTATING);
        assertEquals(6, state.landingRecoveryTicks());
        state.tickLandingRecovery();
        assertEquals(5, state.landingRecoveryTicks());
        state.cancelLandingRecovery();
        assertEquals(0, state.landingRecoveryTicks());
        assertEquals(LandingImpact.Strength.NONE, state.landingStrength());
    }

    @Test
    void sprintCollisionEmitsOnlyOnContactEdge() {
        MovementState state = new MovementState();
        assertTrue(state.updateSprintCollision(true));
        assertFalse(state.updateSprintCollision(true));
        assertFalse(state.updateSprintCollision(false));
        assertTrue(state.updateSprintCollision(true));
    }

    @Test
    void wallRunStoresAStableNormalizedSurfaceNormalUntilExit() {
        MovementState state = new MovementState();

        assertTrue(state.beginWallRun(0.0D, -4.0D));
        assertTrue(state.wallRunning());
        assertEquals(0.0D, state.wallNormalX(), 0.0001D);
        assertEquals(-1.0D, state.wallNormalZ(), 0.0001D);
        assertFalse(state.beginWallRun(0.0D, -1.0D));
        assertTrue(state.endWallRun());
        assertFalse(state.wallRunning());
        assertEquals(0.0D, state.wallNormalZ(), 0.0001D);
    }
}
