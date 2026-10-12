package com.codex.blacklight.client.camera;

import com.codex.blacklight.BlacklightNeoMod;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.*;

/** Depth-aware Minecraft post pass, enabled only by an original CAM DFEN track.
 * Preserves scene depth for the later sensory silhouettes and never blurs the HUD.
 */
@EventBusSubscriber(modid="blacklight",value=Dist.CLIENT)
public final class SourceCameraDepthOfField {
    private static PostChain effect;
    private static RenderTarget savedDepth;
    private static Object level;
    private static int width,height;
    private static boolean failed;
    private static void close(){if(effect!=null){effect.close();effect=null;}if(savedDepth!=null){savedDepth.destroyBuffers();savedDepth=null;}}
    @SubscribeEvent public static void reload(RegisterClientReloadListenersEvent e) {
        e.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)manager->
                Minecraft.getInstance().execute(()->{close();failed=false;}));
    }
    @SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent e){if(e.phase != net.minecraftforge.event.TickEvent.Phase.END)return;
        var current=Minecraft.getInstance().level;
        if(current!=level){close();failed=false;level=current;}
    }
    @SubscribeEvent(priority=EventPriority.HIGH) public static void render(RenderLevelStageEvent e) {
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL)return;
        var frame=PrototypeChaseCamera.filmFrame();
        if(frame==null||frame.dof()<.5||frame.weight()<=0){if(effect!=null)close();return;}
        var mc=Minecraft.getInstance();if(failed||mc.level==null)return;
        boolean copied=false;
        try {
            mc.renderBuffers().bufferSource().endBatch();
            if(effect==null){effect=new PostChain(mc.getTextureManager(),mc.getResourceManager(),mc.getMainRenderTarget(),
                    new ResourceLocation("blacklight","shaders/post/source_dof.json"));width=height=-1;}
            var main=mc.getMainRenderTarget();
            if(width!=main.width||height!=main.height) {
                width=main.width;height=main.height;effect.resize(width,height);
                if(savedDepth!=null)savedDepth.destroyBuffers();savedDepth=new TextureTarget(width,height,true,Minecraft.ON_OSX);
            }
            savedDepth.copyDepthFrom(main);copied=true;
            com.codex.blacklight.client.ClientPostUniforms.set(effect,"Strength",(float)frame.weight());
            com.codex.blacklight.client.ClientPostUniforms.set(effect,"FocusDistance",(float)frame.focus());
            com.codex.blacklight.client.ClientPostUniforms.set(effect,"FocusRange",(float)frame.range());
            com.codex.blacklight.client.ClientPostUniforms.set(effect,"Aperture",(float)frame.aperture());
            com.codex.blacklight.client.ClientPostUniforms.set(effect,"NearPlane",PrototypeChaseCamera.nearPlane());
            com.codex.blacklight.client.ClientPostUniforms.set(effect,"FarPlane",PrototypeChaseCamera.farPlane(mc.gameRenderer.getDepthFar()));
            effect.process(e.getPartialTick());
        } catch(Exception ex) {
            failed=true;BlacklightNeoMod.LOGGER.error("Source cinematic depth pass unavailable; reload resources to retry",ex);
        } finally {
            if(copied&&savedDepth!=null)mc.getMainRenderTarget().copyDepthFrom(savedDepth);
            mc.getMainRenderTarget().bindWrite(false);if(failed)close();
        }
    }
}
