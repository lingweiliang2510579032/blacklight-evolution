package com.codex.blacklight.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface PrototypeCameraAccess {
    @Invoker("setPosition") void blacklight$setPosition(Vec3 position);
    @Invoker("getMaxZoom") float blacklight$getMaxZoom(float distance);
    @Invoker("move") void blacklight$move(float distance,float vertical,float horizontal);
    @Invoker("setRotation") void blacklight$setRotation(float yaw,float pitch,float roll);
}
