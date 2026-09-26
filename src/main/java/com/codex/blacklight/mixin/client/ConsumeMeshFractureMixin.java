package com.codex.blacklight.mixin.client;

import com.codex.blacklight.client.renderer.ConsumeFracture;
import com.codex.blacklight.client.renderer.ConsumeAnatomy;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.api.client.model.Mesh;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.client.renderer.EpicFightRenderTypes;

/** GPU skinning bypasses VertexConsumer. Only a currently fractured victim uses the
 * equivalent CPU posed-mesh route; the user's global rendering setting is untouched. */
@Mixin(value=SkinnedMesh.class,remap=false)
public abstract class ConsumeMeshFractureMixin {
    @Unique private ConsumeAnatomy.Consumer blacklight$receiver;
    @Unique private int[] blacklight$jointParts;
    @Inject(method="draw(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/RenderType;Lyesman/epicfight/api/client/model/Mesh$DrawingFunction;IFFFFILyesman/epicfight/api/model/Armature;[Lyesman/epicfight/api/utils/math/OpenMatrix4f;)V",at=@At("HEAD"),cancellable=true)
    private void blacklight$fracture(PoseStack stack,MultiBufferSource buffers,RenderType type,Mesh.DrawingFunction drawing,
                                    int light,float red,float green,float blue,float alpha,int overlay,Armature armature,OpenMatrix4f[] poses,CallbackInfo ci){
        if(!(buffers instanceof ConsumeFracture))return;
        var out=buffers.getBuffer(EpicFightRenderTypes.getTriangulated(type));
        var previous=blacklight$receiver;var previousParts=blacklight$jointParts;
        try {
            blacklight$receiver=out instanceof ConsumeAnatomy.Consumer consumer?consumer:null;
            blacklight$jointParts=new int[poses.length];
            if(armature!=null)blacklight$mapJoint(armature.rootJoint,0);
            ((SkinnedMesh)(Object)this).drawPosed(stack,out,drawing,light,red,green,blue,alpha,overlay,armature,poses);
        }finally{blacklight$receiver=previous;blacklight$jointParts=previousParts;}
        ci.cancel();
    }
    @Unique private void blacklight$mapJoint(yesman.epicfight.api.animation.Joint joint,int inherited){
        int id=ConsumeAnatomy.named(joint.getName(),inherited);
        if(joint.getId()>=0&&joint.getId()<blacklight$jointParts.length)blacklight$jointParts[joint.getId()]=id;
        for(var child:joint.getSubJoints())blacklight$mapJoint(child,id);
    }
    @Inject(method="getVertexPosition",at=@At("HEAD"))
    private void blacklight$weights(int vertex,org.joml.Vector4f position,OpenMatrix4f[] poses,CallbackInfo ci){
        if(blacklight$receiver==null)return;
        var mesh=(SkinnedMesh)(Object)this;float[] weights=new float[ConsumeAnatomy.COUNT];
        for(int i=0;i<mesh.affectingJointCounts()[vertex];i++){
            int joint=mesh.affectingJointIndices()[vertex][i];
            int part=joint>=0&&joint<blacklight$jointParts.length?blacklight$jointParts[joint]:0;
            weights[part]+=mesh.weights()[mesh.affectingWeightIndices()[vertex][i]];
        }
        blacklight$receiver.anatomy(weights);
    }
}
