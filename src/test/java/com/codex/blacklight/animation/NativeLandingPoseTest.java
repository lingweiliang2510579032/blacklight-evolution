package com.codex.blacklight.animation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NativeLandingPoseTest {
    @Test void runningImpactKeepsActualStrideFeetAndRootWhileTorsoCushions() {
        var skeleton=BlacklightSkeleton.get();
        var stride=MotionLibrary.pose("basic_sprint",.19F,true);
        var impact=MotionLibrary.pose("basic_land_moving",.12F,false);
        var result=NativeLandingPose.blendUpper(stride,impact,LandingMotionPolicy.movingCushionWeight(.12F));
        for(String joint:new String[]{"Root","Thigh_R","Thigh_L","Knee_R","Knee_L","Leg_R","Leg_L"}) {
            var bone=skeleton.searchJointByName(joint);
            assertNotNull(bone,joint);
            assertTrue(skeleton.getBoundTransformFor(stride,bone).equals(skeleton.getBoundTransformFor(result,bone),.00001F),joint);
        }
        assertFalse(stride.get("Torso").toMatrix().equals(result.get("Torso").toMatrix(),.00001F));
        var finished=NativeLandingPose.blendUpper(stride,impact,LandingMotionPolicy.movingCushionWeight(.30F));
        assertTrue(stride.get("Torso").toMatrix().equals(finished.get("Torso").toMatrix(),.00001F));
    }
    @Test void visualRootCannotAddTravelToWorldMovementButVerticalImpactSurvives() {
        var impact=MotionLibrary.pose("basic_land_moving",.12F,false);
        var authored=impact.copy();var before=impact.get("Root").copy();
        NativeLandingPose.stripHorizontalRoot(impact);
        assertEquals(0,impact.get("Root").translation().x);
        assertEquals(0,impact.get("Root").translation().z);
        assertEquals(before.translation().y,impact.get("Root").translation().y);
        assertEquals(before.rotation(),impact.get("Root").rotation());
        assertTrue(authored.get("Root").toMatrix().equals(before.toMatrix(),.00001F));
    }
}
