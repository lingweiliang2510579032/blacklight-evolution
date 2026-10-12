package com.codex.blacklight.client.renderer;

import com.codex.blacklight.power.PowerForm;
import net.minecraft.client.player.AbstractClientPlayer;

/** Compatibility view of the shared all-weapon material clock. */
public final class ClawMorphTracker {
    public static final float DURATION_TICKS=com.codex.blacklight.client.animation.WeaponMorphTiming.QUICK_TICKS;
    private ClawMorphTracker() { }
    public static float progress(AbstractClientPlayer player,float partial) {
        return WeaponMorphTracker.sample(player,partial).frame(PowerForm.CLAWS).growth();
    }
    public static boolean isMorphVisible(AbstractClientPlayer player){return progress(player,0)>.001F;}
}
