package com.codex.blacklight.combat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WeaponMotionProfileTest {
    @Test void retimingKeepsAnticipationContactAndRecoveryInOrder() {
        for(var move:WeaponMove.values()) {
            var p=WeaponMotionProfile.forMove(move);if(p==null)continue;
            {
                float in=p.contactIn(),out=p.contactOut();
                assertEquals(in,WeaponMotionProfile.sourceTime(move,(move.start-1)/20F,2.5F,in,out),.0001);
                float contactTail=Math.max(move.start,move.end-2)/20F;
                assertEquals(out,WeaponMotionProfile.sourceTime(move,contactTail,2.5F,in,out),.0001);
                assertEquals(2.5F,WeaponMotionProfile.sourceTime(move,(move.duration-1)/20F,2.5F,in,out),.0001);
                float previous=-1;
                for(int i=0;i<move.duration*3;i++) {
                    float t=WeaponMotionProfile.sourceTime(move,i/60F,2.5F,in,out);
                    assertTrue(Float.isFinite(t)&&t>=previous&&t<=2.5F);previous=t;
                }
            }
        }
    }
    @Test void singleTickContactMapsItsEndpointsOverOneFullTick() {
        float in=.05F,out=.1333F;
        assertEquals(in,WeaponMotionProfile.sourceTime(6,7,21,.25F,2.5F,in,out),.0001);
        assertEquals((in+out)/2,WeaponMotionProfile.sourceTime(6,7,21,.275F,2.5F,in,out),.0001);
        assertEquals(out,WeaponMotionProfile.sourceTime(6,7,21,.30F,2.5F,in,out),.0001);
        assertEquals(2.5F,WeaponMotionProfile.sourceTime(6,7,21,1F,2.5F,in,out),.0001);
        assertFiniteMonotonic(6,7,21,in,out);
    }
    @Test void zeroAnticipationBeginsAtContactWithoutNaN() {
        float in=.05F,out=.1333F;
        assertEquals(in,WeaponMotionProfile.sourceTime(1,2,5,0,2.5F,in,out),.0001);
        assertEquals(in,WeaponMotionProfile.sourceTime(1,2,5,-.05F,2.5F,in,out),.0001);
        assertEquals(out,WeaponMotionProfile.sourceTime(1,2,5,.05F,2.5F,in,out),.0001);
        assertEquals(2.5F,WeaponMotionProfile.sourceTime(1,2,5,.20F,2.5F,in,out),.0001);
        assertFiniteMonotonic(1,2,5,in,out);
    }
    private static void assertFiniteMonotonic(int start,int end,int duration,float in,float out) {
        float previous=-1;
        for(int i=0;i<=duration*10;i++) {
            float t=WeaponMotionProfile.sourceTime(start,end,duration,i/200F,2.5F,in,out);
            assertTrue(Float.isFinite(t)&&t>=previous&&t<=2.5F);
            previous=t;
        }
        assertEquals(2.5F,previous,.0001);
    }
    @Test void footworkDoesNotSnapBackOrDoubleRushDistance() {
        for(var move:WeaponMove.values()) {
            var p=WeaponMotionProfile.forMove(move);if(p==null)continue;
            float previous=0;
            for(int i=0;i<=move.duration*3;i++) {
                float distance=p.distanceAt(move,i/60F);
                assertTrue(distance>=previous&&distance<=p.step()+.00001);previous=distance;
            }
            assertEquals(p.step(),previous,.00001);
        }
    }
}
