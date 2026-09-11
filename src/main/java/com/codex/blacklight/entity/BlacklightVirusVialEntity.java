package com.codex.blacklight.entity;

import com.codex.blacklight.infection.BlacklightInfectionSystem;
import com.codex.blacklight.registry.ModEntities;
import com.codex.blacklight.registry.ModItems;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

public final class BlacklightVirusVialEntity extends ThrowableItemProjectile {
    public static final double INFECTION_RADIUS = 8.0D;

    public BlacklightVirusVialEntity(EntityType<? extends BlacklightVirusVialEntity> type, Level level) {
        super(type, level);
    }

    public BlacklightVirusVialEntity(Level level, LivingEntity owner) {
        super(ModEntities.BLACKLIGHT_VIRUS_VIAL.get(), owner, level);
    }

    public BlacklightVirusVialEntity(Level level, double x, double y, double z) {
        super(ModEntities.BLACKLIGHT_VIRUS_VIAL.get(), x, y, z, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.BLACKLIGHT_SAMPLE.get();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!(level() instanceof ServerLevel serverLevel)) return;

        Entity owner = getOwner();
        int infected = 0;
        if (owner instanceof LivingEntity livingOwner
                && BlacklightInfectionSystem.infect(livingOwner, this)) {
            infected++;
        }

        for (LivingEntity target : serverLevel.getEntitiesOfClass(
                LivingEntity.class,
                getBoundingBox().inflate(INFECTION_RADIUS),
                target -> target.isAlive() && target.distanceToSqr(this) <= INFECTION_RADIUS * INFECTION_RADIUS)) {
            if (target != owner && BlacklightInfectionSystem.infect(target, this)) infected++;
        }

        serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                getX(), getY() + 0.15D, getZ(), 42, 2.2D, 1.25D, 2.2D, 0.045D);
        serverLevel.sendParticles(ParticleTypes.CRIMSON_SPORE,
                getX(), getY() + 0.3D, getZ(), 120, 3.8D, 1.8D, 3.8D, 0.025D);
        serverLevel.playSound(null, blockPosition(), SoundEvents.GLASS_BREAK,
                SoundSource.PLAYERS, 1.35F, 0.72F);
        serverLevel.playSound(null, blockPosition(), SoundEvents.SCULK_CATALYST_BLOOM,
                SoundSource.HOSTILE, 1.1F, 0.62F);
        serverLevel.broadcastEntityEvent(this, (byte) 3);
        BlacklightInfectionSystem.notifyBurstOwner(owner, infected);
        discard();
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id != 3) return;
        for (int i = 0; i < 28; i++) {
            level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, getItem()),
                    getX(), getY(), getZ(),
                    (random.nextDouble() - 0.5D) * 0.34D,
                    random.nextDouble() * 0.26D,
                    (random.nextDouble() - 0.5D) * 0.34D);
        }
        for (int i = 0; i < 18; i++) {
            level().addParticle(ParticleTypes.SMOKE,
                    getX(), getY() + 0.1D, getZ(),
                    (random.nextDouble() - 0.5D) * 0.18D,
                    random.nextDouble() * 0.12D,
                    (random.nextDouble() - 0.5D) * 0.18D);
        }
    }
}
