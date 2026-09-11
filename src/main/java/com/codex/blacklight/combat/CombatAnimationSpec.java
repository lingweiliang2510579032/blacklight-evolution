package com.codex.blacklight.combat;

import java.util.List;

/**
 * Server-authoritative timing contract shared by combat code and the animation layer.
 * Times are expressed in ticks so animation playback never becomes the damage authority.
 */
public record CombatAnimationSpec(
        String id,
        int duration,
        int startupEnd,
        int activeEnd,
        int cancelStart,
        boolean locksMovement
) {
    public static final CombatAnimationSpec CLAW_AUTO_1 =
            new CombatAnimationSpec("claw_auto_1", 14, 4, 7, 10, true);
    public static final CombatAnimationSpec CLAW_AUTO_2 =
            new CombatAnimationSpec("claw_auto_2", 16, 5, 9, 11, true);
    public static final CombatAnimationSpec CLAW_AUTO_3 =
            new CombatAnimationSpec("claw_auto_3", 22, 7, 12, 17, true);
    public static final CombatAnimationSpec CLAW_GROUNDSPIKE =
            new CombatAnimationSpec("claw_groundspike", GroundSpikeRules.PLANT_TICKS,
                    GroundSpikeRules.PLANT_TICKS, GroundSpikeRules.PLANT_TICKS,
                    GroundSpikeRules.PLANT_TICKS, true);
    public static final CombatAnimationSpec CLAW_DASHING_SLICE =
            new CombatAnimationSpec("claw_dashing_slice", 20, 3, 12, 17, false);
    public static final CombatAnimationSpec CLAW_SPRINT_ATTACK =
            new CombatAnimationSpec("claw_sprint_attack", 16, 2, 8, 12, false);
    public static final CombatAnimationSpec TRANSFORM_CLAWS =
            new CombatAnimationSpec("transform_claws", 24, 24, 24, 24, true);
    public static final CombatAnimationSpec CONSUME =
            new CombatAnimationSpec("consume", 46, 18, 30, 46, true);
    public static final CombatAnimationSpec AIR_DASH =
            new CombatAnimationSpec("air_dash", 12, 3, 7, 9, false);

    public static final List<CombatAnimationSpec> FIRST_SLICE = List.of(
            CLAW_AUTO_1,
            CLAW_AUTO_2,
            CLAW_AUTO_3,
            CLAW_GROUNDSPIKE,
            CLAW_DASHING_SLICE,
            CLAW_SPRINT_ATTACK,
            TRANSFORM_CLAWS,
            CONSUME,
            AIR_DASH
    );

    public CombatAnimationSpec {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Animation id must not be blank");
        }
        if (duration <= 0 || startupEnd < 0 || startupEnd > activeEnd
                || activeEnd > cancelStart || cancelStart > duration) {
            throw new IllegalArgumentException("Invalid animation timing for " + id);
        }
    }

    public boolean isActive(int elapsedTicks) {
        return elapsedTicks >= startupEnd && elapsedTicks < activeEnd;
    }

    public boolean canCancel(int elapsedTicks) {
        return elapsedTicks >= cancelStart;
    }
}
