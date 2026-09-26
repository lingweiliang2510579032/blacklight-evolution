package com.codex.blacklight.integration.epicfight;

import yesman.epicfight.api.animation.JointTransform;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;

/** Rigid surface placement; source joint curves remain unchanged. Epic Fight uses Y-up, -Z-forward. */
public final class NativeWallPose {
    private static final float PLAYER_SCALE=.9375F;
    private NativeWallPose() {}
    public static void orientRun(Pose pose,Armature armature,double right,double up,float wallDistance){
        // All five source run clips travel along their local forward axis. That axis
        // must point along wall travel, while source ground-up points out of the wall.
        float angle=(float)-Math.toDegrees(Math.atan2(right,Math.abs(up)+Math.abs(right)<.001?1:up));
        var surface=OpenMatrix4f.createTranslation(0,.7F,-wallDistance/PLAYER_SCALE);
        surface.mulBack(OpenMatrix4f.createRotatorDeg(angle,new Vec3f(0,0,1)));
        surface.mulBack(OpenMatrix4f.createRotatorDeg(90,new Vec3f(1,0,0)));
        transformRoot(pose,armature,surface);
    }
    public static float headMinimumZ(Pose pose,Armature armature){
        var head=armature.getBoundTransformFor(pose,armature.searchJointByName("Head"));
        float minimum=Float.POSITIVE_INFINITY;
        // Include the outer skin layer, not just the neck joint or head centre.
        for(float x:new float[]{-.275F,.275F})for(float y:new float[]{-.025F,.525F})for(float z:new float[]{-.275F,.275F})
            minimum=Math.min(minimum,head.m02*x+head.m12*y+head.m22*z+head.m32);
        return minimum;
    }
    public static void clearHead(Pose pose,Armature armature,float wallDistance){
        float shift=-wallDistance/PLAYER_SCALE+.025F-headMinimumZ(pose,armature);
        if(shift>0)translate(pose,armature,0,0,shift);
    }
    public static void faceWall(Pose pose,Armature armature,OpenMatrix4f model,double normalX,double normalZ){
        var inverse=OpenMatrix4f.invert(renderModel(model),null);
        double x=inverse.m00*normalX+inverse.m20*normalZ;
        double z=inverse.m02*normalX+inverse.m22*normalZ;
        transformRoot(pose,armature,OpenMatrix4f.createRotatorDeg((float)Math.toDegrees(Math.atan2(x,z)),new Vec3f(0,1,0)));
    }
    public static OpenMatrix4f renderModel(OpenMatrix4f model){
        // PatchedEntityRenderer.mulPoseStack applies this half-turn before the
        // patch's model matrix. Collision and wall-facing must use both transforms.
        return OpenMatrix4f.mul(OpenMatrix4f.createRotatorDeg(180,new Vec3f(0,1,0)),model,null);
    }
    public static void translate(Pose pose,Armature armature,float x,float y,float z){
        var inverse=OpenMatrix4f.invert(armature.searchJointByName("Root").getLocalTransform(),null);
        var root=pose.orElseEmpty("Root").copy();var t=root.translation();
        t.x+=inverse.m00*x+inverse.m10*y+inverse.m20*z;
        t.y+=inverse.m01*x+inverse.m11*y+inverse.m21*z;
        t.z+=inverse.m02*x+inverse.m12*y+inverse.m22*z;
        pose.putJointData("Root",root);
    }
    private static void transformRoot(Pose pose,Armature armature,OpenMatrix4f surface){
        var joint=armature.searchJointByName("Root");var bind=joint.getLocalTransform();
        var world=OpenMatrix4f.mul(bind,pose.orElseEmpty("Root").toMatrix(),null);
        var transformed=OpenMatrix4f.mul(surface,world,null);
        pose.putJointData("Root",JointTransform.fromMatrix(OpenMatrix4f.mul(OpenMatrix4f.invert(bind,null),transformed,null)));
    }
}
