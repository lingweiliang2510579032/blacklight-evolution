package com.codex.blacklight.client.renderer.layer;

import com.codex.blacklight.BlacklightNeoMod;
import com.codex.blacklight.client.model.ClawArmModel;
import com.codex.blacklight.client.renderer.ClawMorphTracker;
import com.codex.blacklight.client.tuning.ClawTransformSettings;
import com.codex.blacklight.client.tuning.ClawTransformSettings.HandSide;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.client.renderer.patched.layer.UniqueLayer;
import yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.model.armature.HumanoidArmature;

/** Renders directly on Epic Fight's animated shoulder and elbow joints; no held-item transform is involved. */
public final class BlacklightClawLayer extends UniqueLayer<
        AbstractClientPlayer,
        AbstractClientPlayerPatch<AbstractClientPlayer>,
        PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            BlacklightNeoMod.MOD_ID, "textures/entity/claw_white.png");
    // Lazily baked on first render instead of at field-initialization time, so the
    // layer no longer depends on LayerDefinition registration order (Epic Fight's
    // patched-renderer hook can run before RegisterLayerDefinitions on some setups).
    private ClawArmModel model;

    @Override
    protected void renderLayer(AbstractClientPlayerPatch<AbstractClientPlayer> entityPatch,
                               AbstractClientPlayer player,
                               PoseStack poseStack,
                               MultiBufferSource buffer,
                               int packedLight,
                               OpenMatrix4f[] poses,
                               float bob,
                               float yRot,
                               float xRot,
                               float partialTicks) {
        float morphProgress = ClawTransformSettings.preview()
                ? 1.0F : ClawMorphTracker.progress(player, partialTicks);
        if (morphProgress <= 0.001F) return;

        renderHand(entityPatch, InteractionHand.MAIN_HAND, poses, false,
                poseStack, buffer, packedLight, morphProgress);
        float delayedLeftProgress = ClawTransformSettings.preview() ? 1.0F
                : Math.max(0.0F, Math.min(1.0F, (morphProgress - 0.065F) / 0.935F));
        renderHand(entityPatch, InteractionHand.OFF_HAND, poses, true,
                poseStack, buffer, packedLight, delayedLeftProgress);
    }

    private void renderHand(AbstractClientPlayerPatch<AbstractClientPlayer> entityPatch,
                            InteractionHand hand,
                            OpenMatrix4f[] poses,
                            boolean mirror,
                            PoseStack poseStack,
                            MultiBufferSource buffer,
                            int packedLight,
                            float morphProgress) {
        if (morphProgress <= 0.001F) return;
        if (model == null) {
            model = new ClawArmModel(
                    Minecraft.getInstance().getEntityModels().bakeLayer(ClawArmModel.LAYER));
        }
        HumanoidArmature armature = Armatures.BIPED.get();
        Joint handJoint = hand == InteractionHand.MAIN_HAND ? armature.handR : armature.handL;
        Joint armJoint = hand == InteractionHand.MAIN_HAND ? armature.armR : armature.armL;
        HandSide side = mirror ? HandSide.LEFT : HandSide.RIGHT;
        float width = (float) ClawTransformSettings.limbWidth(side);

        // Arm_R/L is the shoulder pivot; Hand_R/L is the actual elbow pivot.
        // Pose matrices already contain the complete animated hierarchy. Applying an
        // inverse bind-hand offset and arbitrary tuner translations detached the limb.
        // Keep both segment origins on their real joints and overlap their end rings.
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        poseStack.pushPose();
        applyJoint(poseStack, poses[armJoint.getId()]);
        poseStack.scale(width, 1.0F, width);
        model.renderJoint(mirror ? "Arm_L" : "Arm_R", poseStack, consumer, packedLight,
                OverlayTexture.NO_OVERLAY, morphProgress, 1.0F);
        poseStack.popPose();

        poseStack.pushPose();
        applyJoint(poseStack, poses[handJoint.getId()]);
        poseStack.scale(width, 1.0F, width);
        model.renderJoint(mirror ? "Hand_L" : "Hand_R", poseStack, consumer, packedLight,
                OverlayTexture.NO_OVERLAY, morphProgress, (float) ClawTransformSettings.talonLength(side));
        poseStack.popPose();
    }

    private static void applyJoint(PoseStack stack, OpenMatrix4f joint) {
        // These helpers update both position and normal matrices. The 21.17.3.1
        // mulStack helper leaves its normal buffer stale, flattening mesh lighting.
        MathUtils.translateStack(stack, joint);
        MathUtils.rotateStack(stack, joint);
        MathUtils.scaleStack(stack, joint);
    }
}
