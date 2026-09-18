package com.codex.blacklight.power;

import com.codex.blacklight.combat.ClawCombatState;
import com.codex.blacklight.registry.ModAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Selection is server-authoritative; unavailable and busy forms cannot be selected by packets. */
public final class PowerFormSystem {
    private static final PowerForm[] FORMS = {PowerForm.NONE, PowerForm.CLAWS, PowerForm.HAMMERFISTS, PowerForm.BLADE, PowerForm.WHIPFIST};
    private PowerFormSystem() {}
    public static boolean supported(PowerForm form) {
        for (var candidate : FORMS) if (form == candidate) return true;
        return false;
    }
    public static boolean select(ServerPlayer player, PowerForm form, boolean purchase) {
        var state = player.getData(ModAttachments.PLAYER_STATE);
        var combat = player.getData(ModAttachments.CLAW_COMBAT);
        if (!state.isInfected() || !state.isPrototypeMode() || player.getData(ModAttachments.CONSUMING)
                || combat.action() != ClawCombatState.Action.IDLE || !supported(form) || !combat.canToggleTransform(player.tickCount)) return false;
        if (form != PowerForm.NONE && !state.hasUnlocked(form) && !(purchase && state.buy(form))) {
            player.displayClientMessage(Component.translatable("message.blacklight.form_locked", Component.translatable("power.blacklight." + form.id()), form.epCost()), true);
            return false;
        }
        if (!state.activate(form)) return false;
        combat.markTransformToggled(player.tickCount);
        player.setData(ModAttachments.PLAYER_STATE, state); player.syncData(ModAttachments.PLAYER_STATE);
        player.displayClientMessage(Component.translatable("message.blacklight.form_selected", Component.translatable("power.blacklight." + form.id())), true);
        return true;
    }
    public static void cycle(ServerPlayer player) {
        var state = player.getData(ModAttachments.PLAYER_STATE); int index = 0;
        for (int i = 0; i < FORMS.length; i++) if (FORMS[i] == state.getActivePower()) index = i;
        for (int i = 1; i <= FORMS.length; i++) {
            var next = FORMS[(index + i) % FORMS.length];
            if (next == PowerForm.NONE || state.hasUnlocked(next)) { select(player, next, false); return; }
        }
    }
}
