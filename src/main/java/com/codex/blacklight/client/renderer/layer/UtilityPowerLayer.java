package com.codex.blacklight.client.renderer.layer;

import com.codex.blacklight.client.model.ClawArmModel;
import com.codex.blacklight.power.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.api.utils.math.*;
import yesman.epicfight.client.renderer.patched.layer.UniqueLayer;
import yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch;

public final class UtilityPowerLayer extends UniqueLayer<AbstractClientPlayer,AbstractClientPlayerPatch<AbstractClientPlayer>,PlayerModel<AbstractClientPlayer>> {
    private ClawArmModel armor,shield;
    @Override protected void renderLayer(AbstractClientPlayerPatch<AbstractClientPlayer> patch,AbstractClientPlayer p,PoseStack stack,MultiBufferSource buffer,int light,OpenMatrix4f[] poses,float bob,float yaw,float pitch,float partial){
        boolean plated=UtilityPowerSystem.active(p,PowerForm.ARMOR),guard=UtilityPowerSystem.active(p,PowerForm.SHIELD);
        if((!plated&&!guard)||p.isInvisible())return;
        if(armor==null){armor=new ClawArmModel("utility_armor");shield=new ClawArmModel("utility_shield");}
        var out=buffer.getBuffer(RenderType.entityCutoutNoCull(ResourceLocation.fromNamespaceAndPath("blacklight","textures/entity/claw_white.png")));
        var names=plated?new String[]{"Torso","Chest","Head","Thigh_R","Thigh_L","Leg_R","Leg_L"}:new String[]{"Hand_L"};
        for(String name:names){var joint=patch.getArmature().searchJointByName(name);if(joint==null)continue;stack.pushPose();var matrix=poses[joint.getId()];MathUtils.translateStack(stack,matrix);MathUtils.rotateStack(stack,matrix);MathUtils.scaleStack(stack,matrix);(plated?armor:shield).renderJoint(name,stack,out,light,OverlayTexture.NO_OVERLAY,1,1);stack.popPose();}
    }
}
