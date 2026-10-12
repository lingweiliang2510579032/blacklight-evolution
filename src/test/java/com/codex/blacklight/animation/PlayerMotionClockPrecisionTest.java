package com.codex.blacklight.animation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlayerMotionClockPrecisionTest {
    @Test void loopingGaitStillMovesBetweenFramesAfterLongSession() {
        double session=1_000_000.001;
        float duration=.53333336F;
        float first=PlayerMotionController.loopSampleTime(session,duration);
        float next=PlayerMotionController.loopSampleTime(session+1.0/60,duration);
        // Casting accumulated seconds first loses the complete 60 Hz frame delta.
        assertEquals((float)session,(float)(session+1.0/60));
        assertNotEquals(first,next);
        assertEquals(1.0/60,next-first,.0000001);
        assertTrue(first>=0&&first<duration);
        assertTrue(next>=0&&next<duration);
    }
}
