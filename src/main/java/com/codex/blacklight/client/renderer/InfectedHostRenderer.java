package com.codex.blacklight.client.renderer;

import com.codex.blacklight.entity.BlacklightInfectedHost;
import com.codex.blacklight.registry.ModAttachments;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;

/** Reuses each host species' articulated model, beneath an anatomical biomass mesh. */
public final class InfectedHostRenderer extends EntityRenderer<BlacklightInfectedHost> {
    public InfectedHostRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=.6F;}
    @Override public ResourceLocation getTextureLocation(BlacklightInfectedHost entity){return ConsumedMobVisuals.WHITE;}
    @Override @SuppressWarnings({"rawtypes","unchecked"})
    public void render(BlacklightInfectedHost entity,float yaw,float partial,PoseStack stack,MultiBufferSource buffers,int light){
        var proxy=entity.appearance();if(proxy==null)return;
        proxy.setId(entity.getId());proxy.copyPosition(entity);
        proxy.xo=entity.xo;proxy.yo=entity.yo;proxy.zo=entity.zo;
        proxy.xOld=entity.xOld;proxy.yOld=entity.yOld;proxy.zOld=entity.zOld;
        proxy.yBodyRot=entity.yBodyRot;proxy.yBodyRotO=entity.yBodyRotO;
        proxy.yHeadRot=entity.yHeadRot;proxy.yHeadRotO=entity.yHeadRotO;
        proxy.xRotO=entity.xRotO;proxy.yRotO=entity.yRotO;
        proxy.tickCount=entity.tickCount;proxy.hurtTime=entity.hurtTime;proxy.deathTime=entity.deathTime;
        proxy.setOnGround(entity.onGround());proxy.setDeltaMovement(entity.getDeltaMovement());
        proxy.attackAnim=entity.attackAnim;proxy.oAttackAnim=entity.oAttackAnim;
        // Do not advance animation per render frame: interpolation is sampled from the host.
        entity.advanceAppearanceAnimation();
        if(proxy instanceof net.minecraft.world.entity.ambient.Bat bat)bat.setResting(false);
        proxy.setHealth(Math.max(1,proxy.getMaxHealth()*entity.getHealth()/entity.getMaxHealth()));
        proxy.setData(ModAttachments.CONSUME_VISUAL,entity.getData(ModAttachments.CONSUME_VISUAL));
        proxy.setData(ModAttachments.HIT_REACTION,entity.getData(ModAttachments.HIT_REACTION));
        for(var slot:EquipmentSlot.values())proxy.setItemSlot(slot,entity.getItemBySlot(slot));
        proxy.setCustomName(null);
        EntityRenderer renderer=Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(proxy);
        var skin=new InfectedSkinBuffers(buffers);
        stack.pushPose();stack.scale(1.12F,1.12F,1.12F);
        try{
            if(renderer instanceof net.minecraft.client.renderer.entity.LivingEntityRenderer living){
                try(var morphology=new InfectedMorphology(entity,proxy,living.getModel())){
                    renderer.render(proxy,yaw,partial,stack,skin,light);
                }
            }else renderer.render(proxy,yaw,partial,stack,skin,light);
        }finally{stack.popPose();}
        super.render(entity,yaw,partial,stack,buffers,light);
    }
}
