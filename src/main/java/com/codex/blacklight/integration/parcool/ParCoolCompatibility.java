package com.codex.blacklight.integration.parcool;
import com.alrex.parcool.api.unstable.action.ParCoolActionEvent;
import com.codex.blacklight.mode.PrototypeModeSystem;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;

/** Let regular humans parkour; Prototype's server/client controller owns enhanced movement. */
public final class ParCoolCompatibility {
    private ParCoolCompatibility() {}
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void start(ParCoolActionEvent.TryToStartEvent event) {
        if(PrototypeModeSystem.isActive(event.getPlayer())) event.setCanceled(true);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void keep(ParCoolActionEvent.TryToContinueEvent event) {
        if(PrototypeModeSystem.isActive(event.getPlayer())) event.setCanceled(true);
    }
}
