package com.codex.blacklight.client.particle;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/** A world-space horizontal ring; unlike a normal particle it never billboards to the camera. */
public final class GroundShockwaveParticle extends TextureSheetParticle {

    private final SpriteSet sprites;
    private final float initialSize;

    private GroundShockwaveParticle(ClientLevel level, double x, double y, double z,
                                    SpriteSet sprites) {
        super(level, x, y + 0.025D, z);
        this.sprites = sprites;
        this.hasPhysics = false;
        this.lifetime = 10;
        this.initialSize = 0.56F + random.nextFloat() * 0.10F;
        this.quadSize = initialSize;
        this.alpha = 0.92F;
        this.roll = random.nextFloat() * ((float)Math.PI * 2.0F);
        this.oRoll = roll;
        setColor(0.78F, 0.73F, 0.74F);
        setSpriteFromAge(sprites);
    }

    @Override
    public void render(com.mojang.blaze3d.vertex.VertexConsumer out, Camera camera, float partialTick) {
        float px=(float)(net.minecraft.util.Mth.lerp(partialTick,xo,x)-camera.getPosition().x);
        float py=(float)(net.minecraft.util.Mth.lerp(partialTick,yo,y)-camera.getPosition().y);
        float pz=(float)(net.minecraft.util.Mth.lerp(partialTick,zo,z)-camera.getPosition().z);
        var rotation=new org.joml.Quaternionf().rotationX(-(float)Math.PI*.5F).rotateZ(net.minecraft.util.Mth.lerp(partialTick,oRoll,roll));
        org.joml.Vector3f[] points={new org.joml.Vector3f(-1,-1,0),new org.joml.Vector3f(-1,1,0),new org.joml.Vector3f(1,1,0),new org.joml.Vector3f(1,-1,0)};
        float size=getQuadSize(partialTick);int light=getLightColor(partialTick);
        float[] us={getU1(),getU1(),getU0(),getU0()},vs={getV1(),getV0(),getV0(),getV1()};
        for(int i=0;i<4;i++){
            points[i].rotate(rotation).mul(size).add(px,py,pz);
            out.vertex(points[i].x,points[i].y,points[i].z).uv(us[i],vs[i]).color(rCol,gCol,bCol,alpha).uv2(light).endVertex();
        }
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (++age >= lifetime) {
            remove();
            return;
        }
        setSpriteFromAge(sprites);
        float progress = age / (float)lifetime;
        float eased = 1.0F - (1.0F - progress) * (1.0F - progress);
        quadSize = initialSize * (1.0F + eased * 2.65F);
        alpha = (1.0F - progress) * (1.0F - progress) * 0.92F;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new GroundShockwaveParticle(level, x, y, z, sprites);
        }
    }
}
