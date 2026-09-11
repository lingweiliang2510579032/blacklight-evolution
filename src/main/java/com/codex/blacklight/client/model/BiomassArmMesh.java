package com.codex.blacklight.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/** Faceted muscle volume, with a tapered shoulder and enlarged palm instead of a box cuff. */
final class BiomassArmMesh {
    private static final int SIDES = 8;
    // Authored pixels: y, lateral radius, depth radius. Retains the established arm bind placement.
    private static final float[][] PROFILE = {
            {9.0F, 3.10F, 3.0F}, {6.0F, 3.85F, 3.85F}, {1.0F, 3.65F, 3.45F},
            {-1.0F, 4.2F, 3.8F}, {-4.5F, 4.65F, 4.45F}, {-6.5F, 3.9F, 3.6F}
    };
    private final float[][] faces;
    private final float[][] normals;
    private final float topY;
    private final float height;

    BiomassArmMesh() {
        this(PROFILE);
    }

    BiomassArmMesh(float[][] profile) {
        faces = new float[(profile.length - 1) * SIDES + 2 * SIDES][];
        normals = new float[faces.length][];
        topY = profile[0][0];
        height = topY - profile[profile.length - 1][0];
        float[][][] rings = new float[profile.length][SIDES][3];
        for (int row = 0; row < profile.length; row++) {
            for (int side = 0; side < SIDES; side++) {
                double angle = Math.PI * 2 * side / SIDES;
                rings[row][side] = new float[] {(float)Math.cos(angle) * profile[row][1],
                        profile[row][0], (float)Math.sin(angle) * profile[row][2]};
            }
        }
        int face = 0;
        for (int row = 0; row < profile.length - 1; row++) {
            for (int side = 0; side < SIDES; side++) {
                int next = (side + 1) % SIDES;
                bake(face++, rings[row][next], rings[row + 1][next],
                        rings[row + 1][side], rings[row][side]);
            }
        }
        for (int side = 0; side < SIDES; side++) {
            int next = (side + 1) % SIDES;
            float[] top = {0, profile[0][0], 0};
            float[] bottom = {0, profile[profile.length - 1][0], 0};
            bake(face++, rings[0][side], top, top, rings[0][next]);
            bake(face++, rings[profile.length - 1][next], bottom, bottom,
                    rings[profile.length - 1][side]);
        }
    }

    private void bake(int index, float[] a, float[] b, float[] c, float[] d) {
        faces[index] = new float[] {a[0], a[1], a[2], b[0], b[1], b[2],
                c[0], c[1], c[2], d[0], d[1], d[2]};
        float ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
        float vx = d[0] - a[0], vy = d[1] - a[1], vz = d[2] - a[2];
        float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
        float length = (float)Math.sqrt(nx * nx + ny * ny + nz * nz);
        normals[index] = new float[] {nx / length, ny / length, nz / length};
    }

    void render(PoseStack stack, VertexConsumer consumer, int light, int overlay, int color) {
        PoseStack.Pose pose = stack.last();
        for (int face = 0; face < faces.length; face++) {
            float[] points = faces[face], normal = normals[face];
            for (int vertex = 0; vertex < 4; vertex++) {
                int index = vertex * 3;
                consumer.addVertex(pose, points[index] / 16, points[index + 1] / 16, points[index + 2] / 16)
                        .setColor(color)
                        .setUv((face % SIDES + (vertex < 2 ? 1 : 0)) / (float)SIDES,
                                (topY - points[index + 1]) / height)
                        .setOverlay(overlay).setLight(light)
                        .setNormal(pose, normal[0], normal[1], normal[2]);
            }
        }
    }
}
