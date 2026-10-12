package com.codex.blacklight.mixin.client;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.Map;
import java.util.List;
@Mixin(ModelPart.class)
public interface ModelPartChildrenAccessor {
    @Accessor("children") Map<String,ModelPart> blacklight$children();
    @Accessor("cubes") List<ModelPart.Cube> blacklight$cubes();
}
