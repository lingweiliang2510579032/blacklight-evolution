package com.codex.blacklight.movement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocomotionPhaseTest {
    @Test
    void chargeAndLandingOverrideOrdinaryLocomotion() {
        assertEquals(LocomotionPhase.CHARGING, LocomotionPhase.resolve(
                true, true, 0.0D, true, true, true, false,
                LandingImpact.Strength.DEVASTATING));
        assertEquals(LocomotionPhase.LAND_HEAVY, LocomotionPhase.resolve(
                false, true, 0.0D, true, true, true, false,
                LandingImpact.Strength.HEAVY));
    }

    @Test
    void resolvesStartBrakeTurnRiseAndFallEdges() {
        assertEquals(LocomotionPhase.START, LocomotionPhase.resolve(
                false, true, 0.0D, true, false, false, false,
                LandingImpact.Strength.NONE));
        assertEquals(LocomotionPhase.BRAKE, LocomotionPhase.resolve(
                false, true, 0.0D, false, false, true, false,
                LandingImpact.Strength.NONE));
        assertEquals(LocomotionPhase.TURN, LocomotionPhase.resolve(
                false, true, 0.0D, true, true, true, true,
                LandingImpact.Strength.NONE));
        assertEquals(LocomotionPhase.RISE, LocomotionPhase.resolve(
                false, false, 0.4D, true, true, true, false,
                LandingImpact.Strength.NONE));
        assertEquals(LocomotionPhase.FALL, LocomotionPhase.resolve(
                false, false, -0.2D, true, true, true, false,
                LandingImpact.Strength.NONE));
    }
}
