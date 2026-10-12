package com.codex.blacklight.client.renderer;

import com.codex.blacklight.client.model.BlockArmorModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import org.joml.Vector3f;
import java.util.*;

/** Short-lived pieces taken from the authored shield's currently posed surface. */
@EventBusSubscriber(modid="blacklight",value=Dist.CLIENT)
public final class ShieldShardEffects {
    private record Shard(double born,Vec3 center,Vec3 velocity,Vector3f[] offsets,int material,int light,float spin) { }
    private static final List<Shard> shards=new ArrayList<>();
    private static net.minecraft.client.multiplayer.ClientLevel level;
    private static final int[][] FACES={{0,3,2,1},{4,5,6,7},{0,4,7,3},{1,2,6,5},{3,7,6,2},{0,1,5,4}};
    public static void capture(AbstractClientPlayer player,PoseStack stack,Vector3f[] vertices,int material,int light,float partial,boolean broken) {
        if(shards.size()>=128||VisionPowerRenderer.drawingSilhouettes())return;
        var camera=Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vector3f[] transformed=new Vector3f[vertices.length];Vector3f mean=new Vector3f();
        for(int i=0;i<vertices.length;i++){transformed[i]=stack.last().pose().transformPosition(new Vector3f(vertices[i]));mean.add(transformed[i]);}
        mean.div(vertices.length);
        for(var v:transformed)v.sub(mean).mul(broken?.6F:.25F);
        Vec3 center=new Vec3(mean.x,mean.y,mean.z).add(camera);
        Vec3 velocity=center.subtract(player.position().add(0,1,0)).normalize().scale(broken?.07:.025).add(0,.045,0);
        shards.add(new Shard(player.level().getGameTime()+partial,center,velocity,transformed,material,light,(material%3-1)*.08F+.025F));
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var current=Minecraft.getInstance().level;
        if(current!=level){shards.clear();level=current;}
        if(current!=null)shards.removeIf(s->current.getGameTime()-s.born>18);
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_ENTITIES||shards.isEmpty())return;
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        double now=mc.level.getGameTime()+event.getPartialTick().getGameTimeDeltaPartialTick(false);
        var stack=event.getPoseStack();var camera=event.getCamera().getPosition();stack.pushPose();
        try {
            stack.translate(-camera.x,-camera.y,-camera.z);var pose=stack.last();
            var out=mc.renderBuffers().bufferSource().getBuffer(RenderType.entityTranslucent(BlockArmorModel.TEXTURE));
            for(var s:shards) {
                float age=(float)(now-s.born);if(age<0||age>18)continue;
                Vec3 center=s.center.add(s.velocity.scale(age)).add(0,-.004*age*age,0);
                float alpha=1-Math.clamp((age-8)/10,0,1);Vector3f[] points=new Vector3f[8];
                for(int i=0;i<8;i++)points[i]=new Vector3f(s.offsets[i]).rotateY(s.spin*age);
                for(int[] face:FACES) {
                    var normal=new Vector3f(points[face[1]]).sub(points[face[0]]).cross(new Vector3f(points[face[2]]).sub(points[face[0]])).normalize();
                    for(int i=0;i<4;i++) {
                        var p=points[face[i]];float u=(s.material+((i==1||i==2)?.969F:.031F))/6;
                        out.addVertex(pose,(float)center.x+p.x,(float)center.y+p.y,(float)center.z+p.z)
                                .setColor(255,255,255,(int)(255*alpha)).setUv(u,i<2?.031F:.969F).setOverlay(OverlayTexture.NO_OVERLAY)
                                .setLight(s.light).setNormal(pose,normal.x,normal.y,normal.z);
                    }
                }
            }
        }finally{stack.popPose();}
    }
}
