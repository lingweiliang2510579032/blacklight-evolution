package com.codex.blacklight.combat;

/** Pure combat constants used by the server-authoritative claw hit resolver. */
public final class ClawDamageRules {
    private ClawDamageRules() {
    }

    public static float comboBaseDamage(ClawCombatState.Action action) {
        return switch (action) {
            case AUTO_1 -> 7.0F;
            case AUTO_2 -> 8.0F;
            case AUTO_3 -> 11.0F;
            default -> throw new IllegalArgumentException("Not a claw combo action: " + action);
        };
    }

    public static double comboReach(ClawCombatState.Action action) {
        return switch (action) {
            case AUTO_1 -> 2.75D;
            case AUTO_2 -> 2.9D;
            case AUTO_3 -> 3.25D;
            default -> throw new IllegalArgumentException("Not a claw combo action: " + action);
        };
    }

    public static int comboMaxTargets(ClawCombatState.Action action) {
        return action == ClawCombatState.Action.AUTO_3 ? 4 : 3;
    }

    public static float applyDamageUpgrade(float baseDamage, boolean upgraded) {
        if (baseDamage < 0.0F) throw new IllegalArgumentException("Damage must not be negative");
        return upgraded ? baseDamage * 1.25F : baseDamage;
    }
}
