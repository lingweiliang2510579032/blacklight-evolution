package com.codex.blacklight.combat;

import com.codex.blacklight.power.PowerForm;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WeaponSpecialRoutingTest {
    @Test void groundedBladeSpecialAdvancesFromStandingOrSprintingRegardlessOfComboHistory() {
        for (boolean sprint : new boolean[]{false, true})
            for (boolean second : new boolean[]{false, true})
                assertEquals(WeaponMove.BLADE_RUSH,
                        WeaponMove.choose(PowerForm.BLADE, WeaponMove.Input.SPECIAL, false, sprint, second));
        assertEquals(WeaponMove.BLADE_CUT,
                WeaponMove.choose(PowerForm.BLADE, WeaponMove.Input.ATTACK, false, false, false));
        assertEquals(WeaponMove.BLADE_RETURN,
                WeaponMove.choose(PowerForm.BLADE, WeaponMove.Input.ATTACK, false, false, true));
    }

    @Test void bladeDashIsOneContactWithForwardMovementAndACompleteRecovery() {
        var move = WeaponMove.BLADE_RUSH;
        assertFalse(move.frenzy(), "dash must not replay the old three-cut attack");
        assertEquals(4,move.start);
        assertEquals(22,move.end);
        assertEquals(51,move.duration);
        assertEquals(java.util.List.of(new LocalWeaponMotion.ContactWindow(4,5),
                        new LocalWeaponMotion.ContactWindow(7,10),new LocalWeaponMotion.ContactWindow(12,22)),
                LocalWeaponMotion.find(move.id).orElseThrow().contactWindows());
        for (int tick = 0; tick <= move.duration; tick++)
            assertEquals(tick==4||tick>=7&&tick<10||tick>=12&&tick<22?0:-1,
                    move.contactIndex(tick),"bundled blade rush tick "+tick);
        for(int gap:new int[]{5,6,10,11})assertEquals(-1,move.contactIndex(gap));
        var profile = WeaponMotionProfile.forMove(move);
        assertNotNull(profile, "ordinary launch also needs an advancing dash");
        assertTrue(profile.distanceAt(move, (move.duration - 1) / 20F) > 2);
        assertEquals(move.duration, move.comboLinkTick());
        assertTrue(move.end < move.duration);
    }

    @Test void specialDoesNotReplaceAirLandingOrOtherWeaponRoutes() {
        assertEquals(WeaponMove.BLADE_DIVE,
                WeaponMove.choose(PowerForm.BLADE, WeaponMove.Input.SPECIAL, true, true, false));
        assertEquals(WeaponMove.HAMMER_RAM,
                WeaponMove.choose(PowerForm.HAMMERFISTS, WeaponMove.Input.SPECIAL, false, true, false));
        assertEquals(WeaponMove.HAMMER_QUAKE,
                WeaponMove.choose(PowerForm.HAMMERFISTS, WeaponMove.Input.SPECIAL, false, false, false));
        assertEquals(WeaponMove.WHIP_LONGSHOT,
                WeaponMove.choose(PowerForm.WHIPFIST, WeaponMove.Input.SPECIAL, false, false, false));
        assertEquals(WeaponMove.WHIP_SWEEP,WeaponMove.WHIP_LASH.nextSpecial());
        assertEquals(WeaponMove.HAMMER_COMBO_SPECIAL,WeaponMove.HAMMER_HOOK.nextSpecial());
    }
}
