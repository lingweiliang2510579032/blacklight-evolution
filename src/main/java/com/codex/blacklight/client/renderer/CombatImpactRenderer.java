package com.codex.blacklight.client.renderer;

import com.codex.blacklight.network.CombatImpactPayload;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import java.util.*;

/** Brief broken radial cracks and pressure flashes; no permanent edits to blocks. */
@EventBusSubscriber(modid="blacklight",value=Dist.CLIENT)
public final class CombatImpactRenderer {
    private record Crack(Vec3 a,Vec3 b,double distance){}
    private record Burst(ClientLevel level,CombatImpactPayload event,long started,List<Crack> cracks){}
    private static final List<Burst> BURSTS=new ArrayList<>();
    public static void accept(CombatImpactPayload p){
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||!Double.isFinite(p.x()+p.y()+p.z()))return;
        var center=new Vec3(p.x(),p.y()+.016,p.z());var cracks=new ArrayList<Crack>();
        if(p.ground())for(int ray=0;ray<13;ray++){
            Vec3 prior=center;double angle=ray*Math.PI*2/13;
            for(int step=1;step<=4;step++){
                double length=p.radius()*step/4.0*(.72+.18*Math.sin(ray*4.7));double a=angle+Math.sin(ray*7+step*4.1)*.13;
                var at=center.add(Math.cos(a)*length,0,Math.sin(a)*length);
                var surface=mc.level.clip(new ClipContext(at.add(0,.22,0),at.add(0,-.25,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,mc.player));
                if(surface.getType()!=HitResult.Type.BLOCK||surface.getDirection()!=net.minecraft.core.Direction.UP)break;
                var next=surface.getLocation().add(0,.018,0);cracks.add(new Crack(prior,next,length));prior=next;
            }
        }
        if(BURSTS.size()>=40)BURSTS.removeFirst();
        BURSTS.add(new Burst(mc.level,p,mc.level.getGameTime(),List.copyOf(cracks)));
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent e){
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)return;
        var mc=Minecraft.getInstance();if(mc.level==null){BURSTS.clear();return;}
        double time=mc.level.getGameTime()+e.getPartialTick().getGameTimeDeltaPartialTick(false);
        BURSTS.removeIf(b->b.level!=mc.level||time-b.started>(b.event.ground()?15:b.event.slash()?6:4));
        var type=RenderType.entityTranslucent(ConsumedMobVisuals.WHITE);var buffers=mc.renderBuffers().bufferSource();var out=buffers.getBuffer(type);
        var stack=e.getPoseStack();var camera=e.getCamera().getPosition();stack.pushPose();stack.translate(-camera.x,-camera.y,-camera.z);
        for(var b:BURSTS){
            var p=b.event;double age=Math.max(0,time-b.started);var center=new Vec3(p.x(),p.y()+.02,p.z());
            if(p.slash()){
                var along=new Vec3(p.dx(),p.dy(),p.dz()).normalize();
                var facing=camera.subtract(center).normalize();var side=along.cross(facing).normalize();
                if(side.lengthSqr()<.001)side=along.cross(new Vec3(0,1,0)).normalize();
                if(side.lengthSqr()<.001)side=new Vec3(1,0,0);
                double extent=p.radius()*(.75+.18*Math.min(age,2));
                int alpha=(int)(155*Math.clamp(1-age/6,0,1));
                cut(stack,out,center,along,side,extent,.095,alpha<<24|0x6b1722);
                if(age<2){
                    int flash=(int)(235*(1-age/2));
                    cut(stack,out,center.add(facing.scale(.005)),along,side,extent*.9,.024,flash<<24|0xffe8db);
                }
            }else if(p.ground()){
                double extent=p.radius()*Math.min(1,(age+1)/3);int alpha=(int)(200*Math.min(1,(15-age)/8));
                for(var crack:b.cracks)if(crack.distance<=extent){
                    var along=crack.b.subtract(crack.a);var side=new Vec3(-along.z,0,along.x).normalize().scale(.028*(1-crack.distance/(p.radius()*1.4)));
                    quad(stack,out,alpha<<24|0x29211c,crack.a.add(side),crack.b.add(side),crack.b.subtract(side),crack.a.subtract(side));
                }
                // A wide, fading front made of uneven patches, not a solid wire circle.
                if(age<7)for(int i=0;i<28;i++){
                    double a=i*Math.PI*2/28,c=a+Math.PI*1.5/28,r=p.radius()*Math.min(1,(age+1)/6)*(1+.035*Math.sin(i*3));
                    double inner=Math.max(0,r-.28);int opacity=(int)(58*(1-age/7));
                    quad(stack,out,opacity<<24|0xb2a294,point(center,a,inner),point(center,a,r),point(center,c,r),point(center,c,inner));
                }
            }else{
                double radius=p.radius()*(.3+age*.18);int alpha=(int)(185*(1-age/4));
                var facing=camera.subtract(center).normalize();var right=facing.cross(new Vec3(0,1,0)).normalize();var up=right.cross(facing).normalize();
                for(int i=0;i<6;i++){
                    double a=i*Math.PI/3;var dir=right.scale(Math.cos(a)).add(up.scale(Math.sin(a)));var side=right.scale(-Math.sin(a)).add(up.scale(Math.cos(a))).scale(radius*.08);
                    var tip=center.add(dir.scale(radius));quad(stack,out,alpha<<24|0xffead0,center,tip.add(side),tip,tip.subtract(side));
                }
            }
        }
        stack.popPose();buffers.endBatch(type);
    }
    private static void cut(PoseStack stack,VertexConsumer out,Vec3 center,Vec3 along,Vec3 side,double length,double width,int color){
        // A tapering slice aligned to the strike, not a camera-facing explosion.
        var a=center.subtract(along.scale(length));var b=center.add(along.scale(length));
        quad(stack,out,color,a,center.add(side.scale(width)),b,center.subtract(side.scale(width)));
    }
    private static Vec3 point(Vec3 c,double a,double r){return c.add(Math.cos(a)*r,0,Math.sin(a)*r);}
    private static void quad(PoseStack stack,VertexConsumer out,int color,Vec3... points){
        for(var p:points)out.addVertex(stack.last(),(float)p.x,(float)p.y,(float)p.z).setColor(color).setUv(.5F,.5F)
            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xf000f0).setNormal(stack.last(),0,1,0);
    }
}
