package com.codex.blacklight.animation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Protocol clock correctness independent of a client rendering loop. */
class NativeActionStateTest {
    private static NativeActionState action(long tick,float offset,float transition,boolean looping,float hold) {
        return new NativeActionState("claw_auto1",tick,offset,transition,1.5F,45,looping,hold,17);
    }
    @Test void serverTickAndPartialTickKeepClipOffsetAndTransition() {
        var state=action(400,.25F,.1F,false,-1);
        assertEquals(.25F,state.seconds(401),.00001F);
        assertEquals(.4F,state.seconds(405),.00001F);
        assertEquals(.425F,state.seconds(405.5),.00001F);
        assertTrue(state.active(426.9));
        assertFalse(state.active(427.1));
        assertEquals(1.5F,state.seconds(500),.00001F);
    }
    @Test void longRunningWorldPreservesHalfTickPrecision() {
        long tick=1L<<40;
        assertEquals(.025F,action(tick,0,0,false,-1).seconds(tick+.5),.000001F);
    }
    @Test void explicitHoldSurvivesDurationUntilServerReleasesIt() {
        var held=action(20,0,0,false,.55F);
        assertTrue(held.active(1_000_000));
        assertEquals(.55F,held.seconds(1_000_000),.000001F);
    }
    @Test void loopingClipUsesAuthoredDuration() {
        var loop=action(20,.25F,0,true,-1);
        assertTrue(loop.active(1_000_000));
        assertEquals(.25F,loop.seconds(80),.000001F);
    }
    @Test void emptyStateCannotClaimAnAction() {
        assertFalse(NativeActionState.EMPTY.active(0));
        assertFalse(NativeActionState.EMPTY.active(1_000_000));
        assertEquals(0,NativeActionState.EMPTY.seconds(1_000_000));
    }
    @Test void invalidNetworkClocksCannotEnterPoseSampling() {
        assertThrows(IllegalArgumentException.class,()->action(0,Float.NaN,0,false,-1));
        assertThrows(IllegalArgumentException.class,()->action(0,0,-.1F,false,-1));
        assertThrows(IllegalArgumentException.class,()->action(0,2,0,false,-1));
        assertThrows(IllegalArgumentException.class,()->action(0,0,0,false,2));
        assertThrows(IllegalArgumentException.class,()->new NativeActionState("../Bad Clip",0,0,0,1,0,false,-1,0));
    }
}
