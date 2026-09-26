package com.codex.blacklight.power;

import com.codex.blacklight.registry.ModAttachments;
import net.minecraft.world.entity.player.Player;

/** Hunger is suspended only while an infected player uses Prototype mode. */
public final class PrototypeMetabolism {
    private PrototypeMetabolism() { }

    public static boolean active(Player player) {
        if (player == null || !player.isAlive() || player.isSpectator()) return false;
        var state = player.getData(ModAttachments.PLAYER_STATE);
        return state.isInfected() && state.isPrototypeMode();
    }
}
