package com.codex.blacklight.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CombatAnimationSpecTest {
    @Test
    void attackOnlyHitsInsideItsActiveWindow() {
        CombatAnimationSpec spec = CombatAnimationSpec.CLAW_AUTO_1;

        assertFalse(spec.isActive(spec.startupEnd() - 1));
        assertTrue(spec.isActive(spec.startupEnd()));
        assertTrue(spec.isActive(spec.activeEnd() - 1));
        assertFalse(spec.isActive(spec.activeEnd()));
    }

    @Test
    void cancelWindowBeginsAfterRecoveryStarts() {
        CombatAnimationSpec spec = CombatAnimationSpec.CLAW_AUTO_3;

        assertFalse(spec.canCancel(spec.cancelStart() - 1));
        assertTrue(spec.canCancel(spec.cancelStart()));
    }

    @Test
    void malformedTimingFailsFast() {
        assertThrows(IllegalArgumentException.class,
                () -> new CombatAnimationSpec("broken", 10, 7, 4, 9, true));
    }
}
