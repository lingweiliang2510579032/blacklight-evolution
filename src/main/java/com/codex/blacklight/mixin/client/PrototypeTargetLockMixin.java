package com.codex.blacklight.mixin.client;

import com.codex.blacklight.client.ClientTargetLock;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.api.client.camera.EpicFightCameraAPI;

@Mixin(value=EpicFightCameraAPI.class,remap=false)
public abstract class PrototypeTargetLockMixin {
    @Shadow private boolean lockingOnTarget;
    @Shadow private LivingEntity focusingEntity;
    @Unique private int blacklight$explicitShift;

    @WrapMethod(method="setNextLockOnTarget(IZZ)Z")
    private boolean blacklight$allowDeliberateShift(int direction, boolean checkLock, boolean auto, Operation<Boolean> original){
        blacklight$explicitShift++;
        try{return original.call(direction,checkLock,auto);}
        finally{blacklight$explicitShift--;}
    }
    @Inject(method="setFocusingEntity",at=@At("HEAD"),cancellable=true)
    private void blacklight$retainExplicitLock(LivingEntity requested,CallbackInfo callback){
        if(ClientTargetLock.active() && lockingOnTarget && requested!=null && requested!=focusingEntity
                && ClientTargetLock.valid(focusingEntity) && blacklight$explicitShift==0
                && !yesman.epicfight.api.client.input.InputManager.isActionActive(
                    yesman.epicfight.api.client.input.action.EpicFightInputAction.LOCK_ON_SHIFT_FREELY)) callback.cancel();
    }
    @Inject(method="setFocusingEntity",at=@At("RETURN"))
    private void blacklight$syncFocus(LivingEntity requested,CallbackInfo callback){ClientTargetLock.sync();}
    @Inject(method="setLockOn",at=@At("RETURN"))
    private void blacklight$syncLock(boolean enabled,CallbackInfo callback){ClientTargetLock.sync();}
    @Inject(method="predicateFocusableEntity",at=@At("HEAD"),cancellable=true)
    private void blacklight$filterFocus(Entity entity,CallbackInfoReturnable<Boolean> callback){
        if(ClientTargetLock.active() && entity instanceof LivingEntity living && !ClientTargetLock.valid(living)) callback.setReturnValue(false);
    }
}
