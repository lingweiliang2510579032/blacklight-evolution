package com.codex.blacklight.movement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LandingImpactTest {
    @Test
    void impactStrengthScalesWithRecordedFallSpeed() {
        assertEquals(LandingImpact.Strength.NONE, LandingImpact.classify(0.2D));
        assertEquals(LandingImpact.Strength.LIGHT, LandingImpact.classify(0.5D));
        assertEquals(LandingImpact.Strength.HEAVY, LandingImpact.classify(0.9D));
        assertEquals(LandingImpact.Strength.DEVASTATING, LandingImpact.classify(1.4D));
        assertEquals(LandingImpact.Strength.NONE, LandingImpact.classify(Double.NaN));
    }

    @Test
    void onlyHeavyLandingsApplyShortCancelableRecovery() {
        assertEquals(0, LandingImpact.recoveryTicks(LandingImpact.Strength.LIGHT));
        assertEquals(3, LandingImpact.recoveryTicks(LandingImpact.Strength.HEAVY));
        assertEquals(6, LandingImpact.recoveryTicks(LandingImpact.Strength.DEVASTATING));
    }

    @Test
    void distanceAndImpactSpeedJointlyLimitReleasedEnergy() {
        assertEquals(LandingImpact.Strength.NONE,
                LandingImpact.profile(3.0D, 1.4D).strength());
        assertEquals(LandingImpact.Strength.LIGHT,
                LandingImpact.profile(6.0D, 0.6D).strength());
        assertEquals(LandingImpact.Strength.HEAVY,
                LandingImpact.profile(12.0D, 0.9D).strength());
        assertEquals(LandingImpact.Strength.DEVASTATING,
                LandingImpact.profile(24.0D, 1.4D).strength());
        assertEquals(LandingImpact.Strength.NONE,
                LandingImpact.profile(24.0D, 0.2D).strength());
    }

    @Test
    void damageRadiusAndKnockbackGrowContinuouslyButStayBounded() {
        LandingImpact.Profile impact = LandingImpact.profile(24.0D, 1.4D);
        assertEquals(18.9F, impact.damage(), 0.001F);
        assertEquals(5.72D, impact.radius(), 0.001D);
        assertEquals(1.035D, impact.knockback(), 0.001D);

        LandingImpact.Profile extreme = LandingImpact.profile(100.0D, 3.0D);
        assertEquals(30.0F, extreme.damage(), 0.001F);
        assertEquals(7.0D, extreme.radius(), 0.001D);
        assertEquals(1.25D, extreme.knockback(), 0.001D);
        assertEquals(1.70F, extreme.intensity(), 0.001F);
    }
}
