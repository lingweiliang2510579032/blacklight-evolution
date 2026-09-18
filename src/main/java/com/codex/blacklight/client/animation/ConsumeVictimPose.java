package com.codex.blacklight.client.animation;

import com.codex.blacklight.combat.ConsumeReaction;
import com.codex.blacklight.mixin.client.AgeableModelAccessor;
import com.codex.blacklight.power.PowerForm;
import com.codex.blacklight.registry.ModAttachments;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.*;
import net.minecraft.world.entity.LivingEntity;
import yesman.epicfight.api.animation.Pose;
import java.util.*;

/** Same authored reaction drives vanilla joints, Epic Fight joints, and the Hunter's Gecko rig. */
public final class ConsumeVictimPose {
    private static final Map<EntityModel<?>,Map<ModelPart,PartPose>> SAVED=new WeakHashMap<>();
    private ConsumeVictimPose() {}
    public static ConsumeReaction.Pose reaction(LivingEntity entity,float partial) {
        var data=entity.getData(ModAttachments.CONSUME_VISUAL);
        if(!data.consuming()) {
            float time=entity.level().getGameTime()+partial;
            float strength=data.stage()==com.codex.blacklight.combat.ConsumeVisual.CAPTURE
                    ? Math.clamp((data.phaseProgress(entity.level().getGameTime(),partial)-.35F)*2,0,1):1;
            return new ConsumeReaction.Pose(.12F*strength,(float)Math.sin(time*.17F)*.13F*strength,0,
                    .8F*strength,(.5F+(float)Math.sin(time*.32F)*.18F)*strength,-.2F*strength,0);
        }
        var forms=PowerForm.values();
        return ConsumeReaction.sample(forms[Math.clamp(data.form(),0,forms.length-1)],data.progress(entity.level().getGameTime(),partial));
    }
    public static org.joml.Matrix4f bodyTransform(LivingEntity entity,float partial) {
        var p=reaction(entity,partial);float h=entity.getBbHeight(),g=p.grounded();
        var matrix=new org.joml.Matrix4f();
        var visual=entity.getData(ModAttachments.CONSUME_VISUAL);
        if(visual.stage()==com.codex.blacklight.combat.ConsumeVisual.FLIGHT) {
            float t=visual.phaseProgress(entity.level().getGameTime(),partial);
            float pitch=Math.min(1,t*8)*1.25F;
            return matrix.rotateY((float)Math.toRadians(-entity.yBodyRot)).translate(0,h*.5F,0)
                    .rotateX(pitch).rotateZ((float)Math.sin(t*12)*.18F).translate(0,-h*.5F,0)
                    .rotateY((float)Math.toRadians(entity.yBodyRot));
        }
        float half=h*.5F,groundHeight=Math.clamp(entity.getBbWidth()*.35F,.18F,.36F);
        matrix.translate(0,-p.slump()*h*.24F,0).rotateY((float)Math.toRadians(-entity.yBodyRot));
        // Center the entire victim on the ground as it rotates, instead of tipping a standing model around its feet.
        matrix.translate(0,half*(1-g)+groundHeight*g,0).rotateX(-g*(float)Math.PI/2+p.bend()*.22F)
                .rotateZ(p.twist()*.18F).translate(0,-half,0).rotateY((float)Math.toRadians(entity.yBodyRot));
        return matrix;
    }
    public static void body(LivingEntity entity,PoseStack stack,float partial) { stack.mulPose(bodyTransform(entity,partial)); }
    public static net.minecraft.world.phys.Vec3 point(LivingEntity entity,net.minecraft.world.phys.Vec3 point,float partial) {
        var v=bodyTransform(entity,partial).transformPosition(new org.joml.Vector3f((float)point.x,(float)point.y,(float)point.z));
        return new net.minecraft.world.phys.Vec3(v.x,v.y,v.z);
    }
    public static void vanilla(EntityModel<?> model,LivingEntity entity,float partial) {
        restore(model);
        if (!entity.getData(ModAttachments.CONSUME_VISUAL).active()) return;
        var p=reaction(entity,partial);
        var saved=new IdentityHashMap<ModelPart,PartPose>();SAVED.put(model,saved);
        if (model instanceof HumanoidModel<?> humanoid) {
            head(saved,humanoid.head,p);head(saved,humanoid.hat,p);
            rotate(saved,humanoid.body,p.bend()*.22F,p.twist()*.45F,0);
            limb(saved,humanoid.rightArm,-p.brace()*1.8F,.15F,-.25F-p.brace()*.22F);
            limb(saved,humanoid.leftArm,-p.brace()*1.65F,-.15F,.25F+p.brace()*.22F);
            limb(saved,humanoid.rightLeg,p.kick()*.66F,0,.12F);
            limb(saved,humanoid.leftLeg,-p.kick()*.48F,0,-.12F);
        } else if (model instanceof HierarchicalModel<?> hierarchy) {
            var root=hierarchy.root();
            if(root.hasChild("head"))head(saved,root.getChild("head"),p);
            if(root.hasChild("body"))rotate(saved,root.getChild("body"),p.bend()*.18F,p.twist()*.35F,0);
            if(root.hasChild("arms"))rotate(saved,root.getChild("arms"),-p.brace()*.75F,0,p.twist()*.4F);
            String[] legs={"right_leg","left_leg","right_front_leg","left_front_leg","right_hind_leg","left_hind_leg"};
            for(int i=0;i<legs.length;i++)if(root.hasChild(legs[i]))limb(saved,root.getChild(legs[i]),(i%2==0?1:-1)*p.kick()*.7F,0,(i%2==0?1:-1)*.15F);
        } else if (model instanceof AgeableModelAccessor ageable) {
            for(var head:ageable.blacklight$heads())head(saved,head,p);
            int index=0;
            for(var part:ageable.blacklight$body()) {
                if(index==0)rotate(saved,part,p.bend()*.13F,p.twist()*.3F,p.twist()*.18F);
                else {
                    float side=index%2==0?1:-1;
                    // Quadrupeds tuck the front legs and kick the rear legs while restrained.
                    limb(saved,part,(index<3?-.6F:1.05F)*p.kick()+side*p.twist()*.35F,0,side*p.brace()*.17F);
                }
                index++;
            }
        }
    }
    private static void head(Map<ModelPart,PartPose> saved,ModelPart part,ConsumeReaction.Pose p) {
        rotate(saved,part,p.head(),p.twist(),p.twist()*.55F);
    }
    private static void rotate(Map<ModelPart,PartPose> saved,ModelPart part,float x,float y,float z) {
        saved.putIfAbsent(part,part.storePose());part.xRot+=x;part.yRot+=y;part.zRot+=z;
    }
    private static void limb(Map<ModelPart,PartPose> saved,ModelPart part,float x,float y,float z) {
        saved.putIfAbsent(part,part.storePose());part.xRot=x;part.yRot=y;part.zRot=z;
    }
    public static void restore(EntityModel<?> model) {
        var saved=SAVED.remove(model);if(saved!=null)saved.forEach(ModelPart::loadPose);
    }
    public static void epic(LivingEntity entity,Pose pose,float partial) {
        if(!entity.getData(ModAttachments.CONSUME_VISUAL).active())return;
        var p=reaction(entity,partial);
        // Override the captured locomotion rather than adding perpetual walking or attack poses.
        pose.orElseEmpty("Torso").rotation().rotationXYZ(p.bend()*.55F,p.twist()*.4F,0);
        pose.orElseEmpty("Chest").rotation().rotationXYZ(p.bend()*.45F,p.twist()*.6F,0);
        pose.orElseEmpty("Head").rotation().rotationXYZ(-p.head()*.6F,-p.twist(),p.twist()*.5F);
        for(String side:new String[]{"R","L"}) {
            float sign=side.equals("R")?1:-1;
            pose.orElseEmpty("Arm_"+side).rotation().rotationXYZ(-p.brace()*1.25F,sign*.18F,sign*(.18F+p.brace()*.25F));
            pose.orElseEmpty("Hand_"+side).rotation().rotationXYZ(-.3F-p.brace()*.85F,0,0);
            pose.orElseEmpty("Thigh_"+side).rotation().rotationXYZ(-p.kick()*.55F+sign*p.twist()*.2F,0,sign*.12F);
            pose.orElseEmpty("Leg_"+side).rotation().rotationXYZ(p.kick()*.95F,0,0);
        }
    }
}
