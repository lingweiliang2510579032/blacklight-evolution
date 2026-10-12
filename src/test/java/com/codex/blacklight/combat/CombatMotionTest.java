package com.codex.blacklight.combat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CombatMotionTest {
    @Test void fallbackChargeStillChangesFlightSpeedWithoutPrivateData(){
        for(var move:new WeaponMove[]{WeaponMove.AIR_KICK,WeaponMove.AIR_PAIN})
            assertTrue(CombatMotion.speed(move,1)>CombatMotion.speed(move,0));
    }
    @Test void longApproachHasEnoughTimeWithoutChangingContactReach(){
        for(var move:WeaponMove.values()){
            double speed=CombatMotion.speed(move,0);
            assertTrue(CombatMotion.travelBudget(160,speed)>160/speed);
            assertTrue(CombatMotion.travelBudget(10000,speed)<=640);
            assertTrue(CombatMotion.travelPose(move)<move.start/20F);
        }
    }
}
