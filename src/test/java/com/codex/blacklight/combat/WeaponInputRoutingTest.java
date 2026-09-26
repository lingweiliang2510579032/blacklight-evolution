package com.codex.blacklight.combat;

import com.codex.blacklight.power.PowerForm;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.codex.blacklight.combat.WeaponMove.*;

class WeaponInputRoutingTest {
    @Test void aChordIsOneIntentAndEndsWhenEitherButtonReleases(){
        var pair=new WeaponButtons(true,true,false,false);
        assertEquals(Input.GROUND_POUND,pair.intent());assertTrue(pair.held(Input.GROUND_POUND));
        assertFalse(new WeaponButtons(true,false,false,false).held(Input.GROUND_POUND));
        assertEquals(Input.PAIN,new WeaponButtons(true,false,true,false).intent());
        assertEquals(Input.CLAP,new WeaponButtons(false,true,true,false).intent());
        assertNull(new WeaponButtons(false,false,true,false).intent());
        assertFalse(pair.pressedSince(pair));
    }
    @Test void everyWeaponSeparatesAerialComboFromItsSpecial(){
        for(var form:PowerForm.values())if(WeaponMove.supports(form)){
            assertEquals(AIR_AUTO1,choose(form,Input.ATTACK,true,false,false));
            assertNotEquals(AIR_AUTO1,choose(form,Input.SPECIAL,true,false,false));
        }
        assertEquals(BLADE_DIVE,choose(PowerForm.BLADE,Input.SPECIAL,true,false,false));
        assertEquals(HAMMER_DROP,choose(PowerForm.HAMMERFISTS,Input.SPECIAL,true,false,false));
        assertEquals(WHIP_AIR,choose(PowerForm.WHIPFIST,Input.SPECIAL,true,false,false));
    }
    @Test void jumpingAndLandingInvalidateThePreviousMediumsChain(){
        assertEquals(AIR_AUTO1,BLADE_CUT.followUp(Input.ATTACK,PowerForm.BLADE,true,false));
        assertEquals(BLADE_DIVE,BLADE_CUT.followUp(Input.SPECIAL,PowerForm.BLADE,true,false));
        assertEquals(HAMMER_JAB,AIR_AUTO1.followUp(Input.ATTACK,PowerForm.HAMMERFISTS,false,false));
        assertEquals(WHIP_LASH,AIR_AUTO3.followUp(Input.ATTACK,PowerForm.WHIPFIST,false,false));
    }
    @Test void airComboEndsAfterFourPressesAndClawsAfterThree(){
        assertEquals(AIR_AUTO2,AIR_AUTO1.nextAttack());assertEquals(AIR_AUTO3,AIR_AUTO2.nextAttack());
        assertEquals(AIR_AUTO4,AIR_AUTO3.nextAttack());assertNull(AIR_AUTO4.nextAttack());
        assertEquals(CLAW_AUTO2,CLAW_AUTO1_CHARGED.nextAttack());assertEquals(CLAW_AUTO3,CLAW_AUTO2.nextAttack());
        assertNull(CLAW_AUTO3.nextAttack());assertEquals(COMBO_SPIN,CLAW_AUTO2.nextSpecial());
    }
    @Test void twoButtonActionsRemainDifferentFromSequentialCombos(){
        assertEquals(WHIP_SWEEP,WHIP_LASH.followUp(Input.SPECIAL,PowerForm.WHIPFIST,false,false));
        assertEquals(COMBO_GROUND_POUND,WHIP_LASH.followUp(Input.GROUND_POUND,PowerForm.WHIPFIST,false,false));
        assertEquals(AIR_GROUND_POUND,choose(PowerForm.CLAWS,Input.GROUND_POUND,true,false,false));
        assertEquals(COMBO_AXE_HANDLE,COMBO_KNEE.nextAttack());assertEquals(COMBO_SPIN,COMBO_KNEE.nextSpecial());
    }
    @Test void alternatingCombosKeepTheirBranchAndSupermanIsNotAFourthClick(){
        assertEquals(COMBO_KICK1,MUSCLE_CROSS.nextSpecial());
        assertEquals(COMBO_KICK2,COMBO_KICK1.nextSpecial());
        assertEquals(MUSCLE_COMBO_C,COMBO_KICK1.nextAttack());
        assertNull(MUSCLE_COMBO_C.nextAttack());
        assertEquals(COMBO_BACKHAND,COMBO_AXE_HANDLE.nextAttack());
        assertEquals(COMBO_AXE,COMBO_AXE_HANDLE.nextSpecial());
        assertEquals(COMBO_MACH,COMBO_BACKHAND.nextAttack());
        assertEquals(COMBO_SPIN_LEFT,COMBO_BACKHAND.nextSpecial());
        assertEquals(CLAW_AUTO3,COMBO_SPIN.followUp(Input.ATTACK,PowerForm.CLAWS,false,false));
        assertEquals(AIR_LAUNCH_FOLLOW,MUSCLE_UPPERCUT.followUp(Input.ATTACK,PowerForm.MUSCLE_MASS,true,false));
        assertEquals(AIR_AUTO1,AIR_LAUNCH_FOLLOW.nextAttack());
    }
}
