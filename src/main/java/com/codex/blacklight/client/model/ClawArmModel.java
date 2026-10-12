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
import org.joml.Matrix4f;

/** Blender surfaces authored in the Blacklight skeleton's joint-local space. */
public final class ClawArmModel extends Model {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(BlacklightNeoMod.MOD_ID, "claw_arm"), "main");
    private record Triangle(float[][] points, float[][] normals, int color) { }
    private final Map<String, Triangle[]> joints = new HashMap<>();

    public ClawArmModel(ModelPart unused) {
        this("claw_blender");
    }

    public ClawArmModel(String asset) {
        super(RenderType::entityCutoutNoCull);
        try (var stream = ClawArmModel.class.getResourceAsStream("/assets/blacklight/models/" + asset + ".json")) {
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
                    float[][] normals=new float[3][3];
                    for(int v=0;v<3;v++) {
                        if(face.has("n")) {
                            var n=face.getAsJsonArray("n").get(v).getAsJsonArray();
                            for(int axis=0;axis<3;axis++)normals[v][axis]=n.get(axis).getAsFloat();
                        } else normals[v]=new float[]{nx/len,ny/len,nz/len};
                    }
                    if(asset.equals("form_blade")&&entry.getKey().equals("Hand_R"))for(int v=0;v<3;v++){
                        float sy=BladeShape.lengthScale(p[v][1]);
                        p[v][0]*=BladeShape.WIDTH;p[v][2]*=BladeShape.WIDTH;p[v][1]=BladeShape.length(p[v][1]);
                        var n=normals[v];n[0]/=BladeShape.WIDTH;n[1]/=sy;n[2]/=BladeShape.WIDTH;
                        float size=(float)Math.sqrt(n[0]*n[0]+n[1]*n[1]+n[2]*n[2]);
                        for(int axis=0;axis<3;axis++)n[axis]/=size;
                    }
                    triangles[i] = new Triangle(p,normals,color);
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
        renderJoint(joint,stack,consumer,light,overlay,progress,talonLength,null);
    }

    public void renderJoint(String joint, PoseStack stack, VertexConsumer consumer,
                             int light, int overlay, float progress, float talonLength,Matrix4f wristDelta) {
        renderJoint(joint,stack,consumer,light,overlay,progress,talonLength,wristDelta,Float.POSITIVE_INFINITY);
    }
    public void renderJoint(String joint, PoseStack stack, VertexConsumer consumer,
                             int light, int overlay, float progress, float talonLength,Matrix4f wristDelta,float distalLimit) {
        renderJoint(joint,stack,consumer,light,overlay,progress,talonLength,wristDelta,distalLimit,null);
    }
    public void renderJoint(String joint, PoseStack stack, VertexConsumer consumer,
                             int light, int overlay, float progress, float talonLength,Matrix4f wristDelta,float distalLimit,
                             com.codex.blacklight.combat.HammerScales.Shape hammer) {
        Triangle[] faces = joints.get(joint);
        if (faces == null) return;
        float t=Math.clamp(progress,0,1), growth=t*t*(3-2*t);
        float sheath=com.codex.blacklight.infection.InfectionTimeline.stage(t,.05F,.70F);
        float blade=com.codex.blacklight.infection.InfectionTimeline.stage(t,.48F,1);
        float width=.08F+.92F*sheath;
        boolean forearm=joint.startsWith("Hand");
        PoseStack.Pose pose=stack.last();
        for (Triangle face : faces) {
            if(forearm&&Math.max(face.points[0][1],Math.max(face.points[1][1],face.points[2][1]))>distalLimit)continue;
            // QUADS require a repeated final vertex for triangular authored surfaces.
            for (int v=0;v<4;v++) {
                float[] p=face.points[Math.min(v,2)];
                float[] n=face.normals[Math.min(v,2)];
                float y=p[1];
                if (forearm && y>.30F) y=.30F+(y-.30F)*blade*talonLength;
                float x=p[0]*width,z=p[2]*width,nx=n[0],ny=n[1],nz=n[2];
                if(hammer!=null&&forearm) {
                    // The authored mesh combines forearm and fist in one joint. Blend
                    // their source scales along its length, keeping the elbow seam fixed.
                    float fore=hammer.forearm()-1,fist=hammer.fist()-hammer.forearm();
                    float s=1+fore*smooth(y/.30F)+fist*smooth((y-.28F)/.12F);
                    float derivative=fore*slope(y,.30F)+fist*slope(y-.28F,.12F);
                    float nyScaled=(ny-derivative*(x*nx+z*nz)/s)/s;
                    nx/=s;nz/=s;ny=nyScaled;
                    float length=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);
                    if(length>1e-6F){nx/=length;ny/=length;nz/=length;}
                    x*=s;z*=s;y+=fore*integral(y,.30F)+fist*integral(y-.28F,.12F);
                }
                if(wristDelta!=null&&forearm){
                    // Blend only the wrist/palm and talons; the forearm keeps its elbow
                    // attachment. This transports the native wrist instead of posing it.
                    float w=Math.clamp((p[1]-.28F)/.10F,0,1);w=w*w*(3-2*w);
                    float tx=wristDelta.m00()*x+wristDelta.m10()*y+wristDelta.m20()*z+wristDelta.m30();
                    float ty=wristDelta.m01()*x+wristDelta.m11()*y+wristDelta.m21()*z+wristDelta.m31();
                    float tz=wristDelta.m02()*x+wristDelta.m12()*y+wristDelta.m22()*z+wristDelta.m32();
                    x+=(tx-x)*w;y+=(ty-y)*w;z+=(tz-z)*w;
                    float rx=wristDelta.m00()*nx+wristDelta.m10()*ny+wristDelta.m20()*nz;
                    float ry=wristDelta.m01()*nx+wristDelta.m11()*ny+wristDelta.m21()*nz;
                    float rz=wristDelta.m02()*nx+wristDelta.m12()*ny+wristDelta.m22()*nz;
                    nx+=(rx-nx)*w;ny+=(ry-ny)*w;nz+=(rz-nz)*w;
                    float length=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);
                    if(length>1e-6F){nx/=length;ny/=length;nz/=length;}
                }
                consumer.addVertex(pose,x,y,z).setColor(face.color)
                        .setUv(.5F,.5F).setOverlay(overlay).setLight(light)
                        .setNormal(pose,nx,ny,nz);
            }
        }
    }

    private static float smooth(float t){t=Math.clamp(t,0,1);return t*t*(3-2*t);}
    private static float slope(float value,float width){float t=value/width;return t<=0||t>=1?0:6*t*(1-t)/width;}
    private static float integral(float value,float width){
        if(value<=0)return 0;if(value>=width)return value-width*.5F;
        float t=value/width;return width*(t*t*t-.5F*t*t*t*t);
    }

    @Override
    public void renderToBuffer(PoseStack stack, VertexConsumer consumer, int light, int overlay, int color) {
        renderJoint("Hand_R",stack,consumer,light,overlay,1,1);
    }
}
