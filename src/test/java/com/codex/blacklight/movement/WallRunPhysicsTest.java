package com.codex.blacklight.movement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WallRunPhysicsTest {
    @Test
    void incomingSprintMomentumRaisesClimbSpeedWithinABoundedRange() {
        assertEquals(0.36D, WallRunPhysics.climbVelocity(0.36D, 0.0D), 0.0001D);
        // Sprint momentum converts harder: 2.0 blocks/tick adds 0.40 climb speed.
        assertEquals(0.76D, WallRunPhysics.climbVelocity(0.36D, 2.0D), 0.0001D);
        assertEquals(1.2D, WallRunPhysics.climbVelocity(1.2D, 99.0D), 0.0001D);
    }

    @Test
    void climbVelocityFallsBackAndClampsAtTheDocumentedEdges() {
        assertEquals(0.36D, WallRunPhysics.climbVelocity(Double.NaN, 0.0D), 0.0001D);
        assertEquals(0.36D, WallRunPhysics.climbVelocity(0.36D, Double.NaN), 0.0001D);
        assertEquals(0.36D, WallRunPhysics.climbVelocity(0.36D, -10.0D), 0.0001D);
        // Momentum bonus caps at +0.50 no matter how fast the approach is.
        assertEquals(0.86D, WallRunPhysics.climbVelocity(0.36D, 999.0D), 0.0001D);
        assertTrue(WallRunPhysics.climbVelocity(1.2D, 999.0D) <= 1.2D);
    }

    @Test
    void mantleRequiresTorsoToClearWhileLowerBodyStillTouchesTheWall() {
        assertTrue(WallRunPhysics.shouldMantle(true, false, true));
        assertFalse(WallRunPhysics.shouldMantle(false, false, true));
        assertFalse(WallRunPhysics.shouldMantle(true, true, true));
        assertFalse(WallRunPhysics.shouldMantle(true, false, false));
        assertFalse(WallRunPhysics.shouldMantle(false, false, false));
        assertFalse(WallRunPhysics.shouldMantle(false, true, false));
        assertFalse(WallRunPhysics.shouldMantle(false, true, true));
        assertFalse(WallRunPhysics.shouldMantle(true, true, false));
    }
}
