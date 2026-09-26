package com.codex.blacklight.client.renderer;

import com.codex.blacklight.entity.BlacklightInfectedHost;
import com.codex.blacklight.mixin.client.ModelPartChildrenAccessor;
import com.google.gson.*;
import com.mojang.blaze3d.vertex.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/** Authored Minecraft cuboids attached to actual posed host parts. */
public final class InfectedMorphology implements AutoCloseable {
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("blacklight","textures/entity/infected_morphology.png");
    private static final RenderType TYPE = RenderType.entityCutoutNoCull(TEXTURE);
    private static final ThreadLocal<InfectedMorphology> ACTIVE = new ThreadLocal<>();
    private static final Map<EntityModel<?>,Map<String,Map<ModelPart,Attachment>>> CACHE = new WeakHashMap<>();
    private static final JsonObject DEFINITIONS = definitions();
    private record Bounds(float x,float y,float z,float w,float h,float d) {
        float volume(){return w*h*d;}
    }
    private record Box(float x,float y,float z,float w,float h,float d,int material) { }
    private record Attachment(int anatomy,List<Box> boxes) { }
    private record Pending(Attachment attachment,PoseStack.Pose pose,int light,int overlay,int color) { }
    private final LivingEntity proxy;
    private final InfectedMorphology previous;
    private final Map<ModelPart,Attachment> attachments;
    private final Map<ModelPart,Pending> pending = new IdentityHashMap<>();

    private static JsonObject definitions(){
        try(var stream=InfectedMorphology.class.getResourceAsStream("/assets/blacklight/models/infected_morphology.json")){
            if(stream==null)throw new IllegalStateException("Missing authored infection morphology");
            return JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
        }catch(Exception e){throw new IllegalStateException("Cannot load infection morphology",e);}
    }
    public InfectedMorphology(BlacklightInfectedHost host,LivingEntity proxy,EntityModel<?> model){
        this.proxy=proxy;previous=ACTIVE.get();
        String type=host.hostData().getString("Type");
        var species=DEFINITIONS.getAsJsonObject("species");
        String profile=species.has(type)?species.get(type).getAsString():
                host.family().equals("air")?"winged":host.family().equals("water")?"aquatic":
                host.humanoid()?"humanoid":host.quadruped()?"bovine":"generic";
        attachments=CACHE.computeIfAbsent(model,k->new HashMap<>()).computeIfAbsent(profile,k->bind(model,k));
        ACTIVE.set(this);
    }
    @Override public void close(){pending.clear();if(previous==null)ACTIVE.remove();else ACTIVE.set(previous);}
    private static Bounds bounds(ModelPart part){
        var cubes=((ModelPartChildrenAccessor)(Object)part).blacklight$cubes();
        if(cubes.isEmpty())return null;
        float x=Float.POSITIVE_INFINITY,y=x,z=x,X=-x,Y=-x,Z=-x;
        for(var c:cubes){x=Math.min(x,c.minX);y=Math.min(y,c.minY);z=Math.min(z,c.minZ);X=Math.max(X,c.maxX);Y=Math.max(Y,c.maxY);Z=Math.max(Z,c.maxZ);}
        return new Bounds(x,y,z,Math.max(.5F,X-x),Math.max(.5F,Y-y),Math.max(.5F,Z-z));
    }
    private static Map<ModelPart,Attachment> bind(EntityModel<?> model,String profile){
        var anatomy=ConsumeAnatomy.model(model);Map<Integer,ModelPart> largest=new HashMap<>();
        for(var entry:anatomy.entrySet()){
            Bounds b=bounds(entry.getKey());if(b==null)continue;
            ModelPart old=largest.get(entry.getValue());
            if(old==null||b.volume()>bounds(old).volume())largest.put(entry.getValue(),entry.getKey());
        }
        // The humanoid hat may have the same cube size as the head but be hidden.
        if(model instanceof HumanoidModel<?> h){largest.put(ConsumeAnatomy.HEAD,h.head);largest.put(ConsumeAnatomy.BODY,h.body);}
        var definition=DEFINITIONS.getAsJsonObject("profiles").getAsJsonObject(profile);
        Map<ModelPart,Attachment> result=new IdentityHashMap<>();
        for(var entry:largest.entrySet()){
            String role=switch(entry.getKey()){case ConsumeAnatomy.BODY->"body";case ConsumeAnatomy.HEAD->"head";case ConsumeAnatomy.TAIL->"tail";default->"limb";};
            Bounds b=bounds(entry.getValue());if(b==null)continue;
            boolean horizontalBody=entry.getKey()==ConsumeAnatomy.BODY
                    && Set.of("bovine","porcine","woolly","canine","feline","equine","small_quadruped").contains(profile)
                    && Math.abs(Math.sin(entry.getValue().getInitialPose().xRot))<.5;
            result.put(entry.getValue(),attachment(b,entry.getKey(),definition.getAsJsonArray(role),horizontalBody));
        }
        // Spiders have eight articulated legs, not four humanoid limb slots.
        if(profile.equals("arthropod"))for(var entry:anatomy.entrySet()){
            if(entry.getValue()<ConsumeAnatomy.RIGHT_ARM||entry.getValue()>ConsumeAnatomy.LEFT_LEG)continue;
            Bounds b=bounds(entry.getKey());if(b!=null)result.put(entry.getKey(),attachment(b,entry.getValue(),definition.getAsJsonArray("limb"),false));
        }
        return result;
    }
    private static Attachment attachment(Bounds b,int anatomy,JsonArray source,boolean horizontalBody){
        List<Box> boxes=new ArrayList<>();
        for(var value:source){
            var o=value.getAsJsonObject();var c=o.getAsJsonArray("center");var s=o.getAsJsonArray("size");
            int material=DEFINITIONS.getAsJsonObject("materials").get(o.get("material").getAsString()).getAsInt();
            // Keep small fish/wing details small, while preserving zero-thickness
            // vanilla wing planes as visible but restrained block struts.
            float cy=c.get(1).getAsFloat(),cz=c.get(2).getAsFloat(),sy=s.get(1).getAsFloat(),sz=s.get(2).getAsFloat();
            // Cow/pig bodies bake a quarter turn; horse/hoglin bodies often
            // model the long axis directly in Z. The ridge must stay dorsal.
            if(horizontalBody){float old=cy;cy=1-cz;cz=old;old=sy;sy=sz;sz=old;}
            boxes.add(new Box(b.x+c.get(0).getAsFloat()*b.w,b.y+cy*b.h,b.z+cz*b.d,
                    Math.max(.18F,s.get(0).getAsFloat()*b.w),Math.max(.18F,sy*b.h),Math.max(.18F,sz*b.d),material));
        }
        return new Attachment(anatomy,List.copyOf(boxes));
    }
    /** Called inside ModelPart.compile, after its complete animated transform. */
    public static void capture(ModelPart part,PoseStack.Pose pose,int light,int overlay,int color){
        var context=ACTIVE.get();if(context==null||context.pending.containsKey(part))return;
        Attachment attachment=context.attachments.get(part);if(attachment==null||attachment.boxes.isEmpty())return;
        PoseStack copy=new PoseStack();copy.last().pose().set(pose.pose());copy.last().normal().set(pose.normal());
        context.pending.put(part,new Pending(attachment,copy.last(),light,overlay,color));
    }
    /** Flush before the current renderer's consume fracture wrapper finishes.
     * Deferred emission avoids switching a live vanilla BufferBuilder midway
     * through its model; context never mutates the shared species skeleton. */
    public static void flush(LivingEntity entity,MultiBufferSource buffers){
        var context=ACTIVE.get();if(context==null||context.proxy!=entity||context.pending.isEmpty())return;
        var out=buffers.getBuffer(TYPE);
        for(var p:context.pending.values()){
            if(out instanceof ConsumeAnatomy.Consumer consumer)consumer.anatomy(p.attachment.anatomy);
            for(var box:p.attachment.boxes)draw(box,p,out);
        }
        context.pending.clear();
    }
    private static final int[][] FACES={{0,3,2,1},{4,5,6,7},{1,2,6,5},{0,4,7,3},{3,7,6,2},{0,1,5,4}};
    private static final float[][] NORMALS={{0,0,-1},{0,0,1},{1,0,0},{-1,0,0},{0,1,0},{0,-1,0}};
    private static final int[][] CORNERS={{-1,-1,-1},{1,-1,-1},{1,1,-1},{-1,1,-1},{-1,-1,1},{1,-1,1},{1,1,1},{-1,1,1}};
    private static void draw(Box b,Pending p,VertexConsumer out){
        float u=(b.material*16+.25F)/64F,v=.25F/64F,span=15.5F/64F;
        for(int face=0;face<6;face++)for(int corner=0;corner<4;corner++){
            int[] c=CORNERS[FACES[face][corner]];float[] n=NORMALS[face];
            out.addVertex(p.pose,(b.x+c[0]*b.w*.5F)/16,(b.y+c[1]*b.h*.5F)/16,(b.z+c[2]*b.d*.5F)/16)
                    .setColor(p.color).setUv(u+(corner>=2?span:0),v+(corner==0||corner==3?span:0))
                    .setOverlay(p.overlay).setLight(p.light).setNormal(p.pose,n[0],n[1],n[2]);
        }
    }
}
