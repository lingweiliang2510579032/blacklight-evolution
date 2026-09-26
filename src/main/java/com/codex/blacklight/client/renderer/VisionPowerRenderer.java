package com.codex.blacklight.client.renderer;

import com.codex.blacklight.power.*;
import com.codex.blacklight.infection.BlacklightInfectionSystem;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Bounded sensory markers; client draws only entities already tracked by the server. */
@EventBusSubscriber(modid="blacklight",value=Dist.CLIENT)
public final class VisionPowerRenderer {
    @SubscribeEvent public static void render(RenderLevelStageEvent e){
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL)return;var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;
        boolean infected=UtilityPowerSystem.active(mc.player,PowerForm.INFECTED_VISION),thermal=UtilityPowerSystem.active(mc.player,PowerForm.THERMAL_VISION);if(!infected&&!thermal)return;
        double radius=infected?32:24;var targets=mc.level.getEntitiesOfClass(LivingEntity.class,mc.player.getBoundingBox().inflate(radius),v->v!=mc.player&&v.isAlive()&&!v.isSpectator()&&v.distanceToSqr(mc.player)<radius*radius&&(!infected||BlacklightInfectionSystem.isInfected(v)));
        targets.sort(java.util.Comparator.comparingDouble(mc.player::distanceToSqr));if(targets.isEmpty())return;
        var camera=e.getCamera().getPosition();var stack=e.getPoseStack();stack.pushPose();stack.translate(-camera.x,-camera.y,-camera.z);
        RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.disableDepthTest();RenderSystem.depthMask(false);RenderSystem.setShader(GameRenderer::getPositionColorShader);
        var builder=Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES,DefaultVertexFormat.POSITION_COLOR);var matrix=stack.last().pose();int color=infected?0xDDFF3C45:0xDDFFBD63;
        for(var target:targets.subList(0,Math.min(48,targets.size()))){
            var b=target.getBoundingBox();double[][] vertices={{b.minX,b.minY,b.minZ},{b.maxX,b.minY,b.minZ},{b.maxX,b.minY,b.maxZ},{b.minX,b.minY,b.maxZ},{b.minX,b.maxY,b.minZ},{b.maxX,b.maxY,b.minZ},{b.maxX,b.maxY,b.maxZ},{b.minX,b.maxY,b.maxZ}};
            for(int i=0;i<4;i++)for(int[] edge:new int[][]{{i,(i+1)%4},{i+4,(i+1)%4+4},{i,i+4}})for(int v:edge)builder.addVertex(matrix,(float)vertices[v][0],(float)vertices[v][1],(float)vertices[v][2]).setColor(color);
        }
        BufferUploader.drawWithShader(builder.buildOrThrow());RenderSystem.depthMask(true);RenderSystem.enableDepthTest();RenderSystem.disableBlend();stack.popPose();
    }
}
