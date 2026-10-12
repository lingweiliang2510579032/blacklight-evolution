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

/** Renders directly on Blacklight's animated shoulder and elbow joints; no held-item transform is involved. */
public final class BlacklightClawLayer extends net.minecraft.client.renderer.entity.layers.RenderLayer<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> {
    public BlacklightClawLayer(net.minecraft.client.renderer.entity.RenderLayerParent<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> parent){super(parent);}
    @Override public void render(PoseStack stack,MultiBufferSource buffer,int light,AbstractClientPlayer player,float swing,float amount,float partial,float age,float yaw,float pitch) {
        if(!(getParentModel() instanceof com.codex.blacklight.client.model.BlacklightPlayerModel parent))return;
        var poses=parent.matrices();if(poses==null)return;
        stack.pushPose();
        try {
            com.codex.blacklight.client.renderer.BlacklightBoneRendering.skeletonFrame(stack);
            renderSkeleton(player,stack,buffer,light,poses,age,yaw,pitch,partial);
        }finally{stack.popPose();}
    }
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            BlacklightNeoMod.MOD_ID, "textures/entity/claw_white.png");
    // Lazily baked on first render instead of at field-initialization time, so the
    // layer no longer depends on LayerDefinition registration order (Blacklight's
    // patched-renderer hook can run before RegisterLayerDefinitions on some setups).
    private ClawArmModel model;
    private final java.util.Map<com.codex.blacklight.power.PowerForm, ClawArmModel> forms = new java.util.EnumMap<>(com.codex.blacklight.power.PowerForm.class);
    private record Coil(float intensity,float lastCharged){}
    private final java.util.Map<AbstractClientPlayer,Coil> coils=new java.util.WeakHashMap<>();

    /** Camera-space arms share the exact third-person skeleton and morph clock. */
    public void renderFirstPerson(AbstractClientPlayer player,PoseStack stack,MultiBufferSource buffer,int light,org.joml.Matrix4f[] poses,float partial) {
        renderFirstPerson(player,stack,buffer,light,poses,partial,-1);
    }
    public void renderFirstPerson(AbstractClientPlayer player,PoseStack stack,MultiBufferSource buffer,int light,org.joml.Matrix4f[] poses,float partial,int side) {
        var state=com.codex.blacklight.registry.ModAttachments.getData(player, com.codex.blacklight.registry.ModAttachments.PLAYER_STATE);
        var form=state.getActivePower();
        boolean weapon=form==com.codex.blacklight.power.PowerForm.BLADE||form==com.codex.blacklight.power.PowerForm.WHIPFIST
                ||form==com.codex.blacklight.power.PowerForm.HAMMERFISTS||form==com.codex.blacklight.power.PowerForm.MUSCLE_MASS;
        float clawProgress=ClawMorphTracker.progress(player,partial);
        float progress=weapon?1:ClawTransformSettings.preview()?1:clawProgress;
        if(progress<=.001F)return;
        model=forms.computeIfAbsent(weapon?form:com.codex.blacklight.power.PowerForm.CLAWS,
                f->new ClawArmModel(f==com.codex.blacklight.power.PowerForm.CLAWS?"claw_blender":"form_"+f.id()));
        var hammer=com.codex.blacklight.client.renderer.HammerScaleRenderer.sample(player,partial);
        if(side!=1)renderHand(player,InteractionHand.MAIN_HAND,poses,false,stack,buffer,light,progress,
                form==com.codex.blacklight.power.PowerForm.HAMMERFISTS?hammer[0]:null);
        if(side!=0&&(!weapon||form==com.codex.blacklight.power.PowerForm.HAMMERFISTS||form==com.codex.blacklight.power.PowerForm.MUSCLE_MASS))
            renderHand(player,InteractionHand.OFF_HAND,poses,true,stack,buffer,light,progress,
                    form==com.codex.blacklight.power.PowerForm.HAMMERFISTS?hammer[1]:null);
    }

    private void renderSkeleton(AbstractClientPlayer player,
                               PoseStack poseStack,
                               MultiBufferSource buffer,
                               int packedLight,
                               org.joml.Matrix4f[] poses,
                               float bob,
                               float yRot,
                               float xRot,
                               float partialTicks) {
        var state = com.codex.blacklight.registry.ModAttachments.getData(player, com.codex.blacklight.registry.ModAttachments.PLAYER_STATE);
        var form = state.getActivePower();
        if(state.isPrototypeMode()) {
            int charge=player==Minecraft.getInstance().player?com.codex.blacklight.client.ClientMovementEvents.visualJumpCharge():com.codex.blacklight.registry.ModAttachments.getData(player, com.codex.blacklight.registry.ModAttachments.JUMP_CHARGE);
            float now=player.tickCount+partialTicks;
            if(charge>0)coils.put(player,new Coil(net.minecraft.util.Mth.clamp(charge/(float)com.codex.blacklight.movement.MovementTuningSettings.jumpChargeTicks(),0,1),now));
            var coil=coils.get(player);
            float intensity=coil==null?0:coil.intensity()*net.minecraft.util.Mth.clamp(1-(now-coil.lastCharged())/6,0,1);
            if(intensity>.001F){
                var out=buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
                for(String joint:new String[]{"Thigh_R","Leg_R","Thigh_L","Leg_L"}) {
                    var bone=com.codex.blacklight.animation.PlayerMotionController.skeleton().searchJointByName(joint);
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
                var bone=com.codex.blacklight.animation.PlayerMotionController.skeleton().searchJointByName(side==0?"Hand_R":"Hand_L");
                poseStack.pushPose();applyJoint(poseStack,poses[bone.getId()]);
                com.codex.blacklight.client.renderer.WeaponTrailRenderer.capture(player,poseStack,side==1);poseStack.popPose();
            }
        }
        var attack=com.codex.blacklight.registry.ModAttachments.getData(player, com.codex.blacklight.registry.ModAttachments.WEAPON_VISUAL).spec();
        if(state.isPrototypeMode()&&attack!=null&&(attack.id.contains("kick")||attack.id.contains("knee")||attack.id.contains("spin")||attack==com.codex.blacklight.combat.WeaponMove.COMBO_AXE)){
            for(int side=0;side<2;side++){
                var bone=com.codex.blacklight.animation.PlayerMotionController.skeleton().searchJointByName(side==0?"Leg_R":"Leg_L");
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
        var hammer=com.codex.blacklight.client.renderer.HammerScaleRenderer.sample(player,partialTicks);
        renderHand(player, InteractionHand.MAIN_HAND, poses, false,
                poseStack, buffer, packedLight, morphProgress,form==com.codex.blacklight.power.PowerForm.HAMMERFISTS?hammer[0]:null);
        if (weapon && form != com.codex.blacklight.power.PowerForm.HAMMERFISTS && form != com.codex.blacklight.power.PowerForm.MUSCLE_MASS) return;
        float delayedLeftProgress = ClawTransformSettings.preview() ? 1.0F
                : Math.max(0.0F, Math.min(1.0F, (morphProgress - 0.065F) / 0.935F));
        renderHand(player, InteractionHand.OFF_HAND, poses, true,
                poseStack, buffer, packedLight, delayedLeftProgress,form==com.codex.blacklight.power.PowerForm.HAMMERFISTS?hammer[1]:null);
    }

    private void renderHand(AbstractClientPlayer player,
                            InteractionHand hand,
                            org.joml.Matrix4f[] poses,
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
        com.codex.blacklight.animation.BlacklightSkeleton armature = com.codex.blacklight.animation.PlayerMotionController.skeleton();
        var handJoint = hand == InteractionHand.MAIN_HAND ? armature.handR : armature.handL;
        var armJoint = hand == InteractionHand.MAIN_HAND ? armature.armR : armature.armL;
        HandSide side = mirror ? HandSide.LEFT : HandSide.RIGHT;
        boolean claw = com.codex.blacklight.registry.ModAttachments.getData(player, com.codex.blacklight.registry.ModAttachments.PLAYER_STATE).getActivePower() == com.codex.blacklight.power.PowerForm.CLAWS;
        float width = claw ? (float) ClawTransformSettings.limbWidth(side) : 1;

        // Arm_R/L is the shoulder pivot; Hand_R/L is the actual elbow pivot.
        // Pose matrices already contain the complete animated hierarchy. Applying an
        // inverse bind-hand offset and arbitrary tuner translations detached the limb.
        // Keep both segment origins on their real joints and overlap their end rings.
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        poseStack.pushPose();
        applyJoint(poseStack, poses[armJoint.getId()]);
        poseStack.scale(width, 1.0F, width);
        BiomassTendrils.render(poseStack,consumer,packedLight,morphProgress,.34F,player.tickCount);
        model.renderJoint(mirror ? "Arm_L" : "Arm_R", poseStack, consumer, packedLight,
                OverlayTexture.NO_OVERLAY, morphProgress, 1.0F);
        poseStack.popPose();

        poseStack.pushPose();
        applyJoint(poseStack, poses[handJoint.getId()]);
        poseStack.scale(width, 1.0F, width);
        BiomassTendrils.render(poseStack,consumer,packedLight,morphProgress,.44F,player.tickCount);
        var visual=com.codex.blacklight.registry.ModAttachments.getData(player, com.codex.blacklight.registry.ModAttachments.WEAPON_VISUAL);
        if(!mirror&&visual.charging()&&visual.spec()!=null&&visual.spec().form==com.codex.blacklight.power.PowerForm.BLADE){
            float age=player.level().getGameTime()-visual.started();
            BiomassTendrils.charge(poseStack,consumer,packedLight,net.minecraft.util.Mth.clamp(.35F+age/18F,0,1),.68F,player.tickCount);
        }
        org.joml.Matrix4f wristDelta=null;
        boolean traversal=com.codex.blacklight.movement.WallPoseState.get(player)!=null||!player.onGround();
        boolean paired=com.codex.blacklight.registry.ModAttachments.getData(player, com.codex.blacklight.registry.ModAttachments.GRAB_STAGE)
                !=com.codex.blacklight.combat.ConsumeVisual.NONE
                ||com.codex.blacklight.registry.ModAttachments.getData(player, com.codex.blacklight.registry.ModAttachments.CONSUME_RECOVERY).active();
        if(claw&&(traversal||paired)){
            var wrist=armature.searchJointByName(mirror?"Tool_L":"Tool_R");
            var relative=new org.joml.Matrix4f(poses[handJoint.getId()]).invert().mul(poses[wrist.getId()]);
            wristDelta=new org.joml.Matrix4f(relative).mul(new org.joml.Matrix4f(wrist.getLocalTransform()).invert());
        }
        boolean extendingWhip=!mirror&&com.codex.blacklight.client.renderer.WhipTetherRenderer.extended(player);
        model.renderJoint(mirror ? "Hand_L" : "Hand_R", poseStack, consumer, packedLight,
                 OverlayTexture.NO_OVERLAY, morphProgress, claw ? (float) ClawTransformSettings.talonLength(side) : 1,wristDelta,extendingWhip?.32F:Float.POSITIVE_INFINITY,hammer);
        if(!mirror&&!extendingWhip&&com.codex.blacklight.registry.ModAttachments.getData(player, com.codex.blacklight.registry.ModAttachments.PLAYER_STATE).getActivePower()==com.codex.blacklight.power.PowerForm.WHIPFIST)
            model.renderJoint("Tip_R",poseStack,consumer,packedLight,OverlayTexture.NO_OVERLAY,morphProgress,1);
        if(mirror && claw && com.codex.blacklight.registry.ModAttachments.getData(player, com.codex.blacklight.registry.ModAttachments.GRAB_STAGE)
                ==com.codex.blacklight.combat.ConsumeVisual.HOLD)
            com.codex.blacklight.client.renderer.WhipTetherRenderer.captureGrip(player,poseStack);
        if(!mirror && com.codex.blacklight.registry.ModAttachments.getData(player, com.codex.blacklight.registry.ModAttachments.PLAYER_STATE).getActivePower()==com.codex.blacklight.power.PowerForm.BLADE)
            com.codex.blacklight.client.renderer.WhipTetherRenderer.captureBlade(player,poseStack);
        if (!mirror && com.codex.blacklight.registry.ModAttachments.getData(player, com.codex.blacklight.registry.ModAttachments.PLAYER_STATE).getActivePower() == com.codex.blacklight.power.PowerForm.WHIPFIST)
            com.codex.blacklight.client.renderer.WhipTetherRenderer.capture(player, poseStack);
        com.codex.blacklight.client.renderer.WeaponTrailRenderer.capture(player,poseStack,mirror);
        poseStack.popPose();
    }

    private static void applyJoint(PoseStack stack, org.joml.Matrix4f joint) {
        com.codex.blacklight.client.renderer.BlacklightBoneRendering.apply(stack,joint);
    }
}
