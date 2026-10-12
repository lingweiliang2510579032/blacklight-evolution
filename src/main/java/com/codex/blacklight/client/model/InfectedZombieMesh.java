package com.codex.blacklight.client.model;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Original Blender mesh, split at the real Epic Fight joints. */
public final class InfectedZombieMesh {
    private record Face(float[][] p, float nx, float ny, float nz, int color) {}
    private final Map<String,List<Face>> joints = new LinkedHashMap<>();
    public InfectedZombieMesh() {
        try (var stream = getClass().getResourceAsStream("/assets/blacklight/models/infected_zombie.json")) {
            if (stream == null) throw new IllegalStateException("Missing infected zombie mesh");
            var data=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
            for (var entry:data.getAsJsonObject("joints").entrySet()) {
                var faces=new ArrayList<Face>();
                for (var element:entry.getValue().getAsJsonArray()) {
                    var f=element.getAsJsonObject();float[][] p=new float[3][3];
                    for(int i=0;i<3;i++) for(int a=0;a<3;a++) p[i][a]=f.getAsJsonArray("p").get(i).getAsJsonArray().get(a).getAsFloat();
                    float ux=p[1][0]-p[0][0],uy=p[1][1]-p[0][1],uz=p[1][2]-p[0][2];
                    float vx=p[2][0]-p[0][0],vy=p[2][1]-p[0][1],vz=p[2][2]-p[0][2];
                    float nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx;
                    float len=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);
                    if(len<1e-8) throw new IllegalStateException("Degenerate infected mesh triangle");
                    var c=f.getAsJsonArray("c");int color=0xff000000|c.get(0).getAsInt()<<16|c.get(1).getAsInt()<<8|c.get(2).getAsInt();
                    faces.add(new Face(p,nx/len,ny/len,nz/len,color));
                }
                joints.put(entry.getKey(),List.copyOf(faces));
            }
        } catch(Exception e) { throw new IllegalStateException("Cannot load infected zombie mesh",e); }
    }
    public Set<String> joints() { return joints.keySet(); }
    public void render(String joint, PoseStack stack, VertexConsumer out, int light, int overlay, float growth) {
        if(growth<=.001F)return;
        var pose=stack.last();
        for(var f:joints.get(joint)) for(int i=0;i<4;i++) {
            var p=f.p[Math.min(i,2)];
            out.addVertex(pose,p[0]*growth,p[1]*growth,p[2]*growth).setColor(f.color)
                    .setUv(.5F,.5F).setOverlay(overlay).setLight(light).setNormal(pose,f.nx,f.ny,f.nz);
        }
    }
}
