package com.codex.blacklight.client.renderer.layer;

import com.codex.blacklight.client.model.BlockShieldModel;
import com.codex.blacklight.client.model.BlockArmorModel;
import com.codex.blacklight.registry.ModAttachments;
import com.codex.blacklight.power.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.*;
import yesman.epicfight.api.utils.math.*;
import yesman.epicfight.client.renderer.patched.layer.UniqueLayer;
import yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch;

public final class UtilityPowerLayer extends UniqueLayer<AbstractClientPlayer,AbstractClientPlayerPatch<AbstractClientPlayer>,PlayerModel<AbstractClientPlayer>> {
    private BlockShieldModel shield;
    @Override protected void renderLayer(AbstractClientPlayerPatch<AbstractClientPlayer> patch, AbstractClientPlayer player,
            PoseStack stack, MultiBufferSource buffer, int light, OpenMatrix4f[] poses, float bob, float yaw, float pitch, float partial) {
        if (player.isInvisible()) return;
        var armor = BlockArmorModel.get();
        var form = player.getData(ModAttachments.PLAYER_STATE).getActivePower();
        if (armor.visible(player, partial)) for (String name : armor.joints()) {
            var joint = patch.getArmature().searchJointByName(name); if (joint == null) continue;
            boolean limb = name.startsWith("Arm_") || name.startsWith("Hand_");
            boolean weaponArm = limb && (form == PowerForm.CLAWS || form == PowerForm.HAMMERFISTS || form == PowerForm.MUSCLE_MASS
                    || (form == PowerForm.BLADE || form == PowerForm.WHIPFIST) && name.endsWith("_R"));
            stack.pushPose();
            try { apply(stack, poses[joint.getId()]); armor.renderJoint(name, player, stack, buffer, light, partial, weaponArm); }
            finally { stack.popPose(); }
        }
        if (shield == null) shield = new BlockShieldModel();
        var joint = patch.getArmature().searchJointByName("Tool_L"); if (joint == null) return;
        stack.pushPose();
        try {
            apply(stack, poses[joint.getId()]);
            com.codex.blacklight.client.animation.NativeShieldGrip.apply(patch, stack, partial);
            shield.render(player, stack, buffer, light, partial);
        }
        finally { stack.popPose(); }
    }
    private static void apply(PoseStack stack, OpenMatrix4f matrix) {
        MathUtils.translateStack(stack, matrix); MathUtils.rotateStack(stack, matrix); MathUtils.scaleStack(stack, matrix);
    }
}
