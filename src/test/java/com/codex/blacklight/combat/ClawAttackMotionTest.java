package com.codex.blacklight.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClawAttackMotionTest {
    @Test
    void eachComboLungeAddsUpToItsAuthoredDistance() {
        for (ClawCombatState.Action action : new ClawCombatState.Action[] {
                ClawCombatState.Action.AUTO_1,
                ClawCombatState.Action.AUTO_2,
                ClawCombatState.Action.AUTO_3}) {
            double travelled = 0.0D;
            for (int tick = 0; tick < 30; tick++) {
                double step = ClawAttackMotion.forwardStep(action, tick);
                assertTrue(step >= 0.0D);
                travelled += step;
            }
            assertEquals(ClawAttackMotion.totalDistance(action), travelled, 1.0E-9D);
            assertTrue(ClawAttackMotion.movementEndTick(action)
                    > ClawAttackMotion.movementStartTick(action));
        }
    }

    @Test
    void nonComboActionsNeverReceiveNormalAttackLunge() {
        assertEquals(0.0D, ClawAttackMotion.forwardStep(
                ClawCombatState.Action.GROUND_SPIKE, 5));
        assertEquals(0.0D, ClawAttackMotion.forwardStep(
                ClawCombatState.Action.SPRINT_ATTACK, 5));
    }
}
