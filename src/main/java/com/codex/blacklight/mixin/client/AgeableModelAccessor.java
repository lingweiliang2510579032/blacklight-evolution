package com.codex.blacklight.mixin.client;

import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AgeableListModel.class)
public interface AgeableModelAccessor {
    @Invoker("headParts") Iterable<ModelPart> blacklight$heads();
    @Invoker("bodyParts") Iterable<ModelPart> blacklight$body();
}
