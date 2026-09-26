package com.codex.blacklight.combat;

import com.codex.blacklight.power.PowerForm;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.codex.blacklight.combat.WeaponMove.*;

class AerialSpecialRoutingTest {
    @Test void eachWeaponRetainsItsSpecialWhenEnteringFromAnAirCombo(){
        var forms=new PowerForm[]{PowerForm.NONE,PowerForm.CLAWS,PowerForm.BLADE,PowerForm.HAMMERFISTS,PowerForm.WHIPFIST};
        var moves=new WeaponMove[]{AIR_KICK,CLAW_POUNCE,BLADE_DIVE,HAMMER_DROP,WHIP_AIR};
        for(int i=0;i<forms.length;i++){
            assertEquals(moves[i],choose(forms[i],Input.SPECIAL,true,false,false));
            assertEquals(moves[i],AIR_AUTO2.followUp(Input.SPECIAL,forms[i],true,false));
        }
        assertFalse(CLAW_POUNCE.landingAttack());
        assertTrue(BLADE_DIVE.landingAttack());assertTrue(HAMMER_DROP.landingAttack());
    }
    @Test void authoredPounceAndHeldPoseLoadWithoutPrivateFiles(){
        var motion=LocalWeaponMotion.find("claw_pounce").orElseThrow();
        var charge=motion.charge().orElseThrow();
        assertTrue(LocalVisualMotion.find(charge.animation()).isPresent());
        assertEquals(4,motion.contactBounds().size());
        assertTrue(motion.contactBounds().values().stream().allMatch(b->b.maxForward()<2&&b.minForward()>0));
        assertTrue(charge.releaseTime()*20<motion.contactStartTick());
    }
}
