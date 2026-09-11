package com.codex.blacklight.client.renderer;

import com.codex.blacklight.power.PowerForm;
import com.codex.blacklight.registry.ModAttachments;
import net.minecraft.client.player.AbstractClientPlayer;

import java.util.Map;
import java.util.WeakHashMap;

/** Client render-time interpolation for the 24-tick biological arm replacement. */
public final class ClawMorphTracker {
    public static final float DURATION_TICKS = 24.0F;
    private static final Map<AbstractClientPlayer, MorphState> STATES = new WeakHashMap<>();

    private ClawMorphTracker() { }

    public static float progress(AbstractClientPlayer player, float partialTick) {
        boolean active = isActive(player);
        MorphState morph = STATES.computeIfAbsent(player,
                ignored -> new MorphState(active, active ? 1.0F : 0.0F,
                        active ? 1.0F : 0.0F, player.tickCount));
        float now = player.tickCount + partialTick;
        float current = interpolate(morph, now);
        if (active != morph.targetActive) {
            morph.startProgress = current;
            morph.targetProgress = active ? 1.0F : 0.0F;
            morph.startTick = now;
            morph.targetActive = active;
            current = morph.startProgress;
        }
        return clamp(current);
    }

    public static boolean isMorphVisible(AbstractClientPlayer player) {
        return progress(player, 0.0F) > 0.001F || isActive(player);
    }

    private static boolean isActive(AbstractClientPlayer player) {
        var state = player.getData(ModAttachments.PLAYER_STATE);
        return state.isInfected() && state.isPrototypeMode()
                && state.getActivePower() == PowerForm.CLAWS;
    }

    private static float interpolate(MorphState state, float now) {
        float elapsed = Math.max(0.0F, now - state.startTick);
        float distance = Math.abs(state.targetProgress - state.startProgress);
        if (distance < 1.0E-4F) return state.targetProgress;
        float t = clamp(elapsed / (DURATION_TICKS * distance));
        // Smoothstep keeps the biomass growth organic instead of moving at a robotic constant rate.
        t = t * t * (3.0F - 2.0F * t);
        return state.startProgress + (state.targetProgress - state.startProgress) * t;
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static final class MorphState {
        private boolean targetActive;
        private float startProgress;
        private float targetProgress;
        private float startTick;

        private MorphState(boolean targetActive, float startProgress,
                           float targetProgress, float startTick) {
            this.targetActive = targetActive;
            this.startProgress = startProgress;
            this.targetProgress = targetProgress;
            this.startTick = startTick;
        }
    }
}
