package com.codex.blacklight.client;

import com.codex.blacklight.registry.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** Fixed-size biological meter; health growth never adds rows of hearts. */
@EventBusSubscriber(modid="blacklight",value=Dist.CLIENT)
public final class PrototypeHealthHud {
    private static LocalPlayer owner;
    private static float lastHealth,lastMaximum,trail;
    private static double lastTime=-1,holdUntil;
    private PrototypeHealthHud(){}
    public static boolean active(){
        var p=Minecraft.getInstance().player;
        return p!=null&&!p.isSpectator()&&p.getData(ModAttachments.PLAYER_STATE).isInfected();
    }
    public static int utilityTop(){return active()?40:10;}
    @SubscribeEvent public static void hideHearts(RenderGuiLayerEvent.Pre event){
        if(active()&&event.getName().equals(VanillaGuiLayers.PLAYER_HEALTH))event.setCanceled(true);
    }
    @SubscribeEvent public static void render(RenderGuiEvent.Post event){
        var mc=Minecraft.getInstance();
        if(!active()||mc.level==null){owner=null;lastTime=-1;return;}
        if(mc.options.hideGui)return;
        var p=mc.player;var g=event.getGuiGraphics();
        float maximum=Math.max(1,p.getMaxHealth()),health=Mth.clamp(p.getHealth(),0,maximum);
        double now=(mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false))/20D;
        boolean reset=owner!=p||lastTime<0||now<lastTime||maximum!=lastMaximum;
        if(reset){owner=p;trail=health;lastHealth=health;holdUntil=now;}
        if(health<lastHealth){trail=Math.max(trail,lastHealth);holdUntil=now+.35;}
        double dt=reset?0:Math.clamp(now-lastTime,0,.25);
        if(health>=trail)trail=health;
        else if(now>holdUntil)trail=Math.max(health,(float)(trail-maximum*dt*.65));
        lastHealth=health;lastMaximum=maximum;lastTime=now;
        int x=10,y=10,w=Math.min(180,mc.getWindow().getGuiScaledWidth()-20);
        if(NativeHealthHud.render(g,x,y,w,health/maximum,trail/maximum,p.getAbsorptionAmount()/maximum))return;
        boolean critical=health>0&&health/maximum<.25F;
        int accent=critical?0xFFFF5948:0xFFDB352E;
        cut(g,x-2,y-2,w+4,24,0xB5090C0F);
        g.fill(x,y,x+2,y+18,accent);
        int bx=x+6,by=y+2,bw=w-10,bh=15;
        cut(g,bx-1,by-1,bw+2,bh+2,0xFF61252A);
        cut(g,bx,by,bw,bh,0xFF1C1118);
        meter(g,bx,by,bw,bh,trail/maximum,0xFFE99666,0xFF9F443A);
        int top=p.hasEffect(MobEffects.POISON)?0xFF86BA51:p.hasEffect(MobEffects.WITHER)?0xFF927B9E:accent;
        meter(g,bx,by,bw,bh,health/maximum,top,0xFF711520);
        for(int i=1;i<5;i++){int sx=bx+bw*i/5;g.fill(sx,by+2,sx+1,by+bh-2,0x5010060C);}
        g.fill(bx+4,by+1,bx+bw-5,by+2,0x32FFE5D5);
        // Absorption has its own meter instead of extra heart rows or inflating HP.
        float absorb=p.getAbsorptionAmount();
        if(absorb>0){
            g.fill(bx,by+bh+3,bx+bw,by+bh+5,0xFF302419);
            g.fill(bx,by+bh+3,bx+Math.max(1,Math.round(bw*Math.min(1,absorb/maximum))),by+bh+5,0xFFF1C66D);
        }
    }
    private static void cut(GuiGraphics g,int x,int y,int w,int h,int color){
        for(int row=0;row<h;row++){
            int inset=Math.max(0,4-Math.min(row,h-1-row));
            g.fill(x+inset,y+row,x+w-inset,y+row+1,color);
        }
    }
    private static void meter(GuiGraphics g,int x,int y,int w,int h,float fraction,int top,int bottom){
        int right=x+Math.round(w*Mth.clamp(fraction,0,1));
        for(int row=0;row<h;row++){
            int inset=Math.max(0,4-Math.min(row,h-1-row)),from=x+inset,to=Math.min(right,x+w-inset);
            if(to<=from)continue;
            float t=row/(float)(h-1);
            int r=(int)Mth.lerp(t,(top>>16)&255,(bottom>>16)&255);
            int green=(int)Mth.lerp(t,(top>>8)&255,(bottom>>8)&255);
            int b=(int)Mth.lerp(t,top&255,bottom&255);
            g.fill(from,y+row,to,y+row+1,0xFF000000|r<<16|green<<8|b);
        }
    }
}
