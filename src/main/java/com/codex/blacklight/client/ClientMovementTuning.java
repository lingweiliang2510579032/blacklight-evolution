package com.codex.blacklight.client;

import com.codex.blacklight.movement.MovementTuningSettings;
import com.codex.blacklight.movement.MovementTuningSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;

/** Multiplayer tuning is a session override, never the next singleplayer preset. */
@EventBusSubscriber(modid="blacklight",value=Dist.CLIENT)
public final class ClientMovementTuning {
    private static MovementTuningSnapshot local;
    public static void receive(MovementTuningSnapshot snapshot) {
        // Both logical sides of a local world already share the synchronized settings.
        if(Minecraft.getInstance().hasSingleplayerServer())return;
        if(local==null)local=MovementTuningSettings.snapshot();
        MovementTuningSettings.applyServerSnapshot(snapshot);
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        if(local!=null){MovementTuningSettings.applyServerSnapshot(local);local=null;}
    }
}
