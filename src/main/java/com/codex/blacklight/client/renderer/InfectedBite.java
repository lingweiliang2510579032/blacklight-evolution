package com.codex.blacklight.client.renderer;

import com.codex.blacklight.mixin.client.ModelPartChildrenAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** A small stepped bite in an original textured cuboid. UVs and posed positions are
 * interpolated from that cuboid's actual face; no species body or skeleton is replaced. */
final class InfectedBite {
    final ModelPart.Cube cube;
    final float face,inward,depth,y0,y1,z0,z1;
    private InfectedBite(ModelPart.Cube cube,float face,float inward,float depth,float y0,float y1,float z0,float z1){
        this.cube=cube;this.face=face;this.inward=inward;this.depth=depth;
        this.y0=y0;this.y1=y1;this.z0=z0;this.z1=z1;
    }
    static InfectedBite create(ModelPart part,int anatomy,String profile){
        if(anatomy!=ConsumeAnatomy.BODY&&anatomy!=ConsumeAnatomy.HEAD&&anatomy!=ConsumeAnatomy.RIGHT_LEG)return null;
        ModelPart.Cube best=null;float volume=0;
        for(var c:((ModelPartChildrenAccessor)(Object)part).blacklight$cubes()){
            float v=(c.maxX-c.minX)*(c.maxY-c.minY)*(c.maxZ-c.minZ);
            if(v>volume){volume=v;best=c;}
        }
        if(best==null)return null;
        float width=best.maxX-best.minX,height=best.maxY-best.minY,depth=best.maxZ-best.minZ;
        if(width<1||height<1.5F||depth<1)return null; // Fins and single-plane wings stay intact.
        boolean head=anatomy==ConsumeAnatomy.HEAD;
        float inward=head?-1:1,face=head?best.maxX:best.minX;
        float sizeY=head?.38F:anatomy==ConsumeAnatomy.RIGHT_LEG?.32F:.48F;
        float sizeZ=head?.35F:.50F;
        if(profile.equals("shelled")){sizeY=.33F;sizeZ=.38F;}
        if(profile.equals("aquatic")){sizeY=.38F;sizeZ=.38F;}
        if(profile.equals("arthropod")){sizeY=.40F;sizeZ=.46F;}
        // Off-centre body/cheek wounds leave faces, horns and most native markings visible.
        float middleY=best.minY+height*(head?.63F:.53F),middleZ=best.minZ+depth*(head?.34F:.48F);
        return new InfectedBite(best,face,inward,Math.max(.32F,Math.min(2.2F,width*.20F)),
                middleY-height*sizeY*.5F,middleY+height*sizeY*.5F,
                middleZ-depth*sizeZ*.5F,middleZ+depth*sizeZ*.5F);
    }
    private boolean missing(float y,float z){
        if(y<=y0||y>=y1||z<=z0||z>=z1)return false;
        float fy=(y-y0)/(y1-y0),fz=(z-z0)/(z1-z0);
        return !(fy<.25F&&fz<.16F)&&!(fy>.75F&&fz>.82F);
    }
    void compile(PoseStack.Pose pose,VertexConsumer output,int light,int overlay,int color){
        cube.compile(pose,new Cut(output,new Matrix4f(pose.pose()).invert()),light,overlay,((color>>16)&255)/255F,((color>>8)&255)/255F,(color&255)/255F,((color>>>24)&255)/255F);
    }

    private final class Cut implements VertexConsumer {
        private final VertexConsumer out;
        private final Matrix4f inverse;
        // Four vertices per native Cube face. Allocation is bounded per bitten part,
        // never by entity age, world population, animation frame history or texture.
        private final float[][] vertices=new float[4][11];
        private final Vector3f temporary=new Vector3f();
        private int index,color,overlay,light;
        Cut(VertexConsumer out,Matrix4f inverse){this.out=out;this.inverse=inverse;}
        @Override public void vertex(float x,float y,float z,float red,float green,float blue,float alpha,float u,float v,int overlay,int light,float nx,float ny,float nz){
            int color=((int)(alpha*255)<<24)|((int)(red*255)<<16)|((int)(green*255)<<8)|(int)(blue*255);
            float[] vertex=vertices[index];
            vertex[0]=x;vertex[1]=y;vertex[2]=z;vertex[3]=u;vertex[4]=v;
            vertex[5]=nx;vertex[6]=ny;vertex[7]=nz;
            inverse.transformPosition(x,y,z,temporary).mul(16);
            vertex[8]=temporary.x;vertex[9]=temporary.y;vertex[10]=temporary.z;
            this.color=color;this.overlay=overlay;this.light=light;
            if(++index==4){emitFace();index=0;}
        }
        private void emitFace(){
            // Deformed vanilla cubes (notably sheep's fleece) keep their original
            // min/max fields while their rendered skin expands. Select the actual
            // side plane, rather than requiring it to equal the undeformed cube.
            float plane=vertices[0][8],middle=(cube.minX+cube.maxX)*.5F;
            for(var v:vertices)if(!Float.isFinite(v[8])||!Float.isFinite(v[9])||!Float.isFinite(v[10])
                    ||Math.abs(v[8]-plane)>.003F){original();return;}
            if(inward>0?plane>=middle:plane<=middle){original();return;}
            float lowY=Float.POSITIVE_INFINITY,highY=-lowY,lowZ=lowY,highZ=-lowY;
            for(var v:vertices){lowY=Math.min(lowY,v[9]);highY=Math.max(highY,v[9]);lowZ=Math.min(lowZ,v[10]);highZ=Math.max(highZ,v[10]);}
            if(highY-lowY<.001F||highZ-lowZ<.001F){original();return;}
            float[] ys={lowY,clamp(y0,lowY,highY),clamp(y0+(y1-y0)*.25F,lowY,highY),
                    clamp(y0+(y1-y0)*.75F,lowY,highY),clamp(y1,lowY,highY),highY};
            float[] zs={lowZ,clamp(z0,lowZ,highZ),clamp(z0+(z1-z0)*.16F,lowZ,highZ),
                    clamp(z0+(z1-z0)*.82F,lowZ,highZ),clamp(z1,lowZ,highZ),highZ};
            float orientation=(vertices[1][9]-vertices[0][9])*(vertices[3][10]-vertices[0][10])
                    -(vertices[1][10]-vertices[0][10])*(vertices[3][9]-vertices[0][9]);
            for(int y=0;y<ys.length-1;y++)for(int z=0;z<zs.length-1;z++){
                if(ys[y+1]-ys[y]<.001F||zs[z+1]-zs[z]<.001F||missing((ys[y]+ys[y+1])*.5F,(zs[z]+zs[z+1])*.5F))continue;
                emit(ys[y],zs[z]);
                if(orientation>=0){emit(ys[y+1],zs[z]);emit(ys[y+1],zs[z+1]);emit(ys[y],zs[z+1]);}
                else{emit(ys[y],zs[z+1]);emit(ys[y+1],zs[z+1]);emit(ys[y+1],zs[z]);}
            }
        }
        private void original(){for(var v:vertices)out.vertex(v[0],v[1],v[2],((color >> 16) & 255)/255F,((color >> 8) & 255)/255F,(color & 255)/255F,((color >>> 24) & 255)/255F,v[3],v[4],overlay,light,v[5],v[6],v[7]);}
        private void emit(float y,float z){
            float[] a=vertices[0],b=vertices[1],d=vertices[3];
            float by=b[9]-a[9],bz=b[10]-a[10],dy=d[9]-a[9],dz=d[10]-a[10];
            float s=((y-a[9])*by+(z-a[10])*bz)/(by*by+bz*bz);
            float t=((y-a[9])*dy+(z-a[10])*dz)/(dy*dy+dz*dz);
            out.vertex(a[0]+s*(b[0]-a[0])+t*(d[0]-a[0]),a[1]+s*(b[1]-a[1])+t*(d[1]-a[1]),a[2]+s*(b[2]-a[2])+t*(d[2]-a[2]),((color >> 16) & 255)/255F,((color >> 8) & 255)/255F,(color & 255)/255F,((color >>> 24) & 255)/255F,a[3]+s*(b[3]-a[3])+t*(d[3]-a[3]),a[4]+s*(b[4]-a[4])+t*(d[4]-a[4]),overlay,light,a[5],a[6],a[7]);
        }
        // Cube.compile uses the packed overload above. These methods preserve a
        // compatible forwarding consumer for renderers which use the chained API.
        public void endVertex(){if(out!=null)out.endVertex();}
        public void defaultColor(int r,int g,int b,int a){if(out!=null)out.defaultColor(r,g,b,a);}
        public void unsetDefaultColor(){if(out!=null)out.unsetDefaultColor();}
        @Override public VertexConsumer vertex(double x,double y,double z){out.vertex(x,y,z);return this;}
        @Override public VertexConsumer color(int r,int g,int b,int a){out.color(r,g,b,a);return this;}
        @Override public VertexConsumer uv(float u,float v){out.uv(u,v);return this;}
        @Override public VertexConsumer overlayCoords(int u,int v){out.overlayCoords(u,v);return this;}
        @Override public VertexConsumer uv2(int u,int v){out.uv2(u,v);return this;}
        @Override public VertexConsumer normal(float x,float y,float z){out.normal(x,y,z);return this;}
    }
    private static float clamp(float value,float low,float high){return Math.max(low,Math.min(high,value));}
}
