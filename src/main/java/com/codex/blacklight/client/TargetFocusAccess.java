package com.codex.blacklight.client;

import net.minecraft.world.entity.LivingEntity;

/** Implemented by the existing Epic Fight camera mixin. */
public interface TargetFocusAccess {
    void blacklight$focusEntity(LivingEntity target);
}
