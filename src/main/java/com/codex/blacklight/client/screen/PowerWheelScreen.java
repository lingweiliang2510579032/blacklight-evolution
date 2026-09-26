package com.codex.blacklight.client.screen;

import com.codex.blacklight.power.*;
import com.codex.blacklight.registry.ModAttachments;
import com.codex.blacklight.network.PowerSelectPayload;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Hold the bound key, point at an unlocked sector, release to select. */
public final class PowerWheelScreen extends Screen {
    private final KeyMapping binding;
    private int selected=-1;
    private boolean submitted,utility;
    public PowerWheelScreen(KeyMapping binding){super(Component.translatable("screen.blacklight.wheel"));this.binding=binding;}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void tick(){
        if(minecraft.player==null||!minecraft.player.isAlive()||!minecraft.player.getData(ModAttachments.PLAYER_STATE).isPrototypeMode()||minecraft.player.getData(ModAttachments.CONSUMING))onClose();
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
        if(minecraft.player==null)return;
        var state=minecraft.player.getData(ModAttachments.PLAYER_STATE);
        int cx=width/2,cy=height/2;
        float outer=Math.min(125,Math.min(width*.34F,height*.36F)),inner=outer*.39F;
        selected=PowerWheel.sector(mouseX-cx,mouseY-cy,inner,outer+12,utility);
        g.fill(0,0,width,height,0x6506090d);g.flush();
        RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.disableCull();RenderSystem.setShader(GameRenderer::getPositionColorShader);
        var mesh=Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES,DefaultVertexFormat.POSITION_COLOR);
        var matrix=g.pose().last().pose();
        for(int slot=0;slot<PowerWheel.size(utility);slot++) {
            var form=PowerWheel.form(utility,slot);boolean owned=form==PowerForm.NONE||state.hasUnlocked(form);
            int color=!owned?0xCE191D24:slot==selected?0xEF932D39:0xDC252B33;
            double middle=-Math.PI/2+slot*Math.PI*2/PowerWheel.size(utility),span=Math.PI/PowerWheel.size(utility)-.035;
            for(int j=0;j<18;j++) {
                double a=middle-span+2*span*j/18,b=middle-span+2*span*(j+1)/18;
                float ax=(float)Math.cos(a),ay=(float)Math.sin(a),bx=(float)Math.cos(b),by=(float)Math.sin(b);
                vertex(mesh,matrix,cx+ax*inner,cy+ay*inner,color);vertex(mesh,matrix,cx+ax*outer,cy+ay*outer,color);vertex(mesh,matrix,cx+bx*outer,cy+by*outer,color);
                vertex(mesh,matrix,cx+ax*inner,cy+ay*inner,color);vertex(mesh,matrix,cx+bx*outer,cy+by*outer,color);vertex(mesh,matrix,cx+bx*inner,cy+by*inner,color);
            }
        }
        BufferUploader.drawWithShader(mesh.buildOrThrow());RenderSystem.enableCull();RenderSystem.disableBlend();
        for(int slot=0;slot<PowerWheel.size(utility);slot++) {
            var form=PowerWheel.form(utility,slot);boolean owned=form==PowerForm.NONE||state.hasUnlocked(form);
            double angle=-Math.PI/2+slot*Math.PI*2/PowerWheel.size(utility);
            int x=cx+(int)(Math.cos(angle)*outer*.70),y=cy+(int)(Math.sin(angle)*outer*.70);
            g.drawCenteredString(font,Component.translatable("power.blacklight."+form.id()),x,y-8,owned?0xFFF0F0F0:0xFF737A85);
            if(!owned)g.drawCenteredString(font,Component.translatable("screen.blacklight.wheel.locked",form.epCost()),x,y+4,0xFFAA747A);
            else if(form==state.getActivePower()||UtilityPowerSystem.active(minecraft.player,form))g.drawCenteredString(font,Component.translatable("screen.blacklight.wheel.active"),x,y+4,0xFFFF9F83);
        }
        g.drawCenteredString(font,selected<0?Component.translatable("screen.blacklight.wheel.cancel"):Component.translatable("power.blacklight."+PowerWheel.form(utility,selected).id()),cx,cy-4,0xFFFFFFFF);
        g.drawCenteredString(font,title,cx,Math.max(8,cy-(int)outer-28),0xFFFF8790);
        g.drawCenteredString(font,Component.translatable(utility?"screen.blacklight.wheel.utility_page":"screen.blacklight.wheel.weapon_page"),cx,Math.max(20,cy-(int)outer-14),0xFFCCD1D8);
        Component hint=selected<0?Component.translatable("screen.blacklight.wheel.hint",binding.getTranslatedKeyMessage()):Component.translatable("screen.blacklight.wheel."+PowerWheel.form(utility,selected).id());
        int lineY=cy+(int)outer+12;
        for(var line:font.split(hint,width-24)){g.drawCenteredString(font,line,cx,lineY,0xFFCCD1D8);lineY+=11;}
    }
    private static void vertex(BufferBuilder b,org.joml.Matrix4f m,float x,float y,int color){b.addVertex(m,x,y,0).setColor(color);}
    private void confirm(){
        if(!submitted&&selected>=0&&minecraft.player!=null){
            var form=PowerWheel.form(utility,selected);var state=minecraft.player.getData(ModAttachments.PLAYER_STATE);
            if(form==PowerForm.NONE||state.hasUnlocked(form)){submitted=true;PacketDistributor.sendToServer(new PowerSelectPayload(form.ordinal()));}
        }
        onClose();
    }
    @Override public boolean keyPressed(int key,int scan,int mods){if(key==org.lwjgl.glfw.GLFW.GLFW_KEY_TAB){utility=!utility;selected=-1;return true;}return super.keyPressed(key,scan,mods);}
    @Override public boolean keyReleased(int key,int scan,int mods){if(binding.matches(key,scan)){confirm();return true;}return super.keyReleased(key,scan,mods);}
    @Override public boolean mouseClicked(double x,double y,int button){if(button==0){confirm();return true;}if(button==1){onClose();return true;}return super.mouseClicked(x,y,button);}
}
