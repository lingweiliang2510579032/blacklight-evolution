package com.codex.blacklight.power;

/** Persistent Prototype-style claw upgrades bought with evolution points. */
public enum ClawUpgrade {
    RANGE("range", 1 << 0, 150),
    DAMAGE("damage", 1 << 1, 200),
    DASHING_SLICE("dashing_slice", 1 << 2, 250),
    POUNCE("pounce", 1 << 3, 200);

    private final String id;
    private final int bit;
    private final int epCost;

    ClawUpgrade(String id, int bit, int epCost) {
        this.id = id;
        this.bit = bit;
        this.epCost = epCost;
    }

    public String id() { return id; }
    public int bit() { return bit; }
    public int epCost() { return epCost; }

    public static ClawUpgrade byId(String id) {
        for (ClawUpgrade upgrade : values()) {
            if (upgrade.id.equals(id)) return upgrade;
        }
        return null;
    }
}
