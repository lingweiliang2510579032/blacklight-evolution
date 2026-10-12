package com.codex.blacklight.animation;

import com.codex.blacklight.combat.LocalWeaponMotion;
import com.codex.blacklight.combat.WeaponMove;
import com.codex.blacklight.combat.WeaponVisual;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Real bundled clips exercise the shared body/camera phase clock. */
class WeaponMotionClockTest {
    @Test void groundedPursuitUsesLocomotionUntilStrike() {
        var move=WeaponMove.BLADE_CUT;
        var pursuing=new WeaponVisual(move.ordinal(),100,0,false,0,12,.4F,-1);
        var sample=WeaponMotionClock.sample(pursuing,140.5);
        assertEquals("basic_sprint",sample.key());
        assertTrue(sample.looping());
        assertEquals(2.025F,sample.seconds(),.00001F);
        var strike=new WeaponVisual(move.ordinal(),140,0,false,0,12,-1,-1);
        assertEquals(move.id,WeaponMotionClock.sample(strike,142).key());
    }
    @Test void airPursuitAndContactHoldDoNotFollowExpiredActionLease() {
        var move=WeaponMove.BLADE_DIVE;
        var travel=new WeaponVisual(move.ordinal(),100,0,false,0,12,.3F,-1);
        var held=new WeaponVisual(move.ordinal(),100,0,false,0,12,-1,.45F);
        assertEquals(.3F,WeaponMotionClock.sample(travel,900).seconds(),.00001F);
        assertEquals(.45F,WeaponMotionClock.sample(held,900).seconds(),.00001F);
    }
    @Test void attackResumeRestoresFractionalOffsetExactlyOnce() {
        var move=WeaponMove.BLADE_CUT;
        var visual=new WeaponVisual(move.ordinal(),90,0,false,.517F,12,-1,-1);
        float transition=LocalWeaponMotion.find(move.id).orElseThrow().transitionSeconds();
        assertEquals(.617F-transition,WeaponMotionClock.sample(visual,102).seconds(),.00001F);
    }
    @Test void nativeStrideCalibrationMatchesSourceCoordinateDistance() {
        assertEquals(.333F,MotionLibrary.nominalSpeed("basic_sprint"),.001F);
        assertEquals(.05664F,MotionLibrary.nominalSpeed("basic_walk"),.001F);
        double speed=1.2,nominal=MotionLibrary.nominalSpeed("basic_sprint_fastest");
        float cadence=com.codex.blacklight.movement.TraversalMotionSelector.cadence(speed,nominal);
        assertEquals(speed,nominal*cadence,.00001);
    }
    @Test void legacyCombatClockKeepsAuthoredServerDuration() {
        assertEquals("legacy_claw_groundspike",MotionLibrary.resolve(PlayerAction.CLAW_GROUNDSPIKE));
        assertEquals(2.2F,MotionLibrary.duration(MotionLibrary.resolve(PlayerAction.CLAW_GROUNDSPIKE)),.00001F);
        assertEquals(.7F,MotionLibrary.duration(MotionLibrary.resolve(PlayerAction.CLAW_AUTO_1)),.00001F);
        assertNotEquals(MotionLibrary.duration("claw_groundspike"),MotionLibrary.duration("legacy_claw_groundspike"));
    }
}
