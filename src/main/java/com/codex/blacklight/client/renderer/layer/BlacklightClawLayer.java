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
    private final java.util.Map<com.codex.blacklight.power.PowerForm, ClawArmModel> forms = new java.util.EnumMap<>(com.codex.blacklight.power.PowerForm.class);
    private record Coil(float intensity,float lastCharged){}
    private final java.util.Map<AbstractClientPlayer,Coil> coils=new java.util.WeakHashMap<>();

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
        var state = player.getData(com.codex.blacklight.registry.ModAttachments.PLAYER_STATE);
        var form = state.getActivePower();
        if(state.isPrototypeMode()) {
            int charge=player==Minecraft.getInstance().player?com.codex.blacklight.client.ClientMovementEvents.visualJumpCharge():player.getData(com.codex.blacklight.registry.ModAttachments.JUMP_CHARGE);
            float now=player.tickCount+partialTicks;
            if(charge>0)coils.put(player,new Coil(Math.clamp(charge/(float)com.codex.blacklight.movement.MovementTuningSettings.jumpChargeTicks(),0,1),now));
            var coil=coils.get(player);
            float intensity=coil==null?0:coil.intensity()*Math.clamp(1-(now-coil.lastCharged())/6,0,1);
            if(intensity>.001F){
                var out=buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
                for(String joint:new String[]{"Thigh_R","Leg_R","Thigh_L","Leg_L"}) {
                    var bone=Armatures.BIPED.get().searchJointByName(joint);
                    poseStack.pushPose();applyJoint(poseStack,poses[bone.getId()]);
                    BiomassTendrils.charge(poseStack,out,packedLight,intensity,.43F,player.tickCount+partialTicks);
                    poseStack.popPose();
                }
            }else coils.remove(player);
        }
        boolean weapon = state.isPrototypeMode() && ((form == com.codex.blacklight.power.PowerForm.HAMMERFISTS || form == com.codex.blacklight.power.PowerForm.MUSCLE_MASS)
                || form == com.codex.blacklight.power.PowerForm.BLADE || form == com.codex.blacklight.power.PowerForm.WHIPFIST);
        if(state.isPrototypeMode()&&form==com.codex.blacklight.power.PowerForm.NONE){
            for(int side=0;side<2;side++){
                var bone=Armatures.BIPED.get().searchJointByName(side==0?"Hand_R":"Hand_L");
                poseStack.pushPose();applyJoint(poseStack,poses[bone.getId()]);
                com.codex.blacklight.client.renderer.WeaponTrailRenderer.capture(player,poseStack,side==1);poseStack.popPose();
            }
        }
        var attack=player.getData(com.codex.blacklight.registry.ModAttachments.WEAPON_VISUAL).spec();
        if(state.isPrototypeMode()&&attack!=null&&(attack.id.contains("kick")||attack.id.contains("knee")||attack.id.contains("spin")||attack==com.codex.blacklight.combat.WeaponMove.COMBO_AXE)){
            for(int side=0;side<2;side++){
                var bone=Armatures.BIPED.get().searchJointByName(side==0?"Leg_R":"Leg_L");
                poseStack.pushPose();applyJoint(poseStack,poses[bone.getId()]);
                com.codex.blacklight.client.renderer.WeaponTrailRenderer.captureLocal(player,poseStack,side+2,.23F,.56F);poseStack.popPose();
            }
        }
        // Keep the claw transition clock current even while another weapon is visible.
        float clawProgress = ClawMorphTracker.progress(player, partialTicks);
        float morphProgress = weapon ? 1 : ClawTransformSettings.preview() ? 1 : clawProgress;
        if (morphProgress <= 0.001F) return;

        if (weapon) model = forms.computeIfAbsent(form, f -> new ClawArmModel("form_" + f.id()));
        else model = forms.computeIfAbsent(com.codex.blacklight.power.PowerForm.CLAWS, f -> new ClawArmModel("claw_blender"));
        var hammer=com.codex.blacklight.client.renderer.HammerScaleRenderer.sample(entityPatch,partialTicks);
        renderHand(entityPatch, InteractionHand.MAIN_HAND, poses, false,
                poseStack, buffer, packedLight, morphProgress,form==com.codex.blacklight.power.PowerForm.HAMMERFISTS?hammer[0]:null);
        if (weapon && form != com.codex.blacklight.power.PowerForm.HAMMERFISTS && form != com.codex.blacklight.power.PowerForm.MUSCLE_MASS) return;
        float delayedLeftProgress = ClawTransformSettings.preview() ? 1.0F
                : Math.max(0.0F, Math.min(1.0F, (morphProgress - 0.065F) / 0.935F));
        renderHand(entityPatch, InteractionHand.OFF_HAND, poses, true,
                poseStack, buffer, packedLight, delayedLeftProgress,form==com.codex.blacklight.power.PowerForm.HAMMERFISTS?hammer[1]:null);
    }

    private void renderHand(AbstractClientPlayerPatch<AbstractClientPlayer> entityPatch,
                            InteractionHand hand,
                            OpenMatrix4f[] poses,
                            boolean mirror,
                            PoseStack poseStack,
                            MultiBufferSource buffer,
                            int packedLight,
                            float morphProgress,com.codex.blacklight.combat.HammerScales.Shape hammer) {
        if (morphProgress <= 0.001F) return;
        if (model == null) {
            model = new ClawArmModel(
                    Minecraft.getInstance().getEntityModels().bakeLayer(ClawArmModel.LAYER));
        }
        HumanoidArmature armature = Armatures.BIPED.get();
        Joint handJoint = hand == InteractionHand.MAIN_HAND ? armature.handR : armature.handL;
        Joint armJoint = hand == InteractionHand.MAIN_HAND ? armature.armR : armature.armL;
        HandSide side = mirror ? HandSide.LEFT : HandSide.RIGHT;
        boolean claw = entityPatch.getOriginal().getData(com.codex.blacklight.registry.ModAttachments.PLAYER_STATE).getActivePower() == com.codex.blacklight.power.PowerForm.CLAWS;
        float width = claw ? (float) ClawTransformSettings.limbWidth(side) : 1;

        // Arm_R/L is the shoulder pivot; Hand_R/L is the actual elbow pivot.
        // Pose matrices already contain the complete animated hierarchy. Applying an
        // inverse bind-hand offset and arbitrary tuner translations detached the limb.
        // Keep both segment origins on their real joints and overlap their end rings.
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        poseStack.pushPose();
        applyJoint(poseStack, poses[armJoint.getId()]);
        poseStack.scale(width, 1.0F, width);
        BiomassTendrils.render(poseStack,consumer,packedLight,morphProgress,.34F,entityPatch.getOriginal().tickCount);
        model.renderJoint(mirror ? "Arm_L" : "Arm_R", poseStack, consumer, packedLight,
                OverlayTexture.NO_OVERLAY, morphProgress, 1.0F);
        poseStack.popPose();

        poseStack.pushPose();
        applyJoint(poseStack, poses[handJoint.getId()]);
        poseStack.scale(width, 1.0F, width);
        BiomassTendrils.render(poseStack,consumer,packedLight,morphProgress,.44F,entityPatch.getOriginal().tickCount);
        var visual=entityPatch.getOriginal().getData(com.codex.blacklight.registry.ModAttachments.WEAPON_VISUAL);
        if(!mirror&&visual.charging()&&visual.spec()!=null&&visual.spec().form==com.codex.blacklight.power.PowerForm.BLADE){
            float age=entityPatch.getOriginal().level().getGameTime()-visual.started();
            BiomassTendrils.charge(poseStack,consumer,packedLight,Math.clamp(.35F+age/18F,0,1),.68F,entityPatch.getOriginal().tickCount);
        }
        OpenMatrix4f wristDelta=null;
        var bodyPlayer=entityPatch.getAnimator().getPlayerFor(com.codex.blacklight.integration.epicfight.BlacklightEpicFightAnimations.CLAW_IDLE);
        var bodyMotion=bodyPlayer==null?null:bodyPlayer.getRealAnimation();
        boolean traversal=bodyMotion!=null&&(bodyMotion.get() instanceof com.codex.blacklight.integration.epicfight.NativeTraversalMovement
                ||bodyMotion.get() instanceof com.codex.blacklight.integration.epicfight.NativeTraversalAction);
        boolean paired=entityPatch.getOriginal().getData(com.codex.blacklight.registry.ModAttachments.GRAB_STAGE)
                !=com.codex.blacklight.combat.ConsumeVisual.NONE
                ||entityPatch.getOriginal().getData(com.codex.blacklight.registry.ModAttachments.CONSUME_RECOVERY).active();
        if(claw&&(traversal||paired)){
            Joint wrist=armature.searchJointByName(mirror?"Tool_L":"Tool_R");
            var relative=OpenMatrix4f.mul(OpenMatrix4f.invert(poses[handJoint.getId()],null),poses[wrist.getId()],null);
            wristDelta=OpenMatrix4f.mul(relative,OpenMatrix4f.invert(wrist.getLocalTransform(),null),null);
        }
        boolean extendingWhip=!mirror&&com.codex.blacklight.client.renderer.WhipTetherRenderer.extended(entityPatch.getOriginal());
        model.renderJoint(mirror ? "Hand_L" : "Hand_R", poseStack, consumer, packedLight,
                 OverlayTexture.NO_OVERLAY, morphProgress, claw ? (float) ClawTransformSettings.talonLength(side) : 1,wristDelta,extendingWhip?.32F:Float.POSITIVE_INFINITY,hammer);
        if(!mirror&&!extendingWhip&&entityPatch.getOriginal().getData(com.codex.blacklight.registry.ModAttachments.PLAYER_STATE).getActivePower()==com.codex.blacklight.power.PowerForm.WHIPFIST)
            model.renderJoint("Tip_R",poseStack,consumer,packedLight,OverlayTexture.NO_OVERLAY,morphProgress,1);
        if(mirror && claw && entityPatch.getOriginal().getData(com.codex.blacklight.registry.ModAttachments.GRAB_STAGE)
                ==com.codex.blacklight.combat.ConsumeVisual.HOLD)
            com.codex.blacklight.client.renderer.WhipTetherRenderer.captureGrip(entityPatch.getOriginal(),poseStack);
        if(!mirror && entityPatch.getOriginal().getData(com.codex.blacklight.registry.ModAttachments.PLAYER_STATE).getActivePower()==com.codex.blacklight.power.PowerForm.BLADE)
            com.codex.blacklight.client.renderer.WhipTetherRenderer.captureBlade(entityPatch.getOriginal(),poseStack);
        if (!mirror && entityPatch.getOriginal().getData(com.codex.blacklight.registry.ModAttachments.PLAYER_STATE).getActivePower() == com.codex.blacklight.power.PowerForm.WHIPFIST)
            com.codex.blacklight.client.renderer.WhipTetherRenderer.capture(entityPatch.getOriginal(), poseStack);
        com.codex.blacklight.client.renderer.WeaponTrailRenderer.capture(entityPatch.getOriginal(),poseStack,mirror);
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
