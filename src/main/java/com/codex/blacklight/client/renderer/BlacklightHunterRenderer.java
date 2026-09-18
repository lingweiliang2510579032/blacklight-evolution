package com.codex.blacklight.client.renderer;

import com.codex.blacklight.client.model.BlacklightHunterModel;
import com.codex.blacklight.combat.ConsumeSystem;
import com.codex.blacklight.entity.BlacklightHunter;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class BlacklightHunterRenderer extends GeoEntityRenderer<BlacklightHunter> {
    private static final ResourceLocation WHITE = ResourceLocation.fromNamespaceAndPath("blacklight", "textures/entity/claw_white.png");
    public BlacklightHunterRenderer(EntityRendererProvider.Context context) { super(context, new BlacklightHunterModel()); shadowRadius = .65F; }
    @Override public void render(BlacklightHunter entity, float yaw, float partial, PoseStack stack, MultiBufferSource buffer, int light) {
        var owner = entity.level().getEntity(entity.consumerId());
        var visual = entity.getData(com.codex.blacklight.registry.ModAttachments.CONSUME_VISUAL);
        float t = visual.active() ? visual.progress(entity.level().getGameTime(), partial) : 0;
        float pull = Math.max(0, (t - .65F) / .35F) * .35F;
        Vec3 delta = owner == null ? Vec3.ZERO : owner.getPosition(partial).subtract(entity.getPosition(partial));
        stack.pushPose();
        if (visual.active()) com.codex.blacklight.client.animation.ConsumeVictimPose.body(entity, stack, partial);
        stack.translate(delta.x * pull, .2 * pull, delta.z * pull);
        super.render(entity, yaw, partial, stack, buffer, light);
        stack.popPose();
        if (owner != null && t > 0) {
            Vec3 from = com.codex.blacklight.client.animation.ConsumeVictimPose.point(entity,new Vec3(delta.x * pull, 1.25 - .25 * pull, delta.z * pull),partial);
            Vec3 to = delta.add(delta.multiply(-1,0,-1).normalize().scale(.27)).add(0, 1.15, 0);
            float start=visual.form()==com.codex.blacklight.power.PowerForm.WHIPFIST.ordinal() ? .47F : visual.form()==com.codex.blacklight.power.PowerForm.HAMMERFISTS.ordinal() ? .38F : 0;
            if(t>start)strands(stack, buffer.getBuffer(RenderType.entityCutoutNoCull(WHITE)), light, from, to, (t-start)/(1-start), entity.level().getGameTime() + partial,
                    entity.getBbHeight(),entity.getBbWidth(),com.codex.blacklight.client.animation.ConsumeVictimPose.bodyTransform(entity,partial));
            ConsumedMobVisuals.whip(entity, stack, buffer.getBuffer(RenderType.entityCutoutNoCull(WHITE)), light, delta, t);
        }
    }
    public static void strands(PoseStack stack, VertexConsumer out, int light, Vec3 from, Vec3 to, float progress, float time) {
        strands(stack, out, light, from, to, progress, time, 2, 1);
    }
    public static void strands(PoseStack stack, VertexConsumer out, int light, Vec3 from, Vec3 to, float progress, float time, float height, float width) {
        strands(stack,out,light,from,to,progress,time,height,width,new org.joml.Matrix4f());
    }
    public static void strands(PoseStack stack, VertexConsumer out, int light, Vec3 from, Vec3 to, float progress, float time, float height, float width, org.joml.Matrix4f body) {
        Vec3 axis = to.subtract(from).normalize();
        Vec3 u = axis.cross(new Vec3(0,1,0)).normalize();
        if (u.lengthSqr() < .1) u = new Vec3(1,0,0);
        Vec3 v = axis.cross(u).normalize();
        for (int strand = 0; strand < 14; strand++) {
            int color = strand % 4 == 1 ? 0xff9b302c : 0xff292627;
            double fan = strand * 2.39996;
            var offset=body.transformDirection(new org.joml.Vector3f((float)Math.cos(fan)*width*.4F,-height*.38F+(strand%5)*height*.18F,(float)Math.sin(fan)*width*.32F));
            Vec3 origin=from.add(offset.x,offset.y,offset.z);
            double retract = Math.max(0, Math.min(1,(progress-.55)/.45));
            origin = origin.lerp(to, retract*retract*(3-2*retract));
            for (int i = 0; i < 20; i++) {
                float a = i / 20F, b = (i + 1) / 20F;
                Vec3 p = point(origin,to,u,v,a,strand,time,progress);
                Vec3 q = point(origin,to,u,v,b,strand,time,progress);
                double radius = (.017 + .022 * Math.sin(Math.PI * progress)) * Math.min(1,(1-progress)*8);
                for (int k = 0; k < 5; k++) {
                    Vec3 n = u.scale(Math.cos(k*Math.PI*2/5)).add(v.scale(Math.sin(k*Math.PI*2/5)));
                    Vec3 m = u.scale(Math.cos((k+1)*Math.PI*2/5)).add(v.scale(Math.sin((k+1)*Math.PI*2/5)));
                    vertex(out,stack.last(),p.add(n.scale(radius)),n,color,light);
                    vertex(out,stack.last(),q.add(n.scale(radius)),n,color,light);
                    vertex(out,stack.last(),q.add(m.scale(radius)),m,color,light);
                    vertex(out,stack.last(),p.add(m.scale(radius)),m,color,light);
                }
            }
        }
    }
    private static Vec3 point(Vec3 a,Vec3 b,Vec3 u,Vec3 v,float t,int strand,float time,float progress) {
        // The tendrils first reach outward, then contract toward the player's chest.
        double angle = strand * 2.39996 + t * 3 - time * .13;
        double width = (.04 + .2 * Math.sin(Math.PI * t)) * (1 - progress);
        float reach = Math.min(1, progress / .18F);
        return b.lerp(a, (1-t)*reach).add(u.scale(Math.cos(angle)*width)).add(v.scale(Math.sin(angle)*width));
    }
    private static void vertex(VertexConsumer out,PoseStack.Pose pose,Vec3 p,Vec3 n,int color,int light) {
        out.addVertex(pose,(float)p.x,(float)p.y,(float)p.z).setColor(color).setUv(.5F,.5F)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose,(float)n.x,(float)n.y,(float)n.z);
    }
}
