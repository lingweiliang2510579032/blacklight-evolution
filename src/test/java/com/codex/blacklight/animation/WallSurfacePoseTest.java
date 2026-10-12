package com.codex.blacklight.animation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WallSurfacePoseTest {
    @Test void stoppedGravityDoesNotBecomeADownwardRunningCommand() {
        assertEquals(0,WallSurfacePose.normalizeRunVertical(0,-.0784,true,false));
        assertEquals(0,WallSurfacePose.normalizeRunVertical(.01,-.0784,false,true));
    }
    @Test void actualWallTravelKeepsItsDirection() {
        assertEquals(-.4,WallSurfacePose.normalizeRunVertical(0,-.4,false,false));
        assertEquals(-.4,WallSurfacePose.normalizeRunVertical(0,-.4,true,false));
        assertEquals(-.0784,WallSurfacePose.normalizeRunVertical(.3,-.0784,true,false));
        assertEquals(.55,WallSurfacePose.normalizeRunVertical(0,.55,false,false));
    }
}
