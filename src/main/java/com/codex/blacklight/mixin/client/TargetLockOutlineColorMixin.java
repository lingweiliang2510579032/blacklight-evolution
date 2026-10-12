package com.codex.blacklight.mixin.client;

import com.codex.blacklight.client.ClientTargetOutline;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelRenderer.class)
public abstract class TargetLockOutlineColorMixin {
    @WrapOperation(method="renderLevel",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;getTeamColor()I"))
    private int blacklight$lockOutlineColor(Entity entity,Operation<Integer> original) {
        return ClientTargetOutline.matches(entity)?ClientTargetOutline.COLOR:original.call(entity);
    }
}
