package com.codex.blacklight.mixin;

import com.codex.blacklight.combat.ForgeEntityTickEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Skip the actual update when a captured entity is frozen; keep server traversal intact. */
@Mixin(ServerLevel.class)
public abstract class ServerEntityTickMixin {
    @Redirect(method="tickNonPassenger",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;tick()V"))
    private void blacklight$tick(Entity entity){
        if(MinecraftForge.EVENT_BUS.post(new ForgeEntityTickEvent.Pre(entity)))return;
        entity.tick();
        // tick() can remove the entity and invalidate its capabilities (e.g. deathTime == 20).
        if(!entity.isRemoved())MinecraftForge.EVENT_BUS.post(new ForgeEntityTickEvent.Post(entity));
    }
    @Redirect(method="tickPassenger",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;rideTick()V"))
    private void blacklight$rideTick(Entity entity){
        if(MinecraftForge.EVENT_BUS.post(new ForgeEntityTickEvent.Pre(entity)))return;
        entity.rideTick();
        if(!entity.isRemoved())MinecraftForge.EVENT_BUS.post(new ForgeEntityTickEvent.Post(entity));
    }
}
