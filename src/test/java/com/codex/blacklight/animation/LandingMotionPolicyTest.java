package com.codex.blacklight.animation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LandingMotionPolicyTest {
    @Test void movingContactImmediatelyPermitsStrideAndOnlyLeasesABriefCushion() {
        assertTrue(LandingMotionPolicy.permitsStride("basic_land_moving",0));
        assertEquals(.30F,LandingMotionPolicy.leaseDuration("basic_land_moving",.9F));
        assertEquals(1,LandingMotionPolicy.movingCushionWeight(0));
        assertEquals(.5F,LandingMotionPolicy.movingCushionWeight(.15F),.00001);
        assertEquals(0,LandingMotionPolicy.movingCushionWeight(.30F));
        assertFalse(LandingMotionPolicy.shouldRelease("basic_land_moving",.1F,true,true));
        assertTrue(LandingMotionPolicy.shouldRelease("basic_land_moving",.3F,true,true));
    }
    @Test void heavyContactsKeepTheirPhysicalBraceAndStationaryRecovery() {
        assertFalse(LandingMotionPolicy.permitsStride("basic_land_heavy",.1F));
        assertTrue(LandingMotionPolicy.permitsStride("basic_land_heavy",.15F));
        assertFalse(LandingMotionPolicy.permitsStride("basic_land_devastating",.25F));
        assertTrue(LandingMotionPolicy.permitsStride("basic_land_devastating",.30F));
        assertEquals(1.4F,LandingMotionPolicy.leaseDuration("basic_land_heavy",1.4F));
        assertFalse(LandingMotionPolicy.shouldRelease("basic_land_heavy",1,true,false));
        assertFalse(LandingMotionPolicy.shouldRelease("basic_land_heavy",.15F,true,true));
        assertTrue(LandingMotionPolicy.shouldRelease("basic_land_heavy",.30F,true,true));
    }
    @Test void anotherJumpReleasesOldLandingButCannotReleaseUnrelatedAction() {
        assertTrue(LandingMotionPolicy.shouldRelease("basic_land_moving",.02F,false,true));
        assertTrue(LandingMotionPolicy.shouldRelease("basic_land_heavy",.02F,false,false));
        assertFalse(LandingMotionPolicy.shouldRelease("blade_cut",1,false,true));
        assertFalse(LandingMotionPolicy.permitsStride("basic_jump_running",1));
    }
    @Test void synchronizedLeaseKeepsHeavyAndDevastatingBracesIncludingTransitionTime() {
        for(String clip:new String[]{"basic_land_heavy","basic_land_devastating"}) {
            float authored=clip.endsWith("heavy")?1.4F:3.333333F;
            var state=new NativeActionState(clip,100,0,.08F,
                    LandingMotionPolicy.leaseDuration(clip,authored),0,false,-1,1);
            for(int tick=100;tick<=107;tick++) {
                assertTrue(state.active(tick),clip+" tick="+tick);
                assertFalse(LandingMotionPolicy.shouldRelease(clip,state.seconds(tick),true,true),
                        "a synced action cannot clear before the cushion and physical brace finish: "+clip+" tick="+tick);
            }
            assertTrue(LandingMotionPolicy.shouldRelease(clip,state.seconds(108),true,true));
            assertFalse(LandingMotionPolicy.shouldRelease(clip,state.seconds(108),true,false),
                    "stationary impact retains its complete authored recovery");
            assertEquals(authored,state.duration());
        }
    }
}
