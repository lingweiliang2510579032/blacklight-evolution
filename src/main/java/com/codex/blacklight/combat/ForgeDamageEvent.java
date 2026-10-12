package com.codex.blacklight.combat;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Event;

/** Notification after positive health loss, matching NeoForge's Post observation stage. */
public abstract class ForgeDamageEvent extends Event {
    private final LivingEntity entity;
    private final DamageSource source;
    private final float damage;
    private ForgeDamageEvent(LivingEntity entity,DamageSource source,float damage){this.entity=entity;this.source=source;this.damage=damage;}
    public LivingEntity getEntity(){return entity;}
    public DamageSource getSource(){return source;}
    public float getNewDamage(){return damage;}
    public static final class Post extends ForgeDamageEvent {
        public Post(LivingEntity entity,DamageSource source,float damage){super(entity,source,damage);}
    }
}
