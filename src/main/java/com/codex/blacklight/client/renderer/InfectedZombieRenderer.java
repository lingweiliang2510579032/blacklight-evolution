package com.codex.blacklight.client.renderer;

import com.codex.blacklight.client.renderer.layer.InfectedZombieLayer;
import com.codex.blacklight.infection.BlacklightInfectionSystem;
import com.codex.blacklight.infection.InfectionTimeline;
import com.codex.blacklight.registry.ModAttachments;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.client.mesh.HumanoidMesh;
import yesman.epicfight.client.renderer.patched.entity.PHumanoidRenderer;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.world.capabilities.entitypatch.mob.ZombiePatch;

public final class InfectedZombieRenderer extends PHumanoidRenderer<Zombie,ZombiePatch<Zombie>,ZombieModel<Zombie>,ZombieRenderer,HumanoidMesh> {
    public InfectedZombieRenderer(EntityRendererProvider.Context context, EntityType<?> type) {
        super(Meshes.BIPED_OLD_TEX,context,type);addCustomLayer(new InfectedZombieLayer());
    }
    public static float progress(Zombie zombie,float partial) {
        return InfectionTimeline.progress(zombie.getData(ModAttachments.INFECTION_START),zombie.level().getGameTime(),partial);
    }
    @Override protected void prepareModel(HumanoidMesh mesh,Zombie zombie,ZombiePatch<Zombie> patch,ZombieRenderer renderer) {
        super.prepareModel(mesh,zombie,patch,renderer);
        if(BlacklightInfectionSystem.isInfected(zombie) && InfectionTimeline.growth("Hand_R",progress(zombie,0))>.88F)
            mesh.rightArm.setHidden(true);
    }
    @Override public void setJointTransforms(ZombiePatch<Zombie> patch,Armature armature,Pose pose,float partial) {
        super.setJointTransforms(patch,armature,pose,partial);
        Zombie z=patch.getOriginal();if(!BlacklightInfectionSystem.isInfected(z))return;
        float t=progress(z,partial),spasm=InfectionTimeline.spasm(t);
        float strain=(float)Math.sin(Math.PI*t);
        // Additive joint offsets retain walking, hurt and death animations from Epic Fight.
        if(z.isAlive()) {
            // The heavy arm hangs under its own weight instead of preserving the
            // vanilla zombie's horizontal "tray" pose. Never overwrite attacks/hurt.
            if(!patch.getEntityState().inaction() && !patch.getEntityState().attacking() && !patch.getEntityState().hurt()) {
                var source=com.codex.blacklight.integration.epicfight.BlacklightEpicFightAnimations.CLAW_IDLE;
                if(source!=null) {
                    var stance=source.get().getRawPose(((z.tickCount+partial)/20F)%1.6F);
                    float blend=InfectionTimeline.stage(t,.1F,.8F);
                    for(String joint:new String[]{"Shoulder_R","Arm_R","Hand_R","Tool_R","Shoulder_L","Arm_L","Hand_L","Tool_L"})
                        if(stance.hasTransform(joint))pose.orElseEmpty(joint).rotation().slerp(stance.get(joint).rotation(),blend);
                }
            }
            pose.orElseEmpty("Chest").rotation().rotateX(-strain*.18F).rotateZ(spasm);
            pose.orElseEmpty("Head").rotation().rotateX(strain*.24F).rotateZ(-spasm*1.3F);
            pose.orElseEmpty("Arm_R").rotation().rotateZ(strain*.25F+spasm);
            pose.orElseEmpty("Hand_R").rotation().rotateX(spasm*2);
        }
    }
}
