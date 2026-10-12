package com.codex.blacklight.client.renderer;

import com.codex.blacklight.registry.ModAttachments;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import java.util.*;

/** World-space biomass slipstream: history follows turns instead of rotating with the camera. */
@EventBusSubscriber(modid="blacklight", value=Dist.CLIENT)
public final class GlideTrailRenderer {
    private record Sample(double time, Vec3 center, Vec3 side) {}
    private static final Map<Player, ArrayDeque<Sample>> TRAILS = new WeakHashMap<>();
    @SubscribeEvent
    public static void render(RenderLevelStageEvent e) {
        if (e.getStage()!=RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        var mc=Minecraft.getInstance();
        if(mc.level==null){TRAILS.clear();return;}
        float partial=e.getPartialTick();
        double now=mc.level.getGameTime()+partial;
        TRAILS.entrySet().removeIf(v->v.getKey().isRemoved()||v.getKey().level()!=mc.level);
        for(var player:mc.level.players()) {
            if(player.onGround()||player.isInWater()||player.isPassenger()
                    ||com.codex.blacklight.registry.ModAttachments.getData(player, ModAttachments.GLIDE_UNTIL)<now
                    ||com.codex.blacklight.registry.ModAttachments.getData(player, ModAttachments.WEAPON_VISUAL).spec()!=null) continue;
            var velocity=player.getDeltaMovement();
            var dir=new Vec3(velocity.x,0,velocity.z).normalize();
            if(dir.lengthSqr()<.1) continue;
            var point=player.getPosition(partial).add(0,.85,0).subtract(dir.scale(.3));
            var samples=TRAILS.computeIfAbsent(player,k->new ArrayDeque<>());
            if(!samples.isEmpty()) {
                var last=samples.getLast();
                if(now<last.time||now-last.time>1.5||point.distanceToSqr(last.center)>36)samples.clear();
                else if(now-last.time<.2)continue;
            }
            samples.add(new Sample(now,point,new Vec3(-dir.z,0,dir.x)));
            while(samples.size()>32)samples.removeFirst();
        }
        var stack=e.getPoseStack();var camera=e.getCamera().getPosition();
        stack.pushPose();stack.translate(-camera.x,-camera.y,-camera.z);
        var type=RenderType.entityTranslucent(ConsumedMobVisuals.WHITE);
        var buffers=mc.renderBuffers().bufferSource();var out=buffers.getBuffer(type);
        for(var samples:TRAILS.values()) {
            while(!samples.isEmpty()&&now-samples.getFirst().time>6)samples.removeFirst();
            Sample previous=null;
            for(var current:samples) {
                if(previous!=null) for(int sign:new int[]{-1,1}) {
                    for(int strand=0;strand<2;strand++) {
                        Vec3 a=point(previous,now,sign,strand),b=point(current,now,sign,strand);
                        double fadeA=net.minecraft.util.Mth.clamp(1-(now-previous.time)/6,0,1);
                        double fadeB=net.minecraft.util.Mth.clamp(1-(now-current.time)/6,0,1);
                        Vec3 wa=previous.side.scale(.045*fadeA),wb=current.side.scale(.045*fadeB);
                        quad(out,stack,a,b,wa,wb,(int)(210*fadeA)<<24|0x211016,(int)(210*fadeB)<<24|0x211016);
                        quad(out,stack,a,b,new Vec3(0,.026*fadeA,0),new Vec3(0,.026*fadeB,0),
                                (int)(165*fadeA)<<24|0xa32a2b,(int)(165*fadeB)<<24|0xa32a2b);
                    }
                }
                previous=current;
            }
        }
        stack.popPose();buffers.endBatch(type);
    }
    private static Vec3 point(Sample s,double now,int sign,int strand) {
        double age=now-s.time;
        return s.center.add(s.side.scale(sign*(.32+strand*.15+age*.025)))
                .add(0,strand*.32+Math.sin(s.time*.8+strand)*age*.012,0);
    }
    private static void quad(VertexConsumer out,PoseStack stack,Vec3 a,Vec3 b,Vec3 wa,Vec3 wb,int ca,int cb) {
        vertex(out,stack,a.subtract(wa),ca);vertex(out,stack,a.add(wa),ca);
        vertex(out,stack,b.add(wb),cb);vertex(out,stack,b.subtract(wb),cb);
    }
    private static void vertex(VertexConsumer out,PoseStack stack,Vec3 p,int color) {
        out.vertex((stack.last()).pose(),(float)p.x,(float)p.y,(float)p.z).color(color).uv(.5F,.5F)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xf000f0).normal((stack.last()).normal(),0,1,0).endVertex();
    }
}
