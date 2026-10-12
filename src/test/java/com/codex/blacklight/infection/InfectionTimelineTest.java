package com.codex.blacklight.infection;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class InfectionTimelineTest {
    @Test void clocksSurviveTrackingAndReload() {
        assertEquals(1,InfectionTimeline.progress(-1,100,0));
        assertEquals(0,InfectionTimeline.progress(200,100,0));
        assertEquals(.5F,InfectionTimeline.progress(100,124,0));
        assertEquals(1,InfectionTimeline.progress(100,10000,0));
    }
    @Test void bodySpreadsBeforeClawsAndSettles() {
        assertTrue(InfectionTimeline.growth("Chest",.3F)>InfectionTimeline.growth("Hand_R",.3F));
        for(String joint:new String[]{"Chest","Head","Arm_R","Hand_R","Hand_L","Leg_R"}) {
            float last=0;
            for(int i=0;i<=100;i++) {float t=InfectionTimeline.growth(joint,i/100F);assertTrue(t>=last);last=t;}
            assertEquals(1,last);
        }
        assertEquals(0,InfectionTimeline.spasm(1));
    }
}
