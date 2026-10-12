package com.codex.blacklight.movement;

import net.minecraft.world.entity.player.Player;

/** Keeps one held jump gesture from changing from a ground charge into a glide. */
public final class JumpInputState {
    private boolean held;
    private boolean chargeGesture;
    private int bufferedUntil = -1;
    private int offGroundTicks;

    public void acceptInput(boolean jump, Player player, boolean onWall) {
        if (!jump) {
            chargeGesture = false;
            bufferedUntil = -1;
        } else if (!held) {
            boolean ground = nearGround(player);
            chargeGesture = ground || onWall;
            bufferedUntil = ground && !onWall
                    ? player.tickCount + PrototypeLocomotion.CHARGE_GROUND_GRACE_TICKS : -1;
        }
        held = jump;
    }

    public static boolean nearGround(Player player) {
        double vertical = player.getDeltaMovement().y;
        return player.onGround() || vertical <= .035 && vertical >= -.5
                && TerrainTraversal.supportBelow(player, 1.05);
    }

    public void observeGround(boolean onGround) {
        offGroundTicks = onGround ? 0 : offGroundTicks + 1;
        // Landing while holding an airborne input cannot re-arm glide at the next curb.
        if (onGround && held) chargeGesture = true;
    }

    public boolean consumeChargeRequest(Player player) {
        if (!held || !player.onGround() || bufferedUntil < player.tickCount) return false;
        claimCharge();
        return true;
    }

    public void claimCharge() {
        chargeGesture = true;
        bufferedUntil = -1;
    }

    public void cancelPendingCharge() { bufferedUntil = -1; }

    public boolean canGlide(Player player) {
        return held && !chargeGesture && !player.onGround()
                && offGroundTicks > PrototypeLocomotion.CHARGE_GROUND_GRACE_TICKS
                && player.getDeltaMovement().y < -.02 && !player.horizontalCollision
                && !TerrainTraversal.supportBelow(player, 1.05);
    }

    public void reset() {
        held = false;
        chargeGesture = false;
        bufferedUntil = -1;
        offGroundTicks = 0;
    }
}
