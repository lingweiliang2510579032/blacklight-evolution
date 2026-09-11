package com.codex.blacklight.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/** Camera-facing impact card that expands and fades without adding world geometry. */
public final class ExpandingImpactParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final float initialSize;
    private final float expansion;
    private final float initialAlpha;

    private ExpandingImpactParticle(ClientLevel level, double x, double y, double z,
                                    double xSpeed, double ySpeed, double zSpeed,
                                    SpriteSet sprites, Profile profile) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.sprites = sprites;
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.friction = profile.friction;
        this.gravity = profile.gravity;
        this.hasPhysics = profile.hasPhysics;
        this.lifetime = profile.lifetime + random.nextInt(3);
        this.initialSize = profile.size * (0.86F + random.nextFloat() * 0.28F);
        this.quadSize = initialSize;
        this.expansion = profile.expansion;
        this.initialAlpha = profile.alpha;
        this.alpha = initialAlpha;
        this.roll = random.nextFloat() * ((float)Math.PI * 2.0F);
        this.oRoll = roll;
        setColor(profile.red, profile.green, profile.blue);
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) return;
        setSpriteFromAge(sprites);
        float progress = age / (float)lifetime;
        quadSize = initialSize * (1.0F + progress * expansion);
        alpha = initialAlpha * (1.0F - progress) * (1.0F - progress);
        oRoll = roll;
        roll += 0.015F * (1.0F - progress);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    private enum Profile {
        BIOMASS_SLASH(0.68F, 0.012F, 0.018F, 0.92F,
                0.86F, 0.0F, false, 8, 0.82F, 0.72F),
        DESTRUCTION_BURST(0.34F, 0.29F, 0.27F, 0.88F,
                0.78F, 0.16F, true, 11, 0.72F, 1.25F),
        IMPACT_SMOKE(0.055F, 0.038F, 0.042F, 0.72F,
                0.92F, -0.025F, false, 18, 0.88F, 1.65F);

        private final float red;
        private final float green;
        private final float blue;
        private final float alpha;
        private final float friction;
        private final float gravity;
        private final boolean hasPhysics;
        private final int lifetime;
        private final float size;
        private final float expansion;

        Profile(float red, float green, float blue, float alpha,
                float friction, float gravity, boolean hasPhysics,
                int lifetime, float size, float expansion) {
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.alpha = alpha;
            this.friction = friction;
            this.gravity = gravity;
            this.hasPhysics = hasPhysics;
            this.lifetime = lifetime;
            this.size = size;
            this.expansion = expansion;
        }
    }

    private abstract static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final Profile profile;

        private Provider(SpriteSet sprites, Profile profile) {
            this.sprites = sprites;
            this.profile = profile;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new ExpandingImpactParticle(level, x, y, z,
                    xSpeed, ySpeed, zSpeed, sprites, profile);
        }
    }

    public static final class SlashProvider extends Provider {
        public SlashProvider(SpriteSet sprites) {
            super(sprites, Profile.BIOMASS_SLASH);
        }
    }

    public static final class DestructionProvider extends Provider {
        public DestructionProvider(SpriteSet sprites) {
            super(sprites, Profile.DESTRUCTION_BURST);
        }
    }

    public static final class SmokeProvider extends Provider {
        public SmokeProvider(SpriteSet sprites) {
            super(sprites, Profile.IMPACT_SMOKE);
        }
    }
}
