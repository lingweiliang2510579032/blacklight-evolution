package com.codex.blacklight.client.renderer;

import com.codex.blacklight.entity.HiveTendril;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Visible tissue knots mark the actual severable portions of the cable. */
public final class HiveTendrilRenderer extends EntityRenderer<HiveTendril> {
    public HiveTendrilRenderer(EntityRendererProvider.Context context){super(context);}
    @Override public ResourceLocation getTextureLocation(HiveTendril entity){return ConsumedMobVisuals.WHITE;}
    @Override public void render(HiveTendril entity,float yaw,float partial,PoseStack stack,MultiBufferSource buffers,int light){
        var out=buffers.getBuffer(RenderType.entityCutoutNoCull(ConsumedMobVisuals.WHITE));
        double pulse=.26+Math.sin((entity.tickCount+partial)*.25)*.035;
        ConsumedMobVisuals.tube(stack,out,light,new Vec3(0,.16,0),new Vec3(0,.64,0),pulse,0xff8a3940);
    }
}
