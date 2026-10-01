package com.codex.blacklight.mixin;

import com.codex.blacklight.entity.BlacklightInfectedHost;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/** A converted natural dragon remains the same End encounter, including after reload. */
@Mixin(EndDragonFight.class)
public abstract class InfectedDragonFightMixin {
    @Shadow @Final private ServerLevel level;
    @Shadow private UUID dragonUUID;
    @Shadow private boolean dragonKilled;
    @Shadow private int ticksSinceDragonSeen;

    @Inject(method={"scanState","findOrCreateDragon"},at=@At("HEAD"),cancellable=true)
    private void blacklight$findConvertedDragon(CallbackInfo ci) {
        for(var entity:level.getAllEntities()) {
            if(entity instanceof BlacklightInfectedHost host&&host.isAlive()&&host.dragonFightId()!=null
                    &&(dragonUUID==null||dragonUUID.equals(host.dragonFightId()))) {
                dragonUUID=host.dragonFightId();dragonKilled=false;ticksSinceDragonSeen=0;
                host.refreshDragonFight();ci.cancel();return;
            }
        }
    }
}
