package com.codex.blacklight.client.renderer.layer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Temporary strands wrapping the arm during growth and retraction. Joint local, no world lag. */
public final class BiomassTendrils {
    private BiomassTendrils() {}
    public static void render(PoseStack stack,VertexConsumer out,int light,float progress,float length,float time) {
        if(progress<=.001F||progress>=.999F)return;
        float envelope=(float)Math.sin(Math.PI*progress);
        var pose=stack.last();
        for(int strand=0;strand<3;strand++) {
            int color=strand==1?0xff892b27:0xff262525;
            float radius=.127F+.047F*envelope;
            for(int segment=0;segment<18;segment++) {
                float t=segment/18F,nt=(segment+1)/18F;
                float a=t*9+strand*2.094F+time*.07F,b=nt*9+strand*2.094F+time*.07F;
                float x=(float)Math.cos(a)*radius,z=(float)Math.sin(a)*radius;
                float nx=(float)Math.cos(b)*radius,nz=(float)Math.sin(b)*radius;
                float y=t*length*progress,ny=nt*length*progress;
                float w=.016F*envelope*(1-.65F*t);
                for(int k=0;k<6;k++) {
                    double u=k*Math.PI/3,v=(k+1)*Math.PI/3;
                    vertex(out,pose,x+w*(float)Math.cos(u),y,z+w*(float)Math.sin(u),color,light);
                    vertex(out,pose,nx+w*(float)Math.cos(u),ny,nz+w*(float)Math.sin(u),color,light);
                    vertex(out,pose,nx+w*(float)Math.cos(v),ny,nz+w*(float)Math.sin(v),color,light);
                    vertex(out,pose,x+w*(float)Math.cos(v),y,z+w*(float)Math.sin(v),color,light);
                }
            }
        }
    }
    private static void vertex(VertexConsumer out,PoseStack.Pose pose,float x,float y,float z,int color,int light) {
        float len=(float)Math.sqrt(x*x+z*z);
        out.addVertex(pose,x,y,z).setColor(color).setUv(.5F,.5F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(pose,x/len,0,z/len);
    }
}
