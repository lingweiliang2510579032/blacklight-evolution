package com.codex.blacklight.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Uses Minecraft's actual equip/swing arm transform, rather than duplicating it. */
@Mixin(ItemInHandRenderer.class)
public interface NativeHandRendererAccess {
    @Accessor("offHandHeight") float blacklight$offHandHeight();
    @Accessor("oOffHandHeight") float blacklight$oldOffHandHeight();
    @Invoker("applyItemArmTransform")
    void blacklight$applyItemArmTransform(PoseStack stack,HumanoidArm side,float equip);
    @Invoker("renderPlayerArm")
    void blacklight$renderPlayerArm(PoseStack stack,MultiBufferSource buffers,int light,float equip,float swing,HumanoidArm side);
}
