package com.codex.blacklight.client.renderer;

import com.codex.blacklight.client.renderer.layer.BlacklightClawLayer;
import com.codex.blacklight.client.tuning.ClawTransformSettings;
import com.codex.blacklight.power.PowerForm;
import com.codex.blacklight.registry.ModAttachments;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.EntityType;
import yesman.epicfight.client.mesh.HumanoidMesh;
import yesman.epicfight.client.renderer.patched.entity.PPlayerRenderer;
import yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch;

/** Epic Fight player renderer that replaces the biological limbs at model-preparation time. */
public final class BlacklightPlayerRenderer extends PPlayerRenderer {
    public BlacklightPlayerRenderer(EntityRendererProvider.Context context, EntityType<?> entityType) {
        super(context, entityType);
        addCustomLayer(new BlacklightClawLayer());
        addCustomLayer(new com.codex.blacklight.client.renderer.layer.UtilityPowerLayer());
    }

    @Override
    public void mulPoseStack(com.mojang.blaze3d.vertex.PoseStack stack,
                             yesman.epicfight.api.model.Armature armature,
                             AbstractClientPlayer player,
                             AbstractClientPlayerPatch<AbstractClientPlayer> patch, float partial) {
        int stage=player.getData(ModAttachments.GRAB_STAGE);
        var weapon=player.getData(ModAttachments.WEAPON_VISUAL);
        Float movementFacing=com.codex.blacklight.client.PrototypeAnimationController.movementFacing(player,partial);
        boolean bladeContact=player.getData(ModAttachments.PLAYER_STATE).getActivePower()==PowerForm.BLADE && stage!=com.codex.blacklight.combat.ConsumeVisual.NONE;
        if(stage==com.codex.blacklight.combat.ConsumeVisual.NONE && !bladeContact && weapon.spec()==null && movementFacing==null) {
            super.mulPoseStack(stack,armature,player,patch,partial);
            return;
        }
        // Held targets turn with the player's view. Epic Fight's independent body
        // yaw can otherwise leave the hand pointing away from the restrained mob.
        float oldYaw=com.codex.blacklight.client.animation.NativeConsumePose.facing(player,weapon.spec()==null?player.yRotO:weapon.yaw());
        float yaw=com.codex.blacklight.client.animation.NativeConsumePose.facing(player,weapon.spec()==null?player.getYRot():weapon.yaw());
        if(stage==com.codex.blacklight.combat.ConsumeVisual.NONE&&weapon.spec()==null&&movementFacing!=null)
            oldYaw=yaw=movementFacing;
        var matrix=yesman.epicfight.api.utils.math.MathUtils.getModelMatrixIntegral(
                0,0,0,0,0,0,0,0,oldYaw,yaw,partial,.9375F,.9375F,.9375F);
        stack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180));
        yesman.epicfight.api.utils.math.MathUtils.translateStack(stack,matrix);
        yesman.epicfight.api.utils.math.MathUtils.rotateStack(stack,matrix);
        yesman.epicfight.api.utils.math.MathUtils.scaleStack(stack,matrix);
    }

    @Override
    protected void prepareModel(HumanoidMesh mesh,
                                AbstractClientPlayer player,
                                AbstractClientPlayerPatch<AbstractClientPlayer> entityPatch,
                                PlayerRenderer renderer) {
        super.prepareModel(mesh, player, entityPatch, renderer);
        var state = player.getData(ModAttachments.PLAYER_STATE);
        var form = state.getActivePower();
        boolean weapon = state.isPrototypeMode() && ((form == PowerForm.HAMMERFISTS || form == PowerForm.MUSCLE_MASS) || form == PowerForm.BLADE || form == PowerForm.WHIPFIST);
        if (weapon) {
            mesh.rightArm.setHidden(true); mesh.rightSleeve.setHidden(true);
            if ((form == PowerForm.HAMMERFISTS || form == PowerForm.MUSCLE_MASS)) { mesh.leftArm.setHidden(true); mesh.leftSleeve.setHidden(true); }
            return;
        }
        float morphProgress = ClawTransformSettings.preview()
                ? 1.0F : ClawMorphTracker.progress(player, 0.0F);
        if (morphProgress < 0.72F) return;

        // Hide both the base skin limbs and their second-layer sleeves. This happens after
        // PPlayerRenderer has copied vanilla visibility, so Epic Fight cannot overwrite it.
        mesh.leftArm.setHidden(true);
        mesh.rightArm.setHidden(true);
        mesh.leftSleeve.setHidden(true);
        mesh.rightSleeve.setHidden(true);
    }
}
