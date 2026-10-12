package com.codex.blacklight.client;

import com.codex.blacklight.disguise.DisguiseSystem;
import com.codex.blacklight.registry.ModAttachments;
import com.codex.blacklight.network.DisguiseTogglePayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid="blacklight",value=Dist.CLIENT)
public final class DisguiseControls {
    // R is Epic Fight's mode switch. X is free without Ctrl in the default mappings.
    public static final KeyMapping SWITCH=new KeyMapping("key.blacklight.disguise",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_X,"key.categories.blacklight");
    private static net.minecraft.client.player.LocalPlayer owner;
    private static boolean down;
    private DisguiseControls(){}
    @SubscribeEvent public static void keys(RegisterKeyMappingsEvent e){e.register(SWITCH);}
    @SubscribeEvent public static void key(InputEvent.Key e){
        if(SWITCH.matches(e.getKey(),e.getScanCode())&&(e.getAction()==GLFW.GLFW_PRESS||e.getAction()==GLFW.GLFW_RELEASE))
            press(e.getAction()==GLFW.GLFW_PRESS);
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post e){
        var mc=Minecraft.getInstance();if(mc.getWindow()==null)return;
        if(owner!=mc.player){owner=mc.player;down=physical();}
        press(physical());
        while(SWITCH.consumeClick()){}
    }
    private static boolean physical(){
        var mc=Minecraft.getInstance();if(mc.getWindow()==null)return false;
        var key=SWITCH.getKey();long window=mc.getWindow().getWindow();
        return key.getType()==InputConstants.Type.MOUSE?GLFW.glfwGetMouseButton(window,key.getValue())==GLFW.GLFW_PRESS:
                key.getValue()>=0&&GLFW.glfwGetKey(window,key.getValue())==GLFW.GLFW_PRESS;
    }
    private static void press(boolean current){
        var mc=Minecraft.getInstance();boolean edge=current&&!down;down=current;
        if(!edge||mc.player==null||mc.getConnection()==null||mc.screen!=null||!mc.isWindowActive()
                ||mc.getCameraEntity()!=mc.player||!com.codex.blacklight.mode.PrototypeModeSystem.isActive(mc.player))return;
        // Creative toolbar loading uses Ctrl+X; keep that modified shortcut intact.
        if(net.minecraft.client.gui.screens.Screen.hasControlDown())return;
        PacketDistributor.sendToServer(new DisguiseTogglePayload());
    }
    @SubscribeEvent public static void hand(RenderHandEvent e){
        var p=Minecraft.getInstance().player;
        if(p!=null&&DisguiseSystem.active(p))e.setCanceled(true);
    }
    @SubscribeEvent public static void hud(RenderGuiEvent.Post e){
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.screen!=null||mc.options.hideGui||!com.codex.blacklight.mode.PrototypeModeSystem.isActive(mc.player))return;
        var id=mc.player.getData(ModAttachments.DISGUISE_IDENTITY);if(!id.valid())return;
        Component text=Component.translatable(DisguiseSystem.active(mc.player)?"hud.blacklight.disguise_active":"hud.blacklight.disguise_ready",
                id.entityType().getDescription(),SWITCH.getTranslatedKeyMessage());
        int x=Math.max(10,e.getGuiGraphics().guiWidth()-mc.font.width(text)-10);
        e.getGuiGraphics().drawString(mc.font,text,x,12,DisguiseSystem.active(mc.player)?0xFFCBD9C0:0xFFDCC9C6,true);
    }
}
