package com.codex.blacklight.animation;

import com.codex.blacklight.client.TargetLockRequestState;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TargetLockRequestStateTest {
    @Test void attachmentDelayCannotExpireAPendingLockAfterEightTicks() {
        var state=new TargetLockRequestState();int request=state.request(17);
        for(int tick=0;tick<60;tick++){assertFalse(state.observeAttachment(-1));assertEquals(17,state.desiredId());}
        assertTrue(state.pending());assertTrue(state.acknowledge(request,17));
        state.observeAttachment(-1);assertEquals(17,state.desiredId());
        state.observeAttachment(17);assertTrue(state.observeAttachment(-1));assertEquals(-1,state.desiredId());
    }
    @Test void clearAndNewSelectionIgnoreOlderAcquireReplies() {
        var state=new TargetLockRequestState();int acquire=state.request(17),clear=state.request(-1);
        assertFalse(state.acknowledge(acquire,17));assertEquals(-1,state.desiredId());
        assertTrue(state.acknowledge(clear,-1));
        int newer=state.request(22);assertFalse(state.acknowledge(clear,-1));assertEquals(22,state.desiredId());
        assertTrue(state.acknowledge(newer,22));
    }
    @Test void serverRejectionAndRespawnInvalidatePredictedOwnership() {
        var state=new TargetLockRequestState();int rejected=state.request(17);
        assertTrue(state.acknowledge(rejected,-1));assertEquals(-1,state.desiredId());
        int oldLife=state.request(22);state.resetOwner();int newLife=state.request(31);
        assertFalse(state.acknowledge(oldLife,22));assertTrue(state.acknowledge(newLife,31));
    }
}
