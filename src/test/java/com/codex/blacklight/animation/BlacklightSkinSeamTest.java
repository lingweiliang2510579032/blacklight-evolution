package com.codex.blacklight.animation;

import com.codex.blacklight.client.model.BlacklightSkinWeights;
import com.codex.blacklight.client.renderer.ConsumeAnatomy;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Anatomically adjacent surfaces must stay welded under independent upper/lower limb rotations. */
class BlacklightSkinSeamTest {
    @Test void waistAndShoulderSeamsStayClosedWhileBodyAndLimbsRotateIndependently() {
        var rig=BlacklightSkeleton.get();var bind=rig.getPoseMatrices(new BlacklightPose());
        for(var matrix:bind)matrix.invert();
        for(int frame=0;frame<40;frame++) {
            var pose=new BlacklightPose();float angle=frame*.15F;
            pose.orElseEmpty("Root").rotation().rotateY(angle*.2F);
            pose.orElseEmpty("Torso").rotation().rotateX((float)Math.sin(angle)*.5F);
            pose.orElseEmpty("Chest").rotation().rotateZ((float)Math.cos(angle)*.6F);
            pose.orElseEmpty("Arm_R").rotation().rotateX(angle);
            pose.orElseEmpty("Arm_L").rotation().rotateZ(-angle);
            pose.orElseEmpty("Thigh_R").rotation().rotateX(angle*.7F);
            pose.orElseEmpty("Thigh_L").rotation().rotateX(-angle*.5F);
            var matrices=rig.getPoseMatrices(pose);
            for(int i=0;i<matrices.length;i++)matrices[i].mul(bind[i]);
            for(int side:new int[]{ConsumeAnatomy.RIGHT_LEG,ConsumeAnatomy.LEFT_LEG}) {
                Vector3f waist=new Vector3f(side==ConsumeAnatomy.RIGHT_LEG?.2F:-.2F,.751F,-.125F);
                assertEquals(0,skin(rig,matrices,ConsumeAnatomy.BODY,12,waist).distance(skin(rig,matrices,side,12,waist)),.00001F);
            }
            for(int side:new int[]{ConsumeAnatomy.RIGHT_ARM,ConsumeAnatomy.LEFT_ARM}) {
                Vector3f shoulder=new Vector3f(side==ConsumeAnatomy.RIGHT_ARM?.25F:-.25F,1.501F,-.125F);
                assertEquals(0,skin(rig,matrices,ConsumeAnatomy.BODY,0,shoulder).distance(skin(rig,matrices,side,0,shoulder)),.00001F);
            }
        }
    }
    @Test void elbowWeightIsContinuousAtTheTwoMeshSections() {
        var left=BlacklightSkinWeights.sample(ConsumeAnatomy.RIGHT_ARM,5.9999F,false);
        var right=BlacklightSkinWeights.sample(ConsumeAnatomy.RIGHT_ARM,6.0001F,false);
        assertEquals(left.first(),right.first());assertEquals(left.second(),right.second());
        assertEquals(left.secondWeight(),right.secondWeight(),.001F);
    }
    private static Vector3f skin(BlacklightSkeleton rig,Matrix4f[] matrices,int region,float y,Vector3f point) {
        var blend=BlacklightSkinWeights.sample(region,y,false);
        var first=matrices[rig.searchJointByName(blend.first()).getId()].transformPosition(new Vector3f(point));
        var second=matrices[rig.searchJointByName(blend.second()).getId()].transformPosition(new Vector3f(point));
        return first.lerp(second,blend.secondWeight());
    }
}
