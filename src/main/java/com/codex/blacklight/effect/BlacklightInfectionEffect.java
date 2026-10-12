package com.codex.blacklight.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public final class BlacklightInfectionEffect extends MobEffect {
    public BlacklightInfectionEffect() {
        super(MobEffectCategory.NEUTRAL, 0x680014);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 80 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.getHealth() < entity.getMaxHealth()) {
            entity.heal(0.75F + amplifier * 0.25F);
        }
    }
}
