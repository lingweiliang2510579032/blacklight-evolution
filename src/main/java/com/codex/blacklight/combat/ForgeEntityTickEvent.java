package com.codex.blacklight.combat;

import net.minecraft.world.entity.Entity;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

/** Full server entity update boundaries, including mounted entities. */
public abstract class ForgeEntityTickEvent extends Event {
    private final Entity entity;
    protected ForgeEntityTickEvent(Entity entity) { this.entity=entity; }
    public Entity getEntity() { return entity; }
    @Cancelable public static final class Pre extends ForgeEntityTickEvent { public Pre(Entity entity) { super(entity); } }
    public static final class Post extends ForgeEntityTickEvent { public Post(Entity entity) { super(entity); } }
}
