package com.codex.blacklight.client.renderer.layer;

import com.codex.blacklight.client.animation.BiomassSurface;
import com.codex.blacklight.client.renderer.LiquidBiomassMaterial;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** A few thick streams travel on the joint itself. They use the same clock as
 * the deforming surface and are never emitted as delayed world particles. */
public final class LiquidBiomassRenderer {
    private static final ResourceLocation WHITE = new ResourceLocation("blacklight", "textures/entity/claw_white.png");
    private static final int SEGMENTS = 12;
    private static final int STREAMS = 4;
    private LiquidBiomassRenderer() { }

    public static void render(PoseStack stack, MultiBufferSource buffers, int light,
                              BiomassSurface.Frame frame, float length, float time, boolean forearm) {
        if (!frame.liquid() || com.codex.blacklight.client.renderer.VisionPowerRenderer.drawingSilhouettes()) return;
        var out = buffers.getBuffer(LiquidBiomassMaterial.mesh(WHITE));
        var pose = stack.last();
        float flow = frame.flow(), grown = frame.growth();
        for (int lane = 0; lane < STREAMS; lane++) {
            float lanePhase = lane / (float)STREAMS + (lane % 2 == 0 ? .018F : -.012F);
            for (int segment = 0; segment < SEGMENTS; segment++) {
                float a = segment / (float)SEGMENTS, b = (segment + 1F) / SEGMENTS;
                float wa = weight(a, frame, forearm), wb = weight(b, frame, forearm);
                if (wa < .001F && wb < .001F) continue;
                float angleA = lanePhase + a * .57F + time * .006F;
                float angleB = lanePhase + b * .57F + time * .006F;
                float xa = BiomassSurface.wave(angleA + .25F), za = BiomassSurface.wave(angleA);
                float xb = BiomassSurface.wave(angleB + .25F), zb = BiomassSurface.wave(angleB);
                // Native skin has a square 4/16 cross section. A constant .12
                // radius disappears inside its corners, so each stream follows
                // the actual square surface before the liquid lobe swells out.
                float ra = skinRadius(xa,za) + wa * .021F + BiomassSurface.wave(a * 1.3F + lanePhase + time * .023F) * .006F * wa;
                float rb = skinRadius(xb,zb) + wb * .021F + BiomassSurface.wave(b * 1.3F + lanePhase + time * .023F) * .006F * wb;
                float ya = a * length * (.55F + .45F * grown), yb = b * length * (.55F + .45F * grown);
                float bodyWidth = forearm ? .060F : .050F;
                float widthA = wa * (bodyWidth + .014F * flow), widthB = wb * (bodyWidth + .014F * flow);
                int color = lane == 1 ? 0xEC3E151A : 0xF0171418;
                // Broad, flattened lobes wrap the arm instead of reading as wires.
                for (int side = 0; side < 4; side++) {
                    float u = side / 4F, v = (side + 1F) / 4F;
                    streamVertex(out, pose, xa, za, ra, ya, widthA, u, color, light);
                    streamVertex(out, pose, xb, zb, rb, yb, widthB, u, color, light);
                    streamVertex(out, pose, xb, zb, rb, yb, widthB, v, color, light);
                    streamVertex(out, pose, xa, za, ra, ya, widthA, v, color, light);
                }
                // A narrow wet ridge shares this buffer and does not repeat the
                // weapon mesh. Natural light still controls the graphite highlight.
                int sheen = 0xB04B464E;
                ridgeVertex(out, pose, xa, za, ra + widthA * .30F + .0005F, ya, -widthA * .16F, sheen, light);
                ridgeVertex(out, pose, xb, zb, rb + widthB * .30F + .0005F, yb, -widthB * .16F, sheen, light);
                ridgeVertex(out, pose, xb, zb, rb + widthB * .30F + .0005F, yb, widthB * .16F, sheen, light);
                ridgeVertex(out, pose, xa, za, ra + widthA * .30F + .0005F, ya, widthA * .16F, sheen, light);
            }
        }
    }

    private static float skinRadius(float x,float z) { return .125F / Math.max(.001F,Math.max(Math.abs(x),Math.abs(z))); }

    private static float weight(float at, BiomassSurface.Frame frame, boolean forearm) {
        float axial = forearm ? .32F + at * .68F : at * .32F;
        float front = 1 - BiomassSurface.smooth(Math.abs(axial - frame.growth()) / .34F);
        float covered = BiomassSurface.smooth((frame.growth() + .15F - axial) / .25F);
        float cap = BiomassSurface.smooth(at / .12F) * BiomassSurface.smooth((1 - at) / .10F);
        return frame.flow() * cap * (.25F * covered + .75F * front);
    }
    private static void streamVertex(VertexConsumer out, PoseStack.Pose pose, float rx, float rz,
                                     float radius, float y, float width, float around, int color, int light) {
        float radial = BiomassSurface.wave(around + .25F), tangent = BiomassSurface.wave(around);
        float x = rx * (radius + radial * width * .30F) - rz * tangent * width;
        float z = rz * (radius + radial * width * .30F) + rx * tangent * width;
        float nx=rx*radial-rz*tangent*.30F,nz=rz*radial+rx*tangent*.30F;
        float normalLength=(float)Math.sqrt(nx*nx+nz*nz);
        out.vertex(pose.pose(), x, y, z).color(color).uv(.8F, .5F).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light).normal(pose.normal(), nx/normalLength, 0, nz/normalLength).endVertex();
    }
    private static void ridgeVertex(VertexConsumer out, PoseStack.Pose pose, float rx, float rz,
                                    float radius, float y, float width, int color, int light) {
        out.vertex(pose.pose(), rx * radius - rz * width, y, rz * radius + rx * width)
                .color(color).uv(1F, .5F).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light).normal(pose.normal(), rx, 0, rz).endVertex();
    }
}
