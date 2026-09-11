package com.codex.blacklight.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/** Baked diamond-section blade. The terminal segment ends in a point, not a box. */
final class ClawBladeMesh {
    private final float[][] faces = new float[4][];
    private final float[][] normals = new float[4][];

    ClawBladeMesh(float length, float width, float depth, float tipScale, float bend) {
        float[][] start = {{-width, 0, 0}, {0, 0, -depth}, {width, 0, 0}, {0, 0, depth}};
        float[][] end = {{-width * tipScale, -length, bend}, {0, -length, bend - depth * tipScale},
                {width * tipScale, -length, bend}, {0, -length, bend + depth * tipScale}};
        for (int i = 0; i < 4; i++) {
            int next = (i + 1) % 4;
            float[] a = start[next], b = end[next], c = end[i], d = start[i];
            faces[i] = new float[] {a[0], a[1], a[2], b[0], b[1], b[2],
                    c[0], c[1], c[2], d[0], d[1], d[2]};
            // Use a,b,d: b and c coincide at a pointed tip.
            float ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
            float vx = d[0] - a[0], vy = d[1] - a[1], vz = d[2] - a[2];
            float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
            float magnitude = (float)Math.sqrt(nx * nx + ny * ny + nz * nz);
            normals[i] = new float[] {nx / magnitude, ny / magnitude, nz / magnitude};
        }
    }

    void render(PoseStack stack, VertexConsumer consumer, int light, int overlay) {
        PoseStack.Pose pose = stack.last();
        for (int face = 0; face < faces.length; face++) {
            float[] points = faces[face], normal = normals[face];
            // Reuse the existing dark organic texture; lit facets define the cutting edge.
            for (int vertex = 0; vertex < 4; vertex++) {
                int index = vertex * 3;
                consumer.addVertex(pose, points[index] / 16, points[index + 1] / 16, points[index + 2] / 16)
                        .setColor(0xFFFFFFFF)
                        .setUv((32 + (vertex < 2 ? 0 : 4)) / 64.0F,
                                (24 + (vertex == 0 || vertex == 3 ? 0 : 8)) / 64.0F)
                        .setOverlay(overlay).setLight(light)
                        .setNormal(pose, normal[0], normal[1], normal[2]);
            }
        }
    }
}
