package com.codex.blacklight.registry;

import com.codex.blacklight.BlacklightNeoMod;
import com.codex.blacklight.combat.ClawCombatState;
import com.codex.blacklight.movement.MovementState;
import com.codex.blacklight.state.BlacklightState;
import com.codex.blacklight.state.BlacklightStateStreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ModAttachments {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, BlacklightNeoMod.MOD_ID);

    public static final Supplier<AttachmentType<BlacklightState>> PLAYER_STATE = ATTACHMENTS.register(
            "player_state",
            () -> AttachmentType.builder(BlacklightState::new)
                    .serialize(BlacklightState.CODEC)
                    .copyOnDeath()
                    .sync(BlacklightStateStreamCodec.CODEC)
                    .build());

    public static final Supplier<AttachmentType<ClawCombatState>> CLAW_COMBAT = ATTACHMENTS.register(
            "claw_combat",
            () -> AttachmentType.builder(ClawCombatState::new).build());

    public static final Supplier<AttachmentType<MovementState>> MOVEMENT_STATE = ATTACHMENTS.register(
            "movement_state",
            () -> AttachmentType.builder(MovementState::new).build());

    private ModAttachments() {
    }

    public static void register(IEventBus bus) {
        ATTACHMENTS.register(bus);
    }
}
