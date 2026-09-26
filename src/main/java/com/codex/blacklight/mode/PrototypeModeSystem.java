package com.codex.blacklight.mode;

import com.codex.blacklight.combat.ClawCombatSystem;
import com.codex.blacklight.registry.ModAttachments;
import com.codex.blacklight.state.BlacklightState;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

/** Server authority for switching the player between vanilla and Prototype rules. */
public final class PrototypeModeSystem {
    private PrototypeModeSystem() {
    }

    public static void handle(ServerPlayer player, boolean enabled) {
        BlacklightState state = player.getData(ModAttachments.PLAYER_STATE);
        if (!state.isInfected()) {
            player.displayClientMessage(Component.translatable(
                    "message.blacklight.prototype_mode_requires_infection"), true);
            return;
        }
        if (state.isPrototypeMode() == enabled) return;

        com.codex.blacklight.combat.WeaponCombatSystem.cancel(player);
        state.setPrototypeMode(enabled);
        player.setData(ModAttachments.PLAYER_STATE, state);
        player.syncData(ModAttachments.PLAYER_STATE);
        player.getData(ModAttachments.MOVEMENT_STATE).reset();
        player.stopUsingItem();
        if (enabled) {
            player.closeContainer();
        } else {
            var velocity = player.getDeltaMovement();
            var horizontal = velocity.multiply(1.0D, 0.0D, 1.0D);
            if (horizontal.length() > 0.35D) horizontal = horizontal.normalize().scale(0.35D);
            player.setDeltaMovement(horizontal.x, velocity.y, horizontal.z);
            player.hurtMarked = true;
        }
        synchronizeEpicFightMode(player, enabled);
        ClawCombatSystem.playPrototypeTransition(player, enabled);
        player.displayClientMessage(Component.translatable(enabled
                ? "message.blacklight.prototype_mode_enabled"
                : "message.blacklight.prototype_mode_disabled"), true);
    }

    public static boolean isActive(net.minecraft.world.entity.player.Player player) {
        BlacklightState state = player.getData(ModAttachments.PLAYER_STATE);
        return state.isInfected() && state.isPrototypeMode();
    }

    /** Keeps Epic Fight's own mode aligned so B is the only authoritative mode switch. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        BlacklightState state = player.getData(ModAttachments.PLAYER_STATE);
        if (!state.isInfected()) return;
        synchronizeEpicFightMode(player, state.isPrototypeMode());
    }

    private static void synchronizeEpicFightMode(ServerPlayer player, boolean prototypeMode) {
        ServerPlayerPatch patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (patch == null) return;
        if (prototypeMode && patch.isVanillaMode()) {
            patch.toEpicFightMode(true);
        } else if (!prototypeMode && patch.isEpicFightMode()) {
            patch.toVanillaMode(true);
        }
    }
}
