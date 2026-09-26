package com.codex.blacklight.movement;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TraversalMotionSelectorTest {
    @Test void actorRightIsCorrectOnBothWallOrientations(){
        assertTrue(TraversalMotionSelector.rightward(-.1,0,0)>0);
        assertTrue(TraversalMotionSelector.rightward(0,.1,-90)>0);
        assertTrue(TraversalMotionSelector.rightward(.1,0,0)<0);
    }
    @Test void stoppingRetainsThePreviousGaitEvenAtZeroSpeed(){
        assertEquals("basic_sprint_brake",TraversalMotionSelector.ground(LocomotionPhase.BRAKE,true,0));
        assertEquals("basic_brake",TraversalMotionSelector.ground(LocomotionPhase.BRAKE,false,0));
        assertEquals("basic_turn",TraversalMotionSelector.ground(LocomotionPhase.TURN,true,.8));
    }
    @Test void wallRunningAndSlowClimbingUseDistinctDirections(){
        assertEquals("basic_wall_run_up",TraversalMotionSelector.wall(true,.7,0));
        assertEquals("basic_wall_run_diag_left",TraversalMotionSelector.wall(true,.6,-.3));
        assertEquals("basic_wall_run_right",TraversalMotionSelector.wall(true,0,.6));
        assertEquals("basic_wall_climb_down",TraversalMotionSelector.wall(false,-.1,0));
        assertEquals("basic_wall_climb_left",TraversalMotionSelector.wall(false,0,-.1));
        assertEquals("basic_wall_climb_up",TraversalMotionSelector.wall(false,.1,0));
    }
    @Test void glideUsesTravelRelativeToTheBody(){
        assertEquals("basic_glide_forward",TraversalMotionSelector.glide(.8,.1));
        assertEquals("basic_glide_backward",TraversalMotionSelector.glide(-.8,.1));
        assertEquals("basic_glide_left",TraversalMotionSelector.glide(.1,-.8));
        assertEquals("basic_glide_right",TraversalMotionSelector.glide(.1,.8));
    }
    @Test void gaitSpeedIsBoundedAndInvalidValuesCannotPoisonThePose(){
        assertEquals(.55F,TraversalMotionSelector.cadence(0,.7));
        assertEquals(1.8F,TraversalMotionSelector.cadence(9,.7));
        assertEquals(1F,TraversalMotionSelector.cadence(Double.NaN,.7));
        assertEquals(1F,TraversalMotionSelector.cadence(.7,0));
        assertEquals("basic_sprint_fastest",TraversalMotionSelector.ground(LocomotionPhase.RUN,true,1.2));
        assertEquals("basic_run",TraversalMotionSelector.ground(LocomotionPhase.WALK,false,.3));
    }
}
