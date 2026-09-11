package com.codex.blacklight.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClawCombatStateTest {
    @Test
    void attacksCycleThroughThreeHitCombo() {
        ClawCombatState state = new ClawCombatState();

        assertEquals(ClawCombatState.Action.AUTO_1, state.beginNextAttack());
        state.finish(16);
        assertEquals(ClawCombatState.Action.AUTO_2, state.beginNextAttack());
        state.finish(16);
        assertEquals(ClawCombatState.Action.AUTO_3, state.beginNextAttack());
        state.finish(16);
        assertEquals(ClawCombatState.Action.AUTO_1, state.beginNextAttack());
    }

    @Test
    void comboExpiresAfterGraceWindow() {
        ClawCombatState state = new ClawCombatState();
        state.beginNextAttack();
        state.finish(2);

        state.tickIdle();
        assertEquals(0, state.comboIndex());
        state.tickIdle();
        assertEquals(-1, state.comboIndex());
    }

    @Test
    void queuedAttackIsClearedWhenNextSwingBegins() {
        ClawCombatState state = new ClawCombatState();
        state.beginNextAttack();
        state.queueAttack();
        assertTrue(state.queuedAttack());

        state.beginNextAttack();
        assertFalse(state.queuedAttack());
        assertEquals(ClawCombatState.Action.AUTO_2, state.action());
    }

    @Test
    void dashDirectionIsNormalizedAndClearedOnFinish() {
        ClawCombatState state = new ClawCombatState();
        state.beginDash(3.0D, 4.0D);

        assertEquals(0.6D, state.dashX(), 1.0E-9D);
        assertEquals(0.8D, state.dashZ(), 1.0E-9D);
        assertEquals(ClawCombatState.Action.DASHING_SLICE, state.action());

        state.finish(0);
        assertEquals(0.0D, state.dashX());
        assertEquals(0.0D, state.dashZ());
    }

    @Test
    void sprintAttackHasItsOwnDirectionalState() {
        ClawCombatState state = new ClawCombatState();
        state.beginSprintAttack(0.0D, -2.0D);

        assertEquals(ClawCombatState.Action.SPRINT_ATTACK, state.action());
        assertEquals(0.0D, state.dashX(), 1.0E-9D);
        assertEquals(-1.0D, state.dashZ(), 1.0E-9D);
    }

    @Test
    void comboAttackCapturesAndNormalizesItsFacingDirection() {
        ClawCombatState state = new ClawCombatState();
        state.beginNextAttack();
        state.setAttackDirection(-3.0D, 4.0D);

        assertEquals(-0.6D, state.dashX(), 1.0E-9D);
        assertEquals(0.8D, state.dashZ(), 1.0E-9D);
    }

    @Test
    void groundSpikeBecomesReleasableAtTheAutomaticPlantMarker() {
        ClawCombatState state = new ClawCombatState();
        state.beginGroundSpikeCharge();

        for (int tick = 0; tick < GroundSpikeRules.PLANT_TICKS - 1; tick++) state.advance();
        assertFalse(state.canReleaseGroundSpike(GroundSpikeRules.PLANT_TICKS));

        state.advance();
        assertTrue(state.canReleaseGroundSpike(GroundSpikeRules.PLANT_TICKS));
        state.beginGroundSpikeRecovery();
        assertEquals(ClawCombatState.Action.GROUND_SPIKE, state.action());
        assertTrue(state.impactTriggered());
    }

    @Test
    void groundSpikeTimerCanClampAtTheAutomaticPlantMarkerWithoutDrift() {
        ClawCombatState state = new ClawCombatState();
        state.beginGroundSpikeCharge();
        for (int tick = 0; tick < 80; tick++) {
            state.advance();
            state.holdGroundSpikeChargeAt(GroundSpikeRules.PLANT_TICKS);
        }

        assertEquals(ClawCombatState.Action.GROUND_SPIKE_CHARGING, state.action());
        assertEquals(GroundSpikeRules.PLANT_TICKS, state.elapsedTicks());
        assertTrue(state.canReleaseGroundSpike(GroundSpikeRules.PLANT_TICKS));
    }
}
