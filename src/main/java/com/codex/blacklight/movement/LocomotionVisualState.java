package com.codex.blacklight.movement;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Client-populated pose snapshot kept free of client-only class references. */
public final class LocomotionVisualState {
    private static final Map<Integer, Snapshot> SNAPSHOTS = new ConcurrentHashMap<>();

    private LocomotionVisualState() {
    }

    public static void set(int entityId, LocomotionDirection direction, LocomotionPhase phase) {
        set(entityId,direction,phase,false);
    }

    public static void set(int entityId,LocomotionDirection direction,LocomotionPhase phase,boolean sprint) {
        SNAPSHOTS.put(entityId,new Snapshot(direction,phase,sprint));
    }

    public static Snapshot get(int entityId) {
        // computeIfAbsent inserts lazily instead of allocating a throwaway snapshot
        // on every render-frame miss for unknown entities.
        return SNAPSHOTS.computeIfAbsent(entityId,
                ignored -> new Snapshot(LocomotionDirection.FRONT, LocomotionPhase.IDLE,false));
    }

    public static void remove(int entityId) {
        SNAPSHOTS.remove(entityId);
    }

    public static void clear() {
        SNAPSHOTS.clear();
    }

    public record Snapshot(LocomotionDirection direction, LocomotionPhase phase,boolean sprint) {
    }
}
