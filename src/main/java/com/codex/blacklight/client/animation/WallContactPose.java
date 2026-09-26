package com.codex.blacklight.client.animation;

import com.codex.blacklight.movement.WallPoseState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/** Final blended-pose contact: a safe endpoint does not guarantee a safe rotational transition. */
public final class WallContactPose {
    private WallContactPose() {}
    public static void apply(LivingEntityPatch<?> patch,Armature armature,Pose pose,float partial){
        var entity=patch.getOriginal();
        if(!(entity instanceof Player))return;
        var wall=WallPoseState.get(entity);if(wall==null)return;
        var normal=new Vec3(wall.normalX(),0,wall.normalZ());
        var position=entity.getPosition(partial);var start=position.add(0,.78,0);
        var hit=entity.level().clip(new ClipContext(start,start.subtract(normal.scale(1.2)),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,entity));
        if(hit.getType()!=HitResult.Type.BLOCK||hit.getDirection().getAxis().isVertical())return;
        var model=com.codex.blacklight.integration.epicfight.NativeWallPose.renderModel(patch.getModelMatrix(partial));
        var head=OpenMatrix4f.mul(model,armature.getBoundTransformFor(pose,armature.searchJointByName("Head")),null);
        double minimum=Double.POSITIVE_INFINITY;
        for(float x:new float[]{-.275F,.275F})for(float y:new float[]{-.025F,.525F})for(float z:new float[]{-.275F,.275F}){
            double dx=position.x+head.m00*x+head.m10*y+head.m20*z+head.m30-hit.getLocation().x;
            double dz=position.z+head.m02*x+head.m12*y+head.m22*z+head.m32-hit.getLocation().z;
            minimum=Math.min(minimum,dx*normal.x+dz*normal.z);
        }
        // The head alone is not enough during a run/jump blend: a bent knee or
        // trailing foot can be deeper in the wall than the face. Keep the complete
        // body outside the contact plane, including the joint-mounted charge coils.
        for(String name:new String[]{"Thigh_R","Leg_R","Thigh_L","Leg_L","Arm_R","Hand_R","Arm_L","Hand_L"}){
            var joint=armature.searchJointByName(name);if(joint==null)continue;
            var bone=OpenMatrix4f.mul(model,armature.getBoundTransformFor(pose,joint),null);
            float radius=name.contains("Leg")||name.contains("Thigh")?.155F:.13F;
            float length=name.contains("Leg")||name.contains("Thigh")?.43F:.32F;
            for(float x:new float[]{-radius,radius})for(float y:new float[]{0,length})for(float z:new float[]{-radius,radius}){
                double dx=position.x+bone.m00*x+bone.m10*y+bone.m20*z+bone.m30-hit.getLocation().x;
                double dz=position.z+bone.m02*x+bone.m12*y+bone.m22*z+bone.m32-hit.getLocation().z;
                minimum=Math.min(minimum,dx*normal.x+dz*normal.z);
            }
        }
        double correction=.02-minimum;
        if(correction<=0)return;
        var inverse=OpenMatrix4f.invert(model,null);
        float x=(float)((inverse.m00*normal.x+inverse.m20*normal.z)*correction);
        float y=(float)((inverse.m01*normal.x+inverse.m21*normal.z)*correction);
        float z=(float)((inverse.m02*normal.x+inverse.m22*normal.z)*correction);
        com.codex.blacklight.integration.epicfight.NativeWallPose.translate(pose,armature,x,y,z);
    }
}
