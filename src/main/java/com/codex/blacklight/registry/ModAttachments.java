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

    /** Server game time of infection onset; -1 denotes a pre-existing infection. */
    public static final Supplier<AttachmentType<Long>> INFECTION_START = ATTACHMENTS.register(
            "infection_start", () -> AttachmentType.builder(() -> -1L)
                    .serialize(com.mojang.serialization.Codec.LONG)
                    .sync(net.minecraft.network.codec.ByteBufCodecs.VAR_LONG).build());

    /** Transient action lock; never carried through logout/death or persisted. */
    public static final Supplier<AttachmentType<Boolean>> CONSUMING = ATTACHMENTS.register(
            "consuming", () -> AttachmentType.builder(() -> false)
                    .sync(net.minecraft.network.codec.ByteBufCodecs.BOOL).build());

    private ModAttachments() {
    }

    public static final Supplier<AttachmentType<com.codex.blacklight.combat.ConsumeVisual>> CONSUME_VISUAL = ATTACHMENTS.register(
            "consume_visual", () -> AttachmentType.builder(() -> com.codex.blacklight.combat.ConsumeVisual.EMPTY)
                    .sync(com.codex.blacklight.combat.ConsumeVisual.CODEC).build());

    public static void register(IEventBus bus) {
        ATTACHMENTS.register(bus);
    }
}
