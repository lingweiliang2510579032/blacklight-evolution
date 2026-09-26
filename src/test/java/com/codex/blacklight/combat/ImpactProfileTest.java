package com.codex.blacklight.combat;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ImpactProfileTest {
    @Test void airFinisherDrivesVictimDownInsteadOfBouncingItUp(){assertTrue(ImpactProfile.of(WeaponMove.AIR_AUTO4).lift()<0);assertTrue(ImpactProfile.of(WeaponMove.BLADE_DIVE).knockdown());}
    @Test void lightComboPreservesRangeAndFinishersRelease(){assertTrue(ImpactProfile.of(WeaponMove.CLAW_AUTO1).push()<.15);assertFalse(ImpactProfile.of(WeaponMove.CLAW_AUTO1).knockdown());assertTrue(ImpactProfile.of(WeaponMove.CLAW_AUTO3).knockdown());}
    @Test void heavyVariantsAndSweepsHaveDistinctOutcomes(){assertTrue(ImpactProfile.of(WeaponMove.HAMMER_QUAKE_CHARGED).lift()>ImpactProfile.of(WeaponMove.HAMMER_QUAKE).lift());assertEquals(HitReaction.LASH,ImpactProfile.of(WeaponMove.WHIP_SWEEP).kind());}
}
