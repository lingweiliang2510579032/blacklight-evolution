package com.codex.blacklight.combat;
import com.codex.blacklight.power.PowerForm;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.codex.blacklight.combat.WeaponMove.*;

class UnarmedRoutingTest {
    @Test void ordinaryFormHasPunchKickAndAirBranches(){
        assertEquals(MUSCLE_JAB,choose(PowerForm.NONE,Input.ATTACK,false,false,false));
        assertEquals(COMBO_KNEE,choose(PowerForm.NONE,Input.SPECIAL,false,false,false));
        assertEquals(MUSCLE_CROSS,MUSCLE_JAB.followUp(Input.ATTACK,PowerForm.NONE,false,false));
        assertEquals(COMBO_KICK1,MUSCLE_JAB.followUp(Input.SPECIAL,PowerForm.NONE,false,false));
        assertEquals(AIR_AUTO1,choose(PowerForm.NONE,Input.ATTACK,true,false,false));
        assertEquals(AIR_KICK,choose(PowerForm.NONE,Input.SPECIAL,true,false,false));
        assertTrue(MUSCLE_UPPERCUT.accepts(PowerForm.NONE));assertFalse(HAMMER_JAB.accepts(PowerForm.NONE));
    }
    @Test void ordinaryHandsLeaveToolsUsableAndKeepMuscleBoostMeaningful(){
        assertTrue(controls(PowerForm.NONE,true));assertFalse(controls(PowerForm.NONE,false));
        assertFalse(controls(PowerForm.THERMAL_VISION,true));
        assertTrue(MUSCLE_JAB.damageFor(PowerForm.NONE)<MUSCLE_JAB.damageFor(PowerForm.MUSCLE_MASS));
    }
    @Test void earlyComboInputDoesNotCutShortTheDamageWindow(){
        for(var move:new WeaponMove[]{HAMMER_JAB,HAMMER_HOOK,HAMMER_COMBO_C,MUSCLE_JAB,MUSCLE_CROSS}){
            assertEquals(1,move.comboBufferTick());assertTrue(move.comboLinkTick()>=move.end);
        }
    }
}
