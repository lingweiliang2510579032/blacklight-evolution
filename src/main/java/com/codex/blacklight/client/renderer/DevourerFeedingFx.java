package com.codex.blacklight.client.renderer;

import com.codex.blacklight.entity.BlacklightInfectedHost;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Feeding tissue follows the server's real victim and capture clock, including growing hosts. */
public final class DevourerFeedingFx {
    private static java.lang.ref.WeakReference<net.minecraft.client.multiplayer.ClientLevel> indexedLevel = new java.lang.ref.WeakReference<>(null);
    private static long indexedTick = Long.MIN_VALUE;
    private static final java.util.Map<Integer, Integer> CONSUMERS = new java.util.HashMap<>();
    private DevourerFeedingFx() {}

    /** Called once around the victim's complete model, keeping its torso at the tether endpoint. */
    private static BlacklightInfectedHost consumer(LivingEntity victim) {
        if (!(victim.level() instanceof net.minecraft.client.multiplayer.ClientLevel level)
                || level.getEntity(victim.getId()) != victim) return null; // Native appearance proxies are already scaled by their host.
        if (indexedLevel.get() != level || indexedTick != level.getGameTime()) {
            CONSUMERS.clear(); indexedLevel = new java.lang.ref.WeakReference<>(level); indexedTick = level.getGameTime();
            for (var candidate : level.entitiesForRendering())
                if (candidate instanceof BlacklightInfectedHost host && host.devourerIdentity() && host.feedingTargetId() >= 0)
                    CONSUMERS.put(host.feedingTargetId(), host.getId());
        }
        Integer id = CONSUMERS.get(victim.getId());
        if (id == null || !(level.getEntity(id) instanceof BlacklightInfectedHost host)
                || host.feedingTargetId() != victim.getId() || host.feedingStarted() < 0 || !host.isAlive()) return null;
        return host;
    }

    public static boolean shrinkingVictim(LivingEntity victim, float partial) {
        var host = consumer(victim);
        if (host == null) return false;
        float age = victim.level().getGameTime() - host.feedingStarted() + partial;
        return age >= 54 && age < 80;
    }

    public static void applyVictim(LivingEntity victim, float partial, PoseStack stack) {
        var host = consumer(victim);
        if (host == null) return;
        float age = victim.level().getGameTime() - host.feedingStarted() + partial;
        if (age < 54 || age >= 80) return;
        float size = (float) contraction(age);
        double torso = victim.getBbHeight() * .55;
        stack.translate(0, torso, 0); stack.scale(size, size, size); stack.translate(0, -torso, 0);
    }

    public static void render(BlacklightInfectedHost host, float partial, PoseStack stack,
            MultiBufferSource buffers, int light) {
        if (!host.devourerIdentity() || host.feedingStarted() < 0 || host.isInvisible()) return;
        if (!(host.level().getEntity(host.feedingTargetId()) instanceof LivingEntity prey) || !prey.isAlive()) return;
        float age = host.level().getGameTime() - host.feedingStarted() + partial;
        if (age <= 4 || age >= 80) return;
        double reach = smooth((age - 4) / 14), pull = smooth((age - 48) / 30);
        double fade = 1 - smooth((age - 76) / 4);
        float yaw = Mth.rotLerp(partial, host.yBodyRotO, host.yBodyRot);
        Vec3 forward = Vec3.directionFromRotation(0, yaw), right = new Vec3(forward.z, 0, -forward.x);
        double scale = host.bodyScale();
        Vec3 chest = forward.scale(Math.max(.24, 2.3 * scale)).add(0, Math.max(1.25, 18.5 * scale), 0);
        Vec3 target = prey.getPosition(partial).subtract(host.getPosition(partial)).add(0, prey.getBbHeight() * .55, 0);
        if (target.subtract(chest).lengthSqr() > 48 * 48) return;
        var out = buffers.getBuffer(RenderType.entityCutoutNoCull(ConsumedMobVisuals.WHITE));
        double contraction = contraction(age);
        double preyRadius = Math.clamp(prey.getBbWidth() * .56, .18, 2.5) * contraction;
        double thickness = Math.clamp(.065 + scale * .12, .065, .20) * fade;
        for (int strand = 0; strand < 6; strand++) {
            double a = strand * Math.PI / 3 + host.getId() * .13;
            Vec3 radial = right.scale(Math.cos(a)).add(0, Math.sin(a), 0);
            Vec3 start = chest.add(radial.scale(Math.max(.13, scale * .8)));
            Vec3 end = target.add(radial.scale(preyRadius * (1 - .20 * pull)));
            Vec3 bend = radial.scale((.35 + start.distanceTo(end) * .12) * (1 - .70 * pull));
            Vec3 c1 = start.lerp(end, .28).add(bend);
            Vec3 c2 = start.lerp(end, .75).add(bend.scale(.65));
            Vec3 last = start;
            for (int segment = 1; segment <= 24; segment++) {
                double t = segment / 24.0 * reach;
                Vec3 next = curve(start, c1, c2, end, t);
                double radius = thickness * (1 - segment / 24.0 * .66);
                ConsumedMobVisuals.tube(stack, out, light, last, next, radius,
                        strand % 3 == 0 ? 0xff722b32 : 0xff302128);
                last = next;
            }
            // Tightening coils visibly connect to the torso instead of ending in empty space.
            if (age >= 18) {
                Vec3 previous = end;
                for (int i = 1; i <= 16; i++) {
                    double t = i / 16.0;
                    double angle = a + t * Math.PI * 1.8;
                    Vec3 coil = right.scale(Math.cos(angle)).add(forward.scale(Math.sin(angle)))
                            .scale(preyRadius * (1 - .20 * pull));
                    Vec3 next = target.add(coil).add(0, (t - .5) * prey.getBbHeight() * .55 * contraction, 0);
                    ConsumedMobVisuals.tube(stack, out, light, previous, next, thickness * .65, 0xff52232b);
                    previous = next;
                }
            }
            // A travelling biomass bulge runs from the victim back into the devourer's chest.
            if (age >= 48) {
                double flow = 1 - ((age - 48) * .07 + strand / 6.0) % 1;
                Vec3 point = curve(start, c1, c2, end, flow);
                Vec3 tangent = end.subtract(start).normalize();
                ConsumedMobVisuals.tube(stack, out, light, point.subtract(tangent.scale(thickness)),
                        point.add(tangent.scale(thickness)), thickness * 1.6, 0xff9a3d43);
            }
        }
    }

    private static Vec3 curve(Vec3 a, Vec3 b, Vec3 c, Vec3 d, double t) {
        double s = 1 - t;
        return a.scale(s * s * s).add(b.scale(3 * s * s * t)).add(c.scale(3 * s * t * t)).add(d.scale(t * t * t));
    }
    private static double smooth(double t) { t = Math.clamp(t, 0, 1); return t * t * (3 - 2 * t); }
    private static double contraction(float age) { return 1 - .985 * smooth((age - 54) / 25); }
}
