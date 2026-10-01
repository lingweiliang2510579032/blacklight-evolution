package com.codex.blacklight.mixin.client;

import com.codex.blacklight.client.ClientTargetLock;
import com.codex.blacklight.combat.CombatTargeting;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.api.client.camera.EpicFightCameraAPI;

@Mixin(value=EpicFightCameraAPI.class,remap=false)
public abstract class PrototypeTargetLockMixin implements com.codex.blacklight.client.TargetFocusAccess {
    @Shadow private boolean lockingOnTarget;
    @Shadow private LivingEntity focusingEntity;
    @Unique private int blacklight$explicitShift;
    @Invoker(value="setFocusingEntity",remap=false)
    public abstract void blacklight$focusEntity(LivingEntity target);

    @Inject(method="turnCamera",at=@At("HEAD"),cancellable=true)
    private void blacklight$sourceMouse(double x,double y,CallbackInfoReturnable<Boolean> callback){
        if(com.codex.blacklight.client.camera.PrototypeChaseCamera.mouse(x,y))callback.setReturnValue(true);
    }

    @Inject(method="isTPSMode",at=@At("HEAD"),cancellable=true)
    private void blacklight$survivalCamera(CallbackInfoReturnable<Boolean> callback){
        var player=net.minecraft.client.Minecraft.getInstance().player;
        if(player!=null && !com.codex.blacklight.mode.PrototypeModeSystem.isActive(player)) callback.setReturnValue(false);
        else if(com.codex.blacklight.client.camera.PrototypeChaseCamera.ownsView())callback.setReturnValue(true);
    }

    @Inject(method={"getForwardYRot","getCameraYRot"},at=@At("HEAD"),cancellable=true)
    private void blacklight$controlYaw(CallbackInfoReturnable<Float> callback){
        if(com.codex.blacklight.client.camera.PrototypeChaseCamera.ownsView())
            callback.setReturnValue(com.codex.blacklight.client.camera.PrototypeChaseCamera.controlYaw());
    }

    @WrapMethod(method="setNextLockOnTarget(IZZ)Z")
    private boolean blacklight$allowDeliberateShift(int direction, boolean checkLock, boolean auto, Operation<Boolean> original){
        // Free observation must not also run EF's mouse-flick target selection.
        if(auto&&com.codex.blacklight.client.camera.PrototypeChaseCamera.ownsView())return false;
        if(auto)return original.call(direction,checkLock,true);
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
    @Inject(method="setLockOn",at=@At("HEAD"))
    private void blacklight$lockHitPart(boolean enabled,CallbackInfo callback){
        if(enabled)ClientTargetLock.focusHitPart();
    }
    @Inject(method="predicateFocusableEntity",at=@At("HEAD"),cancellable=true)
    private void blacklight$filterFocus(Entity entity,CallbackInfoReturnable<Boolean> callback){
        if(ClientTargetLock.active() && entity instanceof LivingEntity living){
            if(!ClientTargetLock.valid(living))callback.setReturnValue(false);
            // Limb rays resolve to a LivingEntity parent which intentionally is
            // not pickable. Permit it as focus without adding a root hit surface.
            else if(CombatTargeting.multipart(living))callback.setReturnValue(true);
        }
    }

    @WrapOperation(method={"postClientTick","lambda$setNextLockOnTarget$0"},at=@At(value="INVOKE",
            target="Lyesman/epicfight/api/utils/math/MathUtils;canBeSeen(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;D)Z"))
    private boolean blacklight$visibleBody(Entity target,Entity observer,double range,Operation<Boolean> original){
        if(ClientTargetLock.active()&&target instanceof LivingEntity living&&CombatTargeting.multipart(living))
            return ClientTargetLock.visibleInRange(living);
        return original.call(target,observer,range);
    }

    @WrapOperation(method="postClientTick",at=@At(value="INVOKE",
            target="Lnet/minecraft/client/player/LocalPlayer;distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D"))
    private double blacklight$bodyRange(net.minecraft.client.player.LocalPlayer player,net.minecraft.world.phys.Vec3 point,Operation<Double> original){
        return ClientTargetLock.active()&&focusingEntity!=null&&CombatTargeting.multipart(focusingEntity)
                ?CombatTargeting.distanceSquared(player.getEyePosition(),focusingEntity):original.call(player,point);
    }

    @WrapOperation(method="lambda$setNextLockOnTarget$0",at=@At(value="INVOKE",
            target="Lnet/minecraft/world/entity/Entity;distanceToSqr(Lnet/minecraft/world/entity/Entity;)D"))
    private double blacklight$candidateRange(Entity target,Entity observer,Operation<Double> original){
        return ClientTargetLock.active()&&target instanceof LivingEntity living&&CombatTargeting.multipart(living)
                ?CombatTargeting.distanceSquared(observer.getEyePosition(),living):original.call(target,observer);
    }

    @WrapOperation(method="postClientTick",at=@At(value="INVOKE",
            target="Lnet/minecraft/world/entity/LivingEntity;position()Lnet/minecraft/world/phys/Vec3;"))
    private net.minecraft.world.phys.Vec3 blacklight$bodyPosition(LivingEntity target,Operation<net.minecraft.world.phys.Vec3> original){
        return ClientTargetLock.active()&&CombatTargeting.multipart(target)?ClientTargetLock.focusPoint(target):original.call(target);
    }

    @WrapOperation(method="postClientTick",at=@At(value="INVOKE",
            target="Lnet/minecraft/world/entity/LivingEntity;getEyePosition()Lnet/minecraft/world/phys/Vec3;"))
    private net.minecraft.world.phys.Vec3 blacklight$bodyAim(LivingEntity target,Operation<net.minecraft.world.phys.Vec3> original){
        return ClientTargetLock.active()&&CombatTargeting.multipart(target)?ClientTargetLock.focusPoint(target):original.call(target);
    }

    @WrapOperation(method="postClientTick",at=@At(value="INVOKE",
            target="Lnet/minecraft/world/entity/LivingEntity;getBoundingBox()Lnet/minecraft/world/phys/AABB;"))
    private net.minecraft.world.phys.AABB blacklight$bodyFrame(LivingEntity target,Operation<net.minecraft.world.phys.AABB> original){
        return ClientTargetLock.active()&&CombatTargeting.multipart(target)?ClientTargetLock.focusBounds(target):original.call(target);
    }

    @WrapOperation(method="lambda$setNextLockOnTarget$1",at=@At(value="INVOKE",
            target="Lnet/minecraft/world/entity/Entity;getBoundingBox()Lnet/minecraft/world/phys/AABB;"))
    private net.minecraft.world.phys.AABB blacklight$candidateFrame(Entity target,Operation<net.minecraft.world.phys.AABB> original){
        return ClientTargetLock.active()&&target instanceof LivingEntity living&&CombatTargeting.multipart(living)
                ?ClientTargetLock.focusBounds(living):original.call(target);
    }
}
