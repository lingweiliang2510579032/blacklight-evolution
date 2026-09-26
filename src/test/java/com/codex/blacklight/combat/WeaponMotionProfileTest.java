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
                assertEquals(out,WeaponMotionProfile.sourceTime(move,(move.end-2)/20F,2.5F,in,out),.0001);
                assertEquals(2.5F,WeaponMotionProfile.sourceTime(move,(move.duration-1)/20F,2.5F,in,out),.0001);
                float previous=-1;
                for(int i=0;i<move.duration*3;i++) {
                    float t=WeaponMotionProfile.sourceTime(move,i/60F,2.5F,in,out);
                    assertTrue(Float.isFinite(t)&&t>=previous&&t<=2.5F);previous=t;
                }
            }
        }
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
