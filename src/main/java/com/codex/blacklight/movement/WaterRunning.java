package com.codex.blacklight.movement;

import com.codex.blacklight.registry.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FlowingFluid;

/** Shared collision eligibility: water supports a running player, never a swimmer below it. */
public final class WaterRunning {
    private WaterRunning() { }

    public static boolean canRun(Player player) {
        var state = player.getData(ModAttachments.PLAYER_STATE);
        if (!state.isInfected() || !state.isPrototypeMode() || !player.isAlive()
                || player.isSpectator() || player.isPassenger() || player.isFallFlying()
                || player.getAbilities().flying || player.isShiftKeyDown() || player.isInLava()
                || !player.isSprinting() || player.getDeltaMovement().horizontalDistanceSqr() < .12 * .12
                || player.getData(ModAttachments.WEAPON_VISUAL).spec() != null
                || player.getData(ModAttachments.CONSUMING)) return false;
        BlockPos pos = player.blockPosition();
        FluidState fluid = player.level().getFluidState(pos);
        // A player already submerged must swim up normally; no snapping to the surface.
        return !fluid.is(FluidTags.WATER) || surface(player.level(), pos, fluid)
                && player.getY() >= pos.getY() + fluid.getHeight(player.level(), pos) - .025;
    }

    public static boolean surface(BlockGetter level, BlockPos pos, FluidState fluid) {
        return fluid.is(FluidTags.WATER)
                && !(fluid.hasProperty(FlowingFluid.FALLING) && fluid.getValue(FlowingFluid.FALLING))
                && !level.getFluidState(pos.above()).is(FluidTags.WATER);
    }

    public static boolean onSurface(Player player) {
        if (!canRun(player)) return false;
        BlockPos feet = player.blockPosition();
        for (BlockPos pos : new BlockPos[]{feet, feet.below()}) {
            FluidState fluid = player.level().getFluidState(pos);
            if (surface(player.level(), pos, fluid)) {
                double gap = player.getY() - pos.getY() - fluid.getHeight(player.level(), pos);
                if (gap >= -.025 && gap <= .12) return true;
            }
        }
        return false;
    }
}
