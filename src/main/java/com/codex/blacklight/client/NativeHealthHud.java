package com.codex.blacklight.client;

import com.codex.blacklight.BlacklightNeoMod;
import com.codex.blacklight.combat.MotionResources;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/** Local-only source textures and source GFX layout, scaled together in GUI space. */
final class NativeHealthHud {
    private static JsonObject profile;
    private static boolean loaded;
    private static final Map<String,ResourceLocation> TEXTURES=new HashMap<>();
    private NativeHealthHud() {}
    private static boolean load(){
        if(loaded)return profile!=null;
        loaded=true;
        try {
            var data=MotionResources.read("health-hud.data",1_000_000).orElse(null);
            if(data==null)return false;
            if(data.get("schema").getAsInt()!=1)throw new IllegalArgumentException("HUD schema");
            for(var entry:data.getAsJsonObject("textures").entrySet()){
                var image=NativeImage.read(new ByteArrayInputStream(Base64.getDecoder().decode(entry.getValue().getAsJsonObject().get("png").getAsString())));
                TEXTURES.put(entry.getKey(),Minecraft.getInstance().getTextureManager().register("blacklight_hud_"+entry.getKey(),new DynamicTexture(image)));
            }
            profile=data;
        } catch(Exception error){
            TEXTURES.values().forEach(Minecraft.getInstance().getTextureManager()::release);TEXTURES.clear();
            BlacklightNeoMod.LOGGER.error("Cannot load locally installed Prototype health HUD",error);
        }
        return profile!=null;
    }
    static boolean render(GuiGraphics g,int x,int y,int width,float health,float trail,float absorption){
        if(!load())return false;
        float scale=width/profile.get("width").getAsFloat();
        g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale(scale,scale,1);
        RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
        try {
            // These fills are the original GFX rectangular shapes and color transforms.
            var bar=profile.getAsJsonArray("bar");int bx=bar.get(0).getAsInt(),by=bar.get(1).getAsInt(),bw=bar.get(2).getAsInt(),bh=bar.get(3).getAsInt();
            g.fill(bx,by,bx+bw,by+bh,0x2DFFEACF);
            texture(g,"underline","underline",.8984375F);
            texture(g,"junction","junction",.8984375F);
            int end=bx+Math.round(bw*Mth.clamp(health,0,1));
            g.fill(bx,by,bx+Math.round(bw*Mth.clamp(trail,0,1)),by+bh,color("damageColor",255));
            g.fill(bx,by,end,by+bh,color("fillColor",230));
            if(end>bx)g.fill(bx,by,end,by+1,color("edgeColor",230));
            texture(g,"heart","heart",.8984375F);
            // Minecraft absorption is separate from actual health, using the source's cyan accent.
            if(absorption>0)g.fill(bx,by+20,bx+Math.round(bw*Mth.clamp(absorption,0,1)),by+23,0xE633CCCC);
        } finally {g.flush();RenderSystem.setShaderColor(1,1,1,1);g.pose().popPose();}
        return true;
    }
    private static int color(String key,int alpha){
        var v=profile.getAsJsonArray(key);return alpha<<24|v.get(0).getAsInt()<<16|v.get(1).getAsInt()<<8|v.get(2).getAsInt();
    }
    private static void texture(GuiGraphics g,String key,String placement,float alpha){
        var rect=profile.getAsJsonArray(placement);var data=profile.getAsJsonObject("textures").getAsJsonObject(key);
        int w=data.get("width").getAsInt(),h=data.get("height").getAsInt();
        g.flush();g.pose().pushPose();g.pose().translate(rect.get(0).getAsFloat(),rect.get(1).getAsFloat(),0);
        g.pose().scale(rect.get(2).getAsFloat()/w,rect.get(3).getAsFloat()/h,1);
        // The source portrait needs its own vertical UV orientation; the bar
        // and junction already have the correct direction in GUI coordinates.
        RenderSystem.setShaderColor(1,1,1,alpha);
        boolean flip=key.equals("heart");
        g.blit(TEXTURES.get(key),0,0,w,h,0F,flip?h:0F,w,flip?-h:h,w,h);
        g.flush();RenderSystem.setShaderColor(1,1,1,1);g.pose().popPose();
    }
}
