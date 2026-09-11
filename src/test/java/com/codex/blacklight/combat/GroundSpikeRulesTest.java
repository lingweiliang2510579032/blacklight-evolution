package com.codex.blacklight.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GroundSpikeRulesTest {
    @Test
    void eruptionFieldIsBroadButIntentionallySparse() {
        assertEquals(11, GroundSpikeRules.PLANT_TICKS);
        assertEquals(11, GroundSpikeRules.WITHDRAW_TICKS);
        assertEquals(32.0D, GroundSpikeRules.MAX_LOCKED_TARGET_DISTANCE, 0.0D);
        assertEquals(14.0D, GroundSpikeRules.BASE_CAST_DISTANCE, 0.0D);
        assertEquals(6.0D, GroundSpikeRules.BASE_DAMAGE_RADIUS, 0.0D);
        assertEquals(5.2D, GroundSpikeRules.BASE_VISUAL_RADIUS, 0.0D);
        assertEquals(7, GroundSpikeRules.VISUAL_SPIKE_COUNT);
    }
}
