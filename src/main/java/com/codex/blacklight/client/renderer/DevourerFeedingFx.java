package com.codex.blacklight.client.renderer;

import com.codex.blacklight.entity.BlacklightInfectedHost;
import com.codex.blacklight.infection.HiveFeedingMotion;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Feeding tissue follows the server's real victim and capture clock, including growing hosts. */
public final class DevourerFeedingFx {
    private static java.lang.ref.WeakReference<net.minecraft.client.multiplayer.ClientLevel> indexedLevel = new java.lang.ref.WeakReference<>(null);
    private static long indexedTick = Long.MIN_VALUE;
    private static final java.util.Map<Integer, Integer> CONSUMERS = new java.util.HashMap<>();
    private DevourerFeedingFx() {}

    /** Called once around the victim's complete model, keeping its torso at the tether endpoint. */
    private static BlacklightInfectedHost consumer(LivingEntity victim) {
        if (!(victim.level() instanceof net.minecraft.client.multiplayer.ClientLevel level)
                || level.getEntity(victim.getId()) != victim) return null; // Native appearance proxies are already scaled by their host.
        if (indexedLevel.get() != level || indexedTick != level.getGameTime()) {
            CONSUMERS.clear(); indexedLevel = new java.lang.ref.WeakReference<>(level); indexedTick = level.getGameTime();
            for (var candidate : level.entitiesForRendering())
                if (candidate instanceof BlacklightInfectedHost host && host.devourerIdentity() && host.feedingTargetId() >= 0)
                    CONSUMERS.put(host.feedingTargetId(), host.getId());
        }
        Integer id = CONSUMERS.get(victim.getId());
        if (id == null || !(level.getEntity(id) instanceof BlacklightInfectedHost host)
                || host.feedingTargetId() != victim.getId() || host.feedingStarted() < 0 || !host.isAlive()) return null;
        return host;
    }

    public static boolean shrinkingVictim(LivingEntity victim, float partial) {
        var host = consumer(victim);
        if (host == null) return false;
        float age = victim.level().getGameTime() - host.feedingStarted() + partial;
        return age >= HiveFeedingMotion.PULL_END && age < HiveFeedingMotion.END;
    }

    public static void applyVictim(LivingEntity victim, float partial, PoseStack stack) {
        var host = consumer(victim);
        if (host == null) return;
        float age = victim.level().getGameTime() - host.feedingStarted() + partial;
        if (age < HiveFeedingMotion.PULL_END || age >= HiveFeedingMotion.END) return;
        float size = (float) contraction(age);
        double torso = victim.getBbHeight() * .55;
        stack.translate(0, torso, 0); stack.scale(size, size, size); stack.translate(0, -torso, 0);
    }

    public static void render(BlacklightInfectedHost host, float partial, PoseStack stack,
            MultiBufferSource buffers, int light) {
        if(!host.devourerIdentity()||host.giantBoss()||host.isInvisible()||!host.nurseryData().contains("Home"))return;
        Vec3 base=host.getPosition(partial);
        var home=host.nurseryHome();int mask=host.nurseryData().getInt("Nodes");
        boolean supplied=host.nurseryData().getBoolean("Supplied");
        float age=host.feedingStarted()<0?-1:host.level().getGameTime()-host.feedingStarted()+partial;
        double pulse=Math.sin((host.tickCount+partial)*.10)*.04;
        var out=buffers.getBuffer(RenderType.entityCutoutNoCull(ConsumedMobVisuals.WHITE));
        for(int i=0;i<4;i++)if((mask&(1<<i))!=0){
            Vec3 mouth=HiveFeedingMotion.mouth(home,i).subtract(base);
            boolean feeding=i==host.feedingNode()&&age>=0&&age<HiveFeedingMotion.END+16;
            double open=feeding?HiveFeedingMotion.smooth(age/20)*(1-HiveFeedingMotion.smooth((age-120)/16)):0;
            mouth(stack,out,light,mouth,open,supplied?pulse:0);
            // Surface vessels connect the stationary ports to the incubating body.
            Vec3 root=Vec3.atBottomCenterOf(home).subtract(base).add(0,.12,0);
            Vec3 rim=mouth.add(0,-1.95,0),last=rim;
            for(int j=1;j<=20;j++){
                double t=j/20.0;
                Vec3 next=rim.lerp(root,t).add(0,Math.sin(t*Math.PI)*.13,0);
                ConsumedMobVisuals.tube(stack,out,light,last,next,.10+(supplied?pulse*.4:0),0xff492229);last=next;
            }
            if(feeding&&age>=HiveFeedingMotion.PULL_END){
                double flow=((age-HiveFeedingMotion.PULL_END)/16)%1;
                Vec3 point=rim.lerp(root,flow),along=root.subtract(rim).normalize().scale(.22);
                ConsumedMobVisuals.tube(stack,out,light,point.subtract(along),point.add(along),.20,0xff9e3a42);
            }
        }
        if(host.feedingNode()<0||age<4||age>=HiveFeedingMotion.END||!supplied)return;
        if(!(host.level().getEntity(host.feedingTargetId()) instanceof LivingEntity prey)||!prey.isAlive())return;
        Vec3 mouth=HiveFeedingMotion.mouth(home,host.feedingNode()).subtract(base);
        Vec3 target=prey.getPosition(partial).subtract(base).add(0,prey.getBbHeight()*.55,0);
        if(target.distanceToSqr(mouth)>28*28)return;
        Vec3 forward=target.subtract(mouth).multiply(1,0,1).normalize();
        if(forward.lengthSqr()<.1)forward=new Vec3(0,0,1);
        Vec3 right=new Vec3(forward.z,0,-forward.x);
        double reach=HiveFeedingMotion.smooth((age-4)/16),pull=HiveFeedingMotion.smooth((age-38)/58);
        double contraction=contraction(age),preyRadius=net.minecraft.util.Mth.clamp(prey.getBbWidth()*.56,.18,2.5)*contraction;
        for(int strand=0;strand<6;strand++){
            double a=strand*Math.PI/3;
            Vec3 radial=right.scale(Math.cos(a)).add(0,Math.sin(a),0);
            Vec3 start=mouth.add(radial.scale(.25)),end=target.add(radial.scale(preyRadius));
            Vec3 bend=radial.scale(.25*(1-.7*pull));
            Vec3 c1=start.lerp(end,.28).add(bend),c2=start.lerp(end,.75).add(bend.scale(.65));
            Vec3 last=start;
            for(int segment=1;segment<=24;segment++){
                Vec3 next=curve(start,c1,c2,end,segment/24.0*reach);
                ConsumedMobVisuals.tube(stack,out,light,last,next,.085*(1-segment/24.0*.45),strand%3==0?0xff722b32:0xff302128);
                last=next;
            }
            if(age>=HiveFeedingMotion.CONTACT){
                Vec3 previous=end;
                for(int j=1;j<=16;j++){
                    double t=j/16.0,angle=a+t*Math.PI*1.8;
                    Vec3 coil=right.scale(Math.cos(angle)).add(forward.scale(Math.sin(angle))).scale(preyRadius);
                    Vec3 next=target.add(coil).add(0,(t-.5)*prey.getBbHeight()*.55*contraction,0);
                    ConsumedMobVisuals.tube(stack,out,light,previous,next,.06,0xff52232b);previous=next;
                }
            }
            if(age>=HiveFeedingMotion.PULL_END){
                double flow=1-((age-HiveFeedingMotion.PULL_END)*.07+strand/6.0)%1;
                Vec3 point=curve(start,c1,c2,end,flow),along=end.subtract(start).normalize().scale(.13);
                ConsumedMobVisuals.tube(stack,out,light,point.subtract(along),point.add(along),.15,0xff9a3d43);
            }
        }
    }
    private static void mouth(PoseStack stack,com.mojang.blaze3d.vertex.VertexConsumer out,int light,Vec3 center,double open,double pulse){
        // Stepped square jaws keep the Minecraft silhouette; the dark lumen sits above the node.
        cube(stack,out,light,center.add(0,-.35,0),1.45,.35,1.45,0xff25181e);
        for(int side=0;side<4;side++){
            double a=side*Math.PI/2,offset=.60+open*.25+pulse;
            Vec3 p=center.add(Math.cos(a)*offset,-.18-open*.13,Math.sin(a)*offset);
            cube(stack,out,light,p,side%2==0?.45:1.45,.60,side%2==0?1.45:.45,0xff6b2b33);
            for(int tooth=-1;tooth<=1;tooth++){
                Vec3 t=p.add(side%2==0?-.16*Math.cos(a):tooth*.36,.28,side%2==0?tooth*.36:-.16*Math.sin(a));
                cube(stack,out,light,t,.18,.30-open*.12,.18,0xffb1a091);
            }
            Vec3 root=center.add(Math.cos(a)*.6,-1.85,Math.sin(a)*.6);
            ConsumedMobVisuals.tube(stack,out,light,root,p,.28,0xff3b252b);
        }
    }
    private static void cube(PoseStack stack,com.mojang.blaze3d.vertex.VertexConsumer out,int light,Vec3 c,double x,double y,double z,int color){
        double[][] v={{-x,-y,-z},{x,-y,-z},{x,y,-z},{-x,y,-z},{-x,-y,z},{x,-y,z},{x,y,z},{-x,y,z}};
        int[][] faces={{0,3,2,1},{4,5,6,7},{0,4,7,3},{1,2,6,5},{0,1,5,4},{3,7,6,2}};
        float[][] normals={{0,0,-1},{0,0,1},{-1,0,0},{1,0,0},{0,-1,0},{0,1,0}};
        for(int f=0;f<6;f++)for(int j:faces[f])out.vertex((stack.last()).pose(),(float)(c.x+v[j][0]*.5),(float)(c.y+v[j][1]*.5),(float)(c.z+v[j][2]*.5))
                .color(color).uv(.5F,.5F).overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(light)
                .normal((stack.last()).normal(),normals[f][0],normals[f][1],normals[f][2]).endVertex();
    }

    private static Vec3 curve(Vec3 a, Vec3 b, Vec3 c, Vec3 d, double t) {
        double s = 1 - t;
        return a.scale(s * s * s).add(b.scale(3 * s * s * t)).add(c.scale(3 * s * t * t)).add(d.scale(t * t * t));
    }
    private static double smooth(double t) { t = net.minecraft.util.Mth.clamp(t, 0, 1); return t * t * (3 - 2 * t); }
    private static double contraction(float age) { return HiveFeedingMotion.contraction(age); }
}
