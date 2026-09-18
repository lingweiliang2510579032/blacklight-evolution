package com.codex.blacklight.client.renderer.layer;

import com.codex.blacklight.client.model.InfectedZombieMesh;
import com.codex.blacklight.client.renderer.InfectedZombieRenderer;
import com.codex.blacklight.infection.BlacklightInfectionSystem;
import com.codex.blacklight.infection.InfectionTimeline;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Zombie;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.client.renderer.patched.layer.UniqueLayer;
import yesman.epicfight.world.capabilities.entitypatch.mob.ZombiePatch;

public final class InfectedZombieLayer extends UniqueLayer<Zombie,ZombiePatch<Zombie>,ZombieModel<Zombie>> {
    private static final ResourceLocation WHITE=ResourceLocation.fromNamespaceAndPath("blacklight","textures/entity/claw_white.png");
    private InfectedZombieMesh model;
    @Override protected void renderLayer(ZombiePatch<Zombie> patch,Zombie zombie,PoseStack stack,MultiBufferSource buffer,
            int light,OpenMatrix4f[] poses,float bob,float yaw,float pitch,float partial) {
        if(!BlacklightInfectionSystem.isInfected(zombie) || zombie.isInvisible())return;
        if(model==null)model=new InfectedZombieMesh();
        float t=InfectedZombieRenderer.progress(zombie,partial);
        var out=buffer.getBuffer(RenderType.entityCutoutNoCull(WHITE));
        int overlay=OverlayTexture.pack(OverlayTexture.u(0),OverlayTexture.v(zombie.hurtTime>0||zombie.deathTime>0));
        for(String name:model.joints()) {
            var joint=patch.getArmature().searchJointByName(name);if(joint==null)continue;
            stack.pushPose();var matrix=poses[joint.getId()];
            MathUtils.translateStack(stack,matrix);MathUtils.rotateStack(stack,matrix);MathUtils.scaleStack(stack,matrix);
            model.render(name,stack,out,light,overlay,InfectionTimeline.growth(name,t));stack.popPose();
        }
    }
}
