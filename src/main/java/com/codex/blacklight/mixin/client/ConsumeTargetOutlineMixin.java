package com.codex.blacklight.mixin.client;
import com.codex.blacklight.registry.ModAttachments;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.api.client.camera.EpicFightCameraAPI;

/** A disintegrating receiver is no longer an intact combat target silhouette.
 * This only suppresses its visual guide; the actual camera lock is untouched. */
@Mixin(value=EpicFightCameraAPI.class,remap=false)
public abstract class ConsumeTargetOutlineMixin {
    @Inject(method="shouldHighlightTarget",at=@At("HEAD"),cancellable=true)
    private void blacklight$consumeOutline(Entity entity,CallbackInfoReturnable<Boolean> cir){
        if(entity instanceof LivingEntity living&&living.getData(ModAttachments.CONSUME_VISUAL).consuming())cir.setReturnValue(false);
    }
}
