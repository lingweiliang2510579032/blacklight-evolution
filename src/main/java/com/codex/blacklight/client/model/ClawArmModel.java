package com.codex.blacklight.client.model;

import com.codex.blacklight.BlacklightNeoMod;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** Blender surfaces authored in the shipped Epic Fight skeleton's joint-local space. */
public final class ClawArmModel extends Model {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(BlacklightNeoMod.MOD_ID, "claw_arm"), "main");
    private record Triangle(float[][] points, float nx, float ny, float nz, int color) { }
    private final Map<String, Triangle[]> joints = new HashMap<>();

    public ClawArmModel(ModelPart unused) {
        super(RenderType::entityCutoutNoCull);
        try (var stream = ClawArmModel.class.getResourceAsStream("/assets/blacklight/models/claw_blender.json")) {
            if (stream == null) throw new IllegalStateException("Missing authored claw mesh");
            var root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var entry : root.getAsJsonObject("joints").entrySet()) {
                var source = entry.getValue().getAsJsonArray();
                Triangle[] triangles = new Triangle[source.size()];
                for (int i = 0; i < triangles.length; i++) {
                    var face = source.get(i).getAsJsonObject();
                    float[][] p = new float[3][3];
                    for (int v = 0; v < 3; v++) for (int axis = 0; axis < 3; axis++)
                        p[v][axis] = face.getAsJsonArray("p").get(v).getAsJsonArray().get(axis).getAsFloat();
                    float ux=p[1][0]-p[0][0], uy=p[1][1]-p[0][1], uz=p[1][2]-p[0][2];
                    float vx=p[2][0]-p[0][0], vy=p[2][1]-p[0][1], vz=p[2][2]-p[0][2];
                    float nx=uy*vz-uz*vy, ny=uz*vx-ux*vz, nz=ux*vy-uy*vx;
                    float len=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);
                    if (len < 1e-8F) throw new IllegalStateException("Degenerate claw face");
                    var c = face.getAsJsonArray("c");
                    int color=0xFF000000 | c.get(0).getAsInt()<<16 | c.get(1).getAsInt()<<8 | c.get(2).getAsInt();
                    triangles[i] = new Triangle(p,nx/len,ny/len,nz/len,color);
                }
                joints.put(entry.getKey(), triangles);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Cannot load Blender claw mesh", e);
        }
    }

    public static LayerDefinition createLayer() {
        return LayerDefinition.create(new MeshDefinition(), 1, 1);
    }

    public void renderJoint(String joint, PoseStack stack, VertexConsumer consumer,
                            int light, int overlay, float progress, float talonLength) {
        Triangle[] faces = joints.get(joint);
        if (faces == null) return;
        float t=Math.clamp(progress,0,1), growth=t*t*(3-2*t);
        float width=.82F+.18F*growth;
        boolean forearm=joint.startsWith("Hand");
        PoseStack.Pose pose=stack.last();
        for (Triangle face : faces) {
            // QUADS require a repeated final vertex for triangular authored surfaces.
            for (int v=0;v<4;v++) {
                float[] p=face.points[Math.min(v,2)];
                float y=p[1];
                if (forearm && y>.42F) y=.42F+(y-.42F)*growth*talonLength;
                consumer.addVertex(pose,p[0]*width,y,p[2]*width).setColor(face.color)
                        .setUv(.5F,.5F).setOverlay(overlay).setLight(light)
                        .setNormal(pose,face.nx,face.ny,face.nz);
            }
        }
    }

    @Override
    public void renderToBuffer(PoseStack stack, VertexConsumer consumer, int light, int overlay, int color) {
        renderJoint("Hand_R",stack,consumer,light,overlay,1,1);
    }
}
