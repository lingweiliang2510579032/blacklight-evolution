package com.codex.blacklight.client;

import com.codex.blacklight.BlacklightNeoMod;
import com.codex.blacklight.movement.LandingImpact;
import com.codex.blacklight.network.LandingVisualPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Synchronized ground-hugging fracture fields that grow and then fade naturally. */
@EventBusSubscriber(modid = BlacklightNeoMod.MOD_ID, value = Dist.CLIENT)
public final class ClientLandingVisuals {
    private static final DustParticleOptions CRACK_DARK =
            new DustParticleOptions(new Vector3f(0.035F, 0.001F, 0.002F), 0.55F);
    private static final DustParticleOptions CRACK_RED =
            new DustParticleOptions(new Vector3f(0.56F, 0.004F, 0.008F), 0.42F);
    private static final int MAX_ACTIVE_FIELDS = 8;
    private static final int CRACK_LIFETIME_TICKS = 120;
    private static final float CRACK_GROWTH_TICKS = 8.0F;
    private static final ArrayDeque<CrackField> ACTIVE_FIELDS = new ArrayDeque<>();

    private ClientLandingVisuals() {
    }

    public static void accept(LandingVisualPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || payload.strength() == LandingImpact.Strength.NONE) return;
        PrototypeAnimationController.acceptLanding(payload.entityId(), payload.strength());

        Entity source = minecraft.level.getEntity(payload.entityId());
        double centerX = source != null ? source.getX() : payload.surfacePos().getX() + 0.5D;
        double centerZ = source != null ? source.getZ() : payload.surfacePos().getZ() + 0.5D;
        double radius = Math.max(1.1D, Math.min(7.0D, payload.radius()));
        Random random = new Random(payload.seed());
        List<CrackSegment> segments = buildCracks(minecraft.level, centerX, centerZ,
                payload.surfacePos(), radius, payload.strength(), random);
        while (ACTIVE_FIELDS.size() >= MAX_ACTIVE_FIELDS) ACTIVE_FIELDS.removeFirst();
        ACTIVE_FIELDS.addLast(new CrackField(segments,
                minecraft.level.getGameTime(), CRACK_LIFETIME_TICKS));

        double y = surfaceHeight(minecraft.level, payload.surfacePos(), centerX, centerZ);
        int dust = Mth.clamp(4 + Math.round(payload.fallDistance() * 0.45F), 5, 22);
        for (int i = 0; i < dust; i++) {
            minecraft.level.addParticle(ParticleTypes.CLOUD,
                    centerX + random.nextGaussian() * radius * 0.26D,
                    y + 0.025D,
                    centerZ + random.nextGaussian() * radius * 0.26D,
                    random.nextGaussian() * 0.022D,
                    0.025D + random.nextDouble() * 0.04D,
                    random.nextGaussian() * 0.022D);
        }
        for (int i = 0; i < segments.size(); i += 3) {
            CrackSegment segment = segments.get(i);
            minecraft.level.addParticle(segment.hot() ? CRACK_RED : CRACK_DARK,
                    segment.x2(), segment.y2() + 0.006D, segment.z2(),
                    0.0D, 0.0D, 0.0D);
        }
    }

    @SubscribeEvent
    public static void onClientTick(net.minecraftforge.event.TickEvent.ClientTickEvent event){if(event.phase != net.minecraftforge.event.TickEvent.Phase.END)return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            ACTIVE_FIELDS.clear();
            return;
        }
        long now = minecraft.level.getGameTime();
        ACTIVE_FIELDS.removeIf(field -> now - field.createdAt() > field.lifetime());
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES
                || ACTIVE_FIELDS.isEmpty()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        float partial = event.getPartialTick();
        long now = minecraft.level.getGameTime();
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());

        for (CrackField field : ACTIVE_FIELDS) {
            float age = Math.max(0.0F, now - field.createdAt() + partial);
            float growth = Mth.clamp(age / CRACK_GROWTH_TICKS, 0.0F, 1.0F);
            float fade = age < field.lifetime() * 0.58F
                    ? 1.0F
                    : 1.0F - (age - field.lifetime() * 0.58F)
                    / (field.lifetime() * 0.42F);
            int alpha = Mth.clamp(Math.round(225.0F * fade), 0, 225);
            if (alpha <= 0) continue;
            for (CrackSegment segment : field.segments()) {
                if (segment.reveal() > growth) continue;
                int red = segment.hot() ? 112 : 7;
                int green = segment.hot() ? 2 : 0;
                int blue = segment.hot() ? 3 : 1;
                addLine(lines, poseStack.last(), segment, red, green, blue, alpha);
            }
        }
        buffers.endBatch(RenderType.lines());
        poseStack.popPose();
    }

    private static List<CrackSegment> buildCracks(ClientLevel level,
                                                  double centerX, double centerZ,
                                                  BlockPos origin, double radius,
                                                  LandingImpact.Strength strength,
                                                  Random random) {
        int rays = switch (strength) {
            case LIGHT -> 6;
            case HEAVY -> 9;
            case DEVASTATING -> 13;
            default -> 0;
        };
        int steps = Mth.clamp(5 + (int)Math.ceil(radius * 1.25D), 6, 14);
        List<CrackSegment> result = new ArrayList<>(rays * steps * 2);
        double phase = random.nextDouble() * Math.PI * 2.0D;
        double centerY = surfaceHeight(level, origin, centerX, centerZ);
        for (int ray = 0; ray < rays; ray++) {
            double angle = phase + Math.PI * 2.0D * ray / rays
                    + (random.nextDouble() - 0.5D) * 0.30D;
            double bend = (random.nextDouble() - 0.5D) * 0.62D;
            double previousX = centerX;
            double previousY = centerY;
            double previousZ = centerZ;
            for (int step = 1; step <= steps; step++) {
                float reveal = step / (float)steps;
                double progress = reveal;
                double branchAngle = angle + bend * progress
                        + (random.nextDouble() - 0.5D) * 0.055D;
                double distance = radius * progress * (0.90D + random.nextDouble() * 0.10D);
                double x = centerX + Math.cos(branchAngle) * distance;
                double z = centerZ + Math.sin(branchAngle) * distance;
                double y = surfaceHeight(level, origin, x, z);
                boolean hot = strength != LandingImpact.Strength.LIGHT
                        && (ray + step) % 5 == 0;
                if (Math.abs(y - previousY) <= 0.76D) {
                    result.add(new CrackSegment(previousX, previousY, previousZ,
                            x, y, z, reveal, hot));
                }
                if (step > 2 && step < steps && (step + ray) % 3 == 0) {
                    double side = ((step + ray) & 1) == 0 ? 0.62D : -0.62D;
                    double branchLength = radius * (0.09D + random.nextDouble() * 0.07D);
                    double branchX = x + Math.cos(branchAngle + side) * branchLength;
                    double branchZ = z + Math.sin(branchAngle + side) * branchLength;
                    double branchY = surfaceHeight(level, origin, branchX, branchZ);
                    if (Math.abs(branchY - y) <= 0.76D) {
                        result.add(new CrackSegment(x, y, z, branchX, branchY, branchZ,
                                reveal, hot));
                    }
                }
                previousX = x;
                previousY = y;
                previousZ = z;
            }
        }
        return result;
    }

    private static double surfaceHeight(ClientLevel level, BlockPos origin,
                                        double x, double z) {
        int blockX = Mth.floor(x);
        int blockZ = Mth.floor(z);
        for (int offset = 2; offset >= -3; offset--) {
            BlockPos candidate = new BlockPos(blockX, origin.getY() + offset, blockZ);
            BlockState state = level.getBlockState(candidate);
            if (!state.isAir() && !state.getCollisionShape(level, candidate).isEmpty()) {
                return candidate.getY() + 1.012D;
            }
        }
        return origin.getY() + 1.012D;
    }

    private static void addLine(VertexConsumer consumer, PoseStack.Pose pose,
                                CrackSegment segment, int red, int green,
                                int blue, int alpha) {
        float dx = (float)(segment.x2() - segment.x1());
        float dy = (float)(segment.y2() - segment.y1());
        float dz = (float)(segment.z2() - segment.z1());
        float length = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0E-4F) return;
        float nx = dx / length;
        float ny = dy / length;
        float nz = dz / length;
        consumer.vertex((pose).pose(),(float)segment.x1(),(float)segment.y1(),(float)segment.z1())
                .color(red, green, blue, alpha).normal((pose).normal(),nx,ny,nz).endVertex();
        consumer.vertex((pose).pose(),(float)segment.x2(),(float)segment.y2(),(float)segment.z2())
                .color(red, green, blue, alpha).normal((pose).normal(),nx,ny,nz).endVertex();
    }

    private record CrackField(List<CrackSegment> segments, long createdAt, int lifetime) {
    }

    private record CrackSegment(double x1, double y1, double z1,
                                double x2, double y2, double z2,
                                float reveal, boolean hot) {
    }
}
