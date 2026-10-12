package com.codex.blacklight.mixin;

import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Allow large health pools without vanilla's 1024-point clamp truncating bosses. */
@Mixin(RangedAttribute.class)
public interface HealthAttributeLimitAccessor {
    @Mutable @Accessor("maxValue") void blacklight$setMaximum(double value);
}
