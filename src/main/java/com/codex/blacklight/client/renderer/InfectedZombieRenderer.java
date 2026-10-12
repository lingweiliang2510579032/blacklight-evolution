package com.codex.blacklight.client.renderer;

import com.codex.blacklight.client.renderer.layer.InfectedZombieLayer;
import com.codex.blacklight.infection.BlacklightInfectionSystem;
import com.codex.blacklight.infection.InfectionTimeline;
import com.codex.blacklight.registry.ModAttachments;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.monster.Zombie;

/** Native zombie renderer retains walk, attack, baby, equipment, hurt and death rendering. */
public final class InfectedZombieRenderer extends ZombieRenderer {
    public InfectedZombieRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model=new InfectionModel(context);
        addLayer(new InfectedZombieLayer(this));
    }
    public static float progress(Zombie zombie,float partial) {
        return InfectionTimeline.progress(com.codex.blacklight.registry.ModAttachments.getData(zombie, ModAttachments.INFECTION_START),zombie.level().getGameTime(),partial);
    }
    private static final class InfectionModel extends ZombieModel<Zombie> {
        InfectionModel(EntityRendererProvider.Context context){super(context.bakeLayer(ModelLayers.ZOMBIE));}
        @Override public void setupAnim(Zombie z,float swing,float amount,float age,float yaw,float pitch) {
            super.setupAnim(z,swing,amount,age,yaw,pitch);
            rightArm.visible=true;
            if(!BlacklightInfectionSystem.isInfected(z))return;
            float partial=age-z.tickCount,t=progress(z,partial),spasm=InfectionTimeline.spasm(t),strain=(float)Math.sin(Math.PI*t);
            rightArm.visible=InfectionTimeline.growth("Hand_R",t)<=.88F;
            if(!z.isAlive())return;
            if(!z.isAggressive()&&z.hurtTime==0) {
                float weight=InfectionTimeline.stage(t,.1F,.8F);
                rightArm.xRot+=(.12F-rightArm.xRot)*weight;
                leftArm.xRot+=(.12F-leftArm.xRot)*weight;
            }
            body.xRot-=strain*.18F;body.zRot+=spasm;
            head.xRot+=strain*.24F;head.zRot-=spasm*1.3F;hat.copyFrom(head);
            rightArm.zRot+=strain*.25F+spasm;
        }
    }
}
