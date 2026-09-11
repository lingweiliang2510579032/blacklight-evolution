package com.codex.blacklight.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClawDamageRulesTest {
    @Test
    void comboDamageEscalatesAcrossTheThreeHitChain() {
        assertEquals(7.0F, ClawDamageRules.comboBaseDamage(ClawCombatState.Action.AUTO_1));
        assertEquals(8.0F, ClawDamageRules.comboBaseDamage(ClawCombatState.Action.AUTO_2));
        assertEquals(11.0F, ClawDamageRules.comboBaseDamage(ClawCombatState.Action.AUTO_3));
    }

    @Test
    void damageUpgradeOnlyChangesExplicitClawDamage() {
        assertEquals(16.0F, ClawDamageRules.applyDamageUpgrade(16.0F, false));
        assertEquals(20.0F, ClawDamageRules.applyDamageUpgrade(16.0F, true));
        assertThrows(IllegalArgumentException.class,
                () -> ClawDamageRules.applyDamageUpgrade(-1.0F, true));
    }
}
