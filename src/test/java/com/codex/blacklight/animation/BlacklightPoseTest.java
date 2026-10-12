package com.codex.blacklight.animation;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BlacklightPoseTest {
    @Test void transitionInterpolatesRotationAndMissingBonesAgainstIdentity() {
        var idle=new BlacklightPose();
        var action=new BlacklightPose();
        var hand=action.orElseEmpty("Hand_R");
        hand.translation().set(2,4,6);
        hand.rotation().rotationZ((float)Math.PI/2);
        var half=BlacklightPose.interpolatePose(idle,action,.5F).get("Hand_R");
        assertEquals(new Vector3f(1,2,3),half.translation());
        var rotated=half.rotation().transform(new Vector3f(1,0,0));
        assertEquals(Math.sqrt(.5),rotated.x,.00001);
        assertEquals(Math.sqrt(.5),rotated.y,.00001);
        assertEquals(1,half.rotation().lengthSquared(),.00001);
        assertFalse(idle.hasTransform("Hand_R"));
        assertEquals(new Vector3f(2,4,6),hand.translation());
    }
    @Test void sampledCopiesDoNotShareMutableBoneDeltas() {
        var pose=new BlacklightPose();pose.orElseEmpty("Chest").translation().set(1,2,3);
        var copy=pose.copy();copy.get("Chest").translation().set(9,9,9);
        var transition=BlacklightPose.interpolatePose(pose,copy,.5F);
        transition.get("Chest").translation().zero();
        assertEquals(new Vector3f(1,2,3),pose.get("Chest").translation());
        assertEquals(new Vector3f(9,9,9),copy.get("Chest").translation());
    }
    @Test void reflectedBoneMatrixKeepsItsHandednessAfterRoundTrip() {
        var matrix=new org.joml.Matrix4f().translation(1,2,3).rotateY(.4F).scale(-2,3,4);
        assertTrue(matrix.equals(BoneTransform.fromMatrix(matrix).toMatrix(),.00001F));
    }
}
