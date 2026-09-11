package com.codex.blacklight.movement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocomotionDirectionTest {
    @Test
    void classifiesAllEightLocalMovementSectors() {
        assertEquals(LocomotionDirection.FRONT, LocomotionDirection.classify(1.0D, 0.0D));
        assertEquals(LocomotionDirection.FRONT_RIGHT, LocomotionDirection.classify(1.0D, 1.0D));
        assertEquals(LocomotionDirection.RIGHT, LocomotionDirection.classify(0.0D, 1.0D));
        assertEquals(LocomotionDirection.BACK_RIGHT, LocomotionDirection.classify(-1.0D, 1.0D));
        assertEquals(LocomotionDirection.BACK, LocomotionDirection.classify(-1.0D, 0.0D));
        assertEquals(LocomotionDirection.BACK_LEFT, LocomotionDirection.classify(-1.0D, -1.0D));
        assertEquals(LocomotionDirection.LEFT, LocomotionDirection.classify(0.0D, -1.0D));
        assertEquals(LocomotionDirection.FRONT_LEFT, LocomotionDirection.classify(1.0D, -1.0D));
    }

    @Test
    void stabilizerUsesTenDegreeHysteresisAndTwoTickConfirmation() {
        LocomotionDirection.Stabilizer stabilizer = new LocomotionDirection.Stabilizer();
        assertEquals(LocomotionDirection.FRONT, stabilizer.update(1.0D, 0.0D));

        // 30 degrees is outside the raw 22.5 degree sector but inside its +10 degree hold.
        assertEquals(LocomotionDirection.FRONT,
                stabilizer.update(Math.cos(Math.toRadians(30.0D)),
                        Math.sin(Math.toRadians(30.0D))));

        double forward = Math.cos(Math.toRadians(40.0D));
        double right = Math.sin(Math.toRadians(40.0D));
        assertEquals(LocomotionDirection.FRONT, stabilizer.update(forward, right));
        assertEquals(LocomotionDirection.FRONT_RIGHT, stabilizer.update(forward, right));
    }
}
