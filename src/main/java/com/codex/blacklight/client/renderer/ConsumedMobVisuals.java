package com.codex.blacklight.client.renderer;

import com.codex.blacklight.BlacklightNeoMod;
import com.codex.blacklight.entity.BlacklightHunter;
import com.codex.blacklight.power.PowerForm;
import com.codex.blacklight.registry.ModAttachments;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

/** Victims retain their species model until it contracts into biomass. No replacement mob or item drop. */
@EventBusSubscriber(modid = BlacklightNeoMod.MOD_ID, value = Dist.CLIENT)
public final class ConsumedMobVisuals {
    public static final ResourceLocation WHITE = ResourceLocation.fromNamespaceAndPath("blacklight", "textures/entity/claw_white.png");
    private static final java.util.Set<LivingEntity> SCALED = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    private ConsumedMobVisuals() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void before(RenderLivingEvent.Pre<?, ?> event) {
        var mob = event.getEntity();
        if (mob instanceof BlacklightHunter) return;
        var data = mob.getData(ModAttachments.CONSUME_VISUAL);
        if (!data.active()) return;
        var owner = mob.level().getEntity(data.consumerId());
        if (owner == null) return;
        float partial = event.getPartialTick(), t = data.progress(mob.level().getGameTime(), partial);
        Vec3 delta = owner.getPosition(partial).subtract(mob.getPosition(partial));
        var stack = event.getPoseStack();
        var out = event.getMultiBufferSource().getBuffer(RenderType.entityCutoutNoCull(WHITE));
        Vec3 from = com.codex.blacklight.client.animation.ConsumeVictimPose.point(mob,new Vec3(0, mob.getBbHeight() * .52, 0),partial);
        Vec3 to = delta.add(delta.multiply(-1, 0, -1).normalize().scale(.27)).add(0, 1.15, 0);
        float start = data.form() == PowerForm.WHIPFIST.ordinal() ? .47F : data.form() == PowerForm.HAMMERFISTS.ordinal() ? .38F : .12F;
        if (t > start) BlacklightHunterRenderer.strands(stack, out, event.getPackedLight(), from, to,
                Math.clamp((t - start) / (1-start), 0, 1), mob.level().getGameTime() + partial, mob.getBbHeight(), mob.getBbWidth(),
                com.codex.blacklight.client.animation.ConsumeVictimPose.bodyTransform(mob,partial));
        whip(mob, stack, out, event.getPackedLight(), delta, tetherProgress(mob,partial));
        if (t >= .73F) { event.setCanceled(true); return; }
        stack.pushPose();
        com.codex.blacklight.client.animation.ConsumeVictimPose.body(mob, stack, partial);
        float dissolve = Math.clamp((t - .40F) / .33F, 0, 1);
        float size = 1 - dissolve * dissolve * .75F;
        stack.translate(delta.x * dissolve * .25, .12 * dissolve, delta.z * dissolve * .25);
        stack.scale(size, 1 - dissolve * .15F, size);
        var engine = yesman.epicfight.client.events.engine.RenderEngine.getInstance();
        var patch = yesman.epicfight.world.capabilities.EpicFightCapabilities.getEntityPatch(mob,
                yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch.class);
        // Epic Fight normally draws from its Pre listener and cancels the vanilla render.
        // Render that same armature once inside our transform instead of losing the dissolve.
        if (patch != null && patch.overrideRender() && engine.hasRendererFor(mob)) {
            event.setCanceled(true);
            try {
                engine.renderEntityArmatureModel(mob, patch, event.getRenderer(), event.getMultiBufferSource(),
                        stack, event.getPackedLight(), partial);
            } finally { stack.popPose(); }
        } else SCALED.add(mob);
    }
    @SubscribeEvent
    public static void after(RenderLivingEvent.Post<?, ?> event) {
        if (SCALED.remove(event.getEntity())) event.getPoseStack().popPose();
    }

    public static float tetherProgress(LivingEntity mob,float partial) {
        var data=mob.getData(ModAttachments.CONSUME_VISUAL);
        if(data.stage()==com.codex.blacklight.combat.ConsumeVisual.CAPTURE)
            return (mob.level().getGameTime()-data.started()+partial)/60F;
        if(data.stage()==com.codex.blacklight.combat.ConsumeVisual.HOLD)return .44F;
        if(data.consuming())return .44F+data.progress(mob.level().getGameTime(),partial)*.5F;
        return 1;
    }
    /** A thick, continuous cable and tightening coil distinguish whip capture from generic tendrils. */
    public static void whip(LivingEntity mob, PoseStack stack, VertexConsumer out, int light, Vec3 delta, float t) {
        if (mob.getData(ModAttachments.CONSUME_VISUAL).form() != PowerForm.WHIPFIST.ordinal() || t > .8F) return;
        if (t < .185F) return;
        double radius = mob.getBbWidth() * .58 * (1-Math.clamp((t-.5)/.3, 0, 1));
        for (int i = 0; i < 48; i++) {
            double a = i / 48.0, b = (i+1) / 48.0;
            Vec3 p = new Vec3(Math.cos(a*Math.PI*5)*radius, mob.getBbHeight()*(.22+.6*a), Math.sin(a*Math.PI*5)*radius);
            Vec3 q = new Vec3(Math.cos(b*Math.PI*5)*radius, mob.getBbHeight()*(.22+.6*b), Math.sin(b*Math.PI*5)*radius);
            tube(stack, out, light, p, q, .045, i%4==0 ? 0xff833b32 : 0xff35292a);
        }
    }
    public static void tube(PoseStack stack, VertexConsumer out, int light, Vec3 a, Vec3 b, double radius, int color) {
        Vec3 axis = b.subtract(a).normalize(), u = axis.cross(new Vec3(0,1,0)).normalize();
        if (u.lengthSqr() < .1) u = new Vec3(1,0,0);
        Vec3 v = axis.cross(u).normalize();
        for (int i=0; i<6; i++) {
            Vec3 n=u.scale(Math.cos(i*Math.PI/3)).add(v.scale(Math.sin(i*Math.PI/3)));
            Vec3 m=u.scale(Math.cos((i+1)*Math.PI/3)).add(v.scale(Math.sin((i+1)*Math.PI/3)));
            vertex(stack,out,light,a.add(n.scale(radius)),n,color); vertex(stack,out,light,b.add(n.scale(radius)),n,color);
            vertex(stack,out,light,b.add(m.scale(radius)),m,color); vertex(stack,out,light,a.add(m.scale(radius)),m,color);
        }
    }
    private static void vertex(PoseStack stack, VertexConsumer out, int light, Vec3 p, Vec3 n, int color) {
        out.addVertex(stack.last(), (float)p.x, (float)p.y, (float)p.z).setColor(color).setUv(.5F,.5F)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(stack.last(),(float)n.x,(float)n.y,(float)n.z);
    }
}
