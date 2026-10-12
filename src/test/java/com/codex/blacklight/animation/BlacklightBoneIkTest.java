package com.codex.blacklight.animation;

import com.codex.blacklight.client.animation.BlacklightBoneIk;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BlacklightBoneIkTest {
    @Test void reachingPalmKeepsArmLengthsAndDoesNotMoveRoot() {
        var rig=BlacklightSkeleton.get();
        var pose=new BlacklightPose();
        var shoulder=rig.searchJointByName("Arm_L");
        var elbow=rig.searchJointByName("Hand_L");
        var wrist=rig.searchJointByName("Tool_L");
        var palmLocal=new Matrix4f(wrist.getLocalTransform()).invert().transformPosition(new Vector3f(.00267F,.32362F,.07666F));
        Vector3f s=position(rig,pose,shoulder),e=position(rig,pose,elbow),w=position(rig,pose,wrist);
        float upper=s.distance(e),lower=e.distance(w);
        Vector3f root=position(rig,pose,rig.rootJoint);
        Vector3f target=rig.getBoundTransformFor(pose,wrist).transformPosition(new Vector3f(palmLocal)).add(.08F,.09F,.10F);
        BlacklightBoneIk.reach(rig,pose,"L",target,palmLocal,48);
        assertEquals(upper,position(rig,pose,shoulder).distance(position(rig,pose,elbow)),.0001F);
        assertEquals(lower,position(rig,pose,elbow).distance(position(rig,pose,wrist)),.0001F);
        assertEquals(0,root.distance(position(rig,pose,rig.rootJoint)),.0001F);
        Vector3f actual=rig.getBoundTransformFor(pose,wrist).transformPosition(new Vector3f(palmLocal));
        assertTrue(actual.distance(target)<.035F,"Palm error: "+actual.distance(target));
    }
    @Test void unreachablePalmRemainsFiniteAndLeavesBindImmutable() {
        var rig=BlacklightSkeleton.get();
        var wrist=rig.searchJointByName("Tool_R");
        var before=wrist.getLocalTransform();
        var pose=new BlacklightPose();
        BlacklightBoneIk.reach(rig,pose,"R",new Vector3f(8,8,8),new Vector3f(),24);
        assertTrue(rig.getBoundTransformFor(pose,wrist).isFinite());
        assertEquals(before,wrist.getLocalTransform());
    }
    private static Vector3f position(BlacklightSkeleton rig,BlacklightPose pose,BlacklightSkeleton.Bone bone) {
        return rig.getBoundTransformFor(pose,bone).getTranslation(new Vector3f());
    }
}
