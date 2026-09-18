package com.codex.blacklight.client.renderer;

import com.codex.blacklight.BlacklightNeoMod;
import com.codex.blacklight.client.animation.ConsumeVictimPose;
import com.codex.blacklight.power.PowerForm;
import com.codex.blacklight.registry.ModAttachments;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector3f;
import java.util.*;

/** Draw after both entities: use the animated arm's actual rendered transform, with no one-frame attachment lag. */
@EventBusSubscriber(modid=BlacklightNeoMod.MOD_ID,value=Dist.CLIENT)
public final class WhipTetherRenderer {
    private static final Map<Integer,Vec3> HANDS=new HashMap<>();
    private static Vec3 camera=Vec3.ZERO;
    private WhipTetherRenderer() {}
    public static void capture(AbstractClientPlayer player,PoseStack handStack) {
        // The hook's authored local tip. This stack is the very same one used to draw the right forearm.
        // LevelRenderer puts view rotation in RenderSystem's global matrix. Entity stacks are already camera-relative world space.
        var relative=handStack.last().pose().transformPosition(new Vector3f(.18F,.75F,0));
        HANDS.put(player.getId(),new Vec3(relative.x,relative.y,relative.z).add(camera));
    }
    public static Vec3 capturedHand(int entityId) { return HANDS.get(entityId); }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if(event.getStage()==RenderLevelStageEvent.Stage.AFTER_SKY) {
            HANDS.clear();camera=event.getCamera().getPosition();return;
        }
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_ENTITIES)return;
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);
        var buffers=mc.renderBuffers().bufferSource();var type=RenderType.entityCutoutNoCull(ConsumedMobVisuals.WHITE);
        var out=buffers.getBuffer(type);var stack=event.getPoseStack();
        for(var entity:mc.level.entitiesForRendering()) {
            if(!(entity instanceof LivingEntity mob))continue;
            var data=mob.getData(ModAttachments.CONSUME_VISUAL);
            if(!data.active()||data.form()!=PowerForm.WHIPFIST.ordinal())continue;
            var owner=mc.level.getEntity(data.consumerId());if(owner==null)continue;
            float t=data.progress(mc.level.getGameTime(),partial);if(t<.075F||t>.8F)continue;
            Vec3 delta=mob.getPosition(partial).subtract(owner.getPosition(partial));
            // First person / culled owner has no rendered body joint. Keep a local wrist fallback in that case.
            Vec3 right=new Vec3(-delta.z,0,delta.x).normalize().scale(.38);
            Vec3 hand=HANDS.getOrDefault(owner.getId(),owner.getPosition(partial).add(right).add(0,1.05,0));
            Vec3 body=mob.getPosition(partial).add(ConsumeVictimPose.point(mob,new Vec3(0,mob.getBbHeight()*.55,0),partial));
            float reach=Math.clamp((t-.075F)/.11F,0,1);
            Vec3 end=hand.lerp(body,reach);
            Vec3 side=new Vec3(-delta.z,0,delta.x).normalize();
            double sweep=Math.sin(Math.PI*reach)*.8;
            Vec3 control=hand.lerp(end,.45).add(side.scale(sweep)).add(0,.28*(1-reach)-.12,0);
            stack.pushPose();stack.translate(hand.x-camera.x,hand.y-camera.y,hand.z-camera.z);
            int light=mc.getEntityRenderDispatcher().getPackedLightCoords(mob,partial);
            for(int i=0;i<32;i++) {
                double a=i/32.0,b=(i+1)/32.0;
                Vec3 p=bezier(hand,control,end,a).subtract(hand),q=bezier(hand,control,end,b).subtract(hand);
                ConsumedMobVisuals.tube(stack,out,light,p,q,.052*Math.min(1,(.8-t)*10),i%6==0?0xff83382d:0xff30282a);
            }
            stack.popPose();
        }
        buffers.endBatch(type);
    }
    private static Vec3 bezier(Vec3 a,Vec3 b,Vec3 c,double t) {
        return a.scale((1-t)*(1-t)).add(b.scale(2*(1-t)*t)).add(c.scale(t*t));
    }
}
