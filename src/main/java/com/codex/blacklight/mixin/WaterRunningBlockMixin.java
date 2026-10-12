package com.codex.blacklight.mixin;

import com.codex.blacklight.movement.WaterRunning;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LiquidBlock.class)
public abstract class WaterRunningBlockMixin {
    @Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true)
    private void blacklight$surfaceCollision(BlockState state, BlockGetter level, BlockPos pos,
                                             CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
        if (!(context instanceof EntityCollisionContext entityContext)
                || !(entityContext.getEntity() instanceof Player player)
                || !WaterRunning.canRun(player)) return;
        var fluid = state.getFluidState();
        if (!WaterRunning.surface(level, pos, fluid)) return;
        // Use the visible height for source and shallow flowing water. Ordinary solid
        // block collisions remain intact; waterfalls and submerged water stay passable.
        // A tiny separation avoids floating-point water-contact flicker at the feet.
        var shape = Shapes.box(0, 0, 0, 1, fluid.getHeight(level, pos) + .002, 1);
        if (context.isAbove(shape, pos, false)) cir.setReturnValue(shape);
    }
}
