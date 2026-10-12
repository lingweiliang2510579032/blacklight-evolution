package com.codex.blacklight.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/** Explicit target feedback is native to Blacklight and follows every user-selected perspective. */
@EventBusSubscriber(modid="blacklight",value=Dist.CLIENT)
public final class TargetLockHud {
    private record WorldFrame(Object level,long frame,Matrix4f view,Matrix4f projection,Vec3 camera,float partial) { }
    public record Marker(int targetId,float x,float y,boolean inFront,boolean onScreen,String cameraType) { }
    private static long frame;
    private static WorldFrame world;
    private static Marker marker;
    private TargetLockHud() { }
    public static Marker lastMarker(){return marker;}
    @SubscribeEvent public static void begin(TickEvent.RenderTickEvent event){if(event.phase!=TickEvent.Phase.START)return;frame++;marker=null;}
    @SubscribeEvent public static void captureWorld(RenderLevelStageEvent event) {
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL)return;
        var mc=Minecraft.getInstance();
        world=new WorldFrame(mc.level,frame,new Matrix4f(event.getPoseStack().last().pose()),new Matrix4f(event.getProjectionMatrix()),
                event.getCamera().getPosition(),event.getPartialTick());
    }
    @SubscribeEvent public static void render(RenderGuiEvent.Post event) {
        var mc=Minecraft.getInstance();var g=event.getGuiGraphics();
        if(mc.player==null||mc.level==null||mc.options.hideGui||!ClientTargetLock.active())return;
        var target=ClientTargetLock.target();
        if(target==null){
            String notice=ClientTargetLock.notice();
            if(!notice.isBlank())panel(g,notice,g.guiWidth()/2,g.guiHeight()-54,0xFFFFCA77);
            return;
        }
        String name=(ClientTargetLock.pending()?"锁定中 · ":"锁定 · ")+target.getDisplayName().getString();
        var captured=world;
        if(captured!=null&&captured.level==mc.level&&captured.frame==frame) {
            var focus=ClientTargetLock.focusPoint(target).add(target.getPosition(captured.partial).subtract(target.position()));
            var eye=captured.camera;
            var point=WorldHudProjection.project(captured.view,captured.projection,eye.x,eye.y,eye.z,
                    focus.x,focus.y,focus.z,g.guiWidth(),g.guiHeight());
            marker=new Marker(target.getId(),point.x(),point.y(),point.inFront(),point.onScreen(),mc.options.getCameraType().name());
            if(point.onScreen()) {
                int x=Math.round(point.x()),y=Math.round(point.y()),color=ClientTargetLock.pending()?0xFFFFCA77:0xFFFF6B64;
                brackets(g,x,y,color);
                panel(g,name,x,Math.min(g.guiHeight()-36,y+17),color);
            }
        }
        String key=com.codex.blacklight.client.ClientClawEvents.targetLockKey().getTranslatedKeyMessage().getString();
        String status=name+"  "+Math.round(Math.sqrt(com.codex.blacklight.combat.CombatTargeting.distanceSquared(mc.player.getEyePosition(),target)))+"m"
                +(marker==null||!marker.onScreen()?" · 目标在视野外":"")+"  ["+key+" 取消]";
        panel(g,status,g.guiWidth()/2,g.guiHeight()-54,0xFFE8DDD6);
    }
    private static void brackets(GuiGraphics g,int x,int y,int color) {
        int r=9,l=5;
        for(int sx:new int[]{-1,1})for(int sy:new int[]{-1,1}) {
            int px=x+sx*r,py=y+sy*r;
            g.fill(Math.min(px,px-sx*l),py,Math.max(px,px-sx*l)+1,py+2,color);
            g.fill(px,Math.min(py,py-sy*l),px+2,Math.max(py,py-sy*l)+1,color);
        }
    }
    private static void panel(GuiGraphics g,String text,int x,int y,int color) {
        var font=Minecraft.getInstance().font;text=font.plainSubstrByWidth(text,Math.max(20,g.guiWidth()-24));
        int w=font.width(text);x=net.minecraft.util.Mth.clamp(x,w/2+6,Math.max(w/2+6,g.guiWidth()-w/2-6));
        g.fill(x-w/2-4,y-2,x+w/2+5,y+11,0xA6090B10);
        g.drawCenteredString(font,text,x,y,color);
    }
}
