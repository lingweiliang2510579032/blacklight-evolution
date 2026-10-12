package com.codex.blacklight.mixin.client;

import com.codex.blacklight.client.camera.PrototypeChaseCamera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(GameRenderer.class)
public abstract class PrototypeCameraProjectionMixin {
    @ModifyArg(method="getProjectionMatrix",at=@At(value="INVOKE",target="Lorg/joml/Matrix4f;perspective(FFFF)Lorg/joml/Matrix4f;"),index=2)
    private float blacklight$near(float value){return PrototypeChaseCamera.ownsView()?PrototypeChaseCamera.nearPlane():value;}
    @ModifyArg(method="getProjectionMatrix",at=@At(value="INVOKE",target="Lorg/joml/Matrix4f;perspective(FFFF)Lorg/joml/Matrix4f;"),index=3)
    private float blacklight$far(float value){return PrototypeChaseCamera.ownsView()?PrototypeChaseCamera.farPlane(value):value;}
}
