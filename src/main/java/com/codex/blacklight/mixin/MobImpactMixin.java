package com.codex.blacklight.mixin;

import com.codex.blacklight.combat.ImpactControl;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobImpactMixin {
    /** Pause decisions, not entity physics, timers, gravity or saving. */
    @Inject(method="serverAiStep",at=@At("HEAD"),cancellable=true)
    private void blacklight$stun(CallbackInfo ci){if(ImpactControl.stunned((Mob)(Object)this))ci.cancel();}
}
