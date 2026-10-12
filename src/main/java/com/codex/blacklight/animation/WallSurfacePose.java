package com.codex.blacklight.animation;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Rigid wall-local placement shared by the renderer and server pose validation. */
public final class WallSurfacePose {
    private static final float PLAYER_SCALE=.9375F;
    private WallSurfacePose() {}
    /** Gravity during a stopped or retiring wall pose is not a downward running command. */
    public static double normalizeRunVertical(double right,double up,boolean grounded,boolean retiring) {
        return (grounded||retiring)&&Math.abs(right)<.018&&Math.abs(up)<.12?0:up;
    }
    public static void orientRun(BlacklightPose pose,BlacklightSkeleton skeleton,double right,double up,float distance) {
        float angle=(float)-Math.atan2(right,Math.abs(up)+Math.abs(right)<.001?1:up);
        var hip=skeleton.getBoundTransformFor(pose,skeleton.rootJoint).getTranslation(new Vector3f());
        var axis=skeleton.getBoundTransformFor(pose,skeleton.head).getTranslation(new Vector3f()).sub(hip);
        // The authored sprint leans 40..65 degrees into its original travel plane.
        // Align that actual torso axis to the wall tangent around the hips. Using
        // a fixed quarter turn rotates both the stance and its world-space origin.
        float tilt=Math.abs(axis.y)+Math.abs(axis.z)<.001F?0:(float)Math.atan2(-axis.z,axis.y);
        tilt=Math.clamp(tilt,-(float)Math.PI/2,(float)Math.PI/2);
        var surface=new Matrix4f().translation(hip.x,hip.y,-distance/PLAYER_SCALE)
                .rotateZ(angle).rotateX(tilt).translate(-hip.x,-hip.y,-hip.z);
        transformRoot(pose,skeleton,surface);
    }
    public static float headMinimumZ(BlacklightPose pose,BlacklightSkeleton skeleton) {
        var head=skeleton.getBoundTransformFor(pose,skeleton.head);
        float minimum=Float.POSITIVE_INFINITY;
        for(float x:new float[]{-.275F,.275F})for(float y:new float[]{-.025F,.525F})for(float z:new float[]{-.275F,.275F})
            minimum=Math.min(minimum,head.m02()*x+head.m12()*y+head.m22()*z+head.m32());
        return minimum;
    }
    public static void clearHead(BlacklightPose pose,BlacklightSkeleton skeleton,float distance) {
        float shift=-distance/PLAYER_SCALE+.025F-headMinimumZ(pose,skeleton);
        if(shift>0)translate(pose,skeleton,0,0,shift);
    }
    public static float skinMinimumZ(BlacklightPose pose,BlacklightSkeleton skeleton) {
        float minimum=headMinimumZ(pose,skeleton);
        for(String name:new String[]{"Thigh_R","Leg_R","Thigh_L","Leg_L","Arm_R","Hand_R","Arm_L","Hand_L"}) {
            var joint=skeleton.searchJointByName(name);if(joint==null)continue;
            var matrix=skeleton.getBoundTransformFor(pose,joint);
            boolean leg=name.contains("Leg")||name.contains("Thigh");
            float radius=leg?.155F:.13F,length=leg?.43F:.32F;
            for(float x:new float[]{-radius,radius})for(float y:new float[]{0,length})for(float z:new float[]{-radius,radius})
                minimum=Math.min(minimum,matrix.m02()*x+matrix.m12()*y+matrix.m22()*z+matrix.m32());
        }
        return minimum;
    }
    /** Translate the complete posed skin; preserve the authored stride and arm counterbalance. */
    public static void clearSkin(BlacklightPose pose,BlacklightSkeleton skeleton,float distance) {
        float shift=-distance/PLAYER_SCALE+.025F-skinMinimumZ(pose,skeleton);
        if(shift>0)translate(pose,skeleton,0,0,shift);
    }
    public static void faceWall(BlacklightPose pose,BlacklightSkeleton skeleton,Matrix4f renderModel,double nx,double nz) {
        var inverse=new Matrix4f(renderModel).invert();
        var normal=inverse.transformDirection(new Vector3f((float)nx,0,(float)nz));
        transformRoot(pose,skeleton,new Matrix4f().rotationY((float)Math.atan2(normal.x,normal.z)));
    }
    public static void translate(BlacklightPose pose,BlacklightSkeleton skeleton,float x,float y,float z) {
        var delta=new Matrix4f(skeleton.rootJoint.getLocalTransform()).invert().transformDirection(new Vector3f(x,y,z));
        pose.orElseEmpty("Root").translation().add(delta);
    }
    private static void transformRoot(BlacklightPose pose,BlacklightSkeleton skeleton,Matrix4f transform) {
        var bind=skeleton.rootJoint.getLocalTransform();
        var world=new Matrix4f(bind).mul(pose.orElseEmpty("Root").toMatrix());
        var local=new Matrix4f(bind).invert().mul(transform).mul(world);
        pose.getJointTransformData().put("Root",BoneTransform.fromMatrix(local));
    }
}
