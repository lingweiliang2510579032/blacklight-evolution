package com.codex.blacklight.animation;

import java.util.Objects;

/** Installed once at bootstrap. A missing engine is an error, never silent missing animation. */
public final class PlayerActions {
    private static volatile PlayerActionBackend backend;

    private PlayerActions() {}

    public static synchronized void install(PlayerActionBackend implementation) {
        Objects.requireNonNull(implementation, "implementation");
        if (backend != null) throw new IllegalStateException("Player action engine already installed");
        backend = implementation;
    }

    public static PlayerActionBackend get() {
        var installed = backend;
        if (installed == null) throw new IllegalStateException("Player action engine not installed");
        return installed;
    }
}
