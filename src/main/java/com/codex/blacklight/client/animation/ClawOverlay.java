package com.codex.blacklight.client.animation;

import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/** A biological arm extension, never an ItemStack and never rendered in the player's hand slot. */
public final class ClawOverlay implements GeoAnimatable {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    // Game-time source injected by the render host; wall-clock time would keep
    // animations running while paused and drift from the server tick rate.
    private double clientTick;

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    /** Called by the render host once per client frame with game time (not wall clock). */
    public void setClientTick(double tick) {
        this.clientTick = tick;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getTick(Object context) {
        return clientTick;
    }
}
