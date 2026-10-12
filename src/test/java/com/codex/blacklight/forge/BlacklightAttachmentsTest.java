package com.codex.blacklight.forge;

import com.codex.blacklight.disguise.DisguiseIdentity;
import com.codex.blacklight.power.UtilityState;
import com.codex.blacklight.registry.ModAttachments;
import com.codex.blacklight.state.GrowthProgress;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BlacklightAttachmentsTest {
    @Test
    void diskNbtRoundTripPreservesProgressionAndExcludesTransientActions() {
        BlacklightAttachments source = progressedPlayer();
        source.set(ModAttachments.CONSUMING.get(), true);
        source.set(ModAttachments.GRAB_STAGE.get(), 3);
        source.set(ModAttachments.GLIDE_UNTIL.get(), 900L);
        CompoundTag saved = source.serializeNBT();
        assertEquals(Set.of("player_state", "growth", "utility_state", "infection_start", "disguise_identity"), saved.getAllKeys());

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        BlacklightAttachments loaded = new BlacklightAttachments();
        loaded.set(ModAttachments.CONSUMING.get(), true);
        try {
            buffer.writeNbt(saved);
            loaded.deserializeNBT(buffer.readNbt());
            assertFalse(buffer.isReadable());
        } finally {
            buffer.release();
        }
        assertEquals(saved, loaded.serializeNBT());
        assertTrue(loaded.get(ModAttachments.PLAYER_STATE.get()).isInfected());
        assertEquals(350, loaded.get(ModAttachments.PLAYER_STATE.get()).getBiomass());
        assertEquals(120, loaded.get(ModAttachments.PLAYER_STATE.get()).getEvolutionPoints());
        assertEquals(new GrowthProgress(7, 42), loaded.get(ModAttachments.GROWTH.get()));
        assertEquals(33F, loaded.get(ModAttachments.UTILITY_STATE.get()).shield());
        assertEquals(111L, loaded.get(ModAttachments.INFECTION_START.get()));
        assertFalse(loaded.has(ModAttachments.CONSUMING.get()));
        assertFalse(loaded.has(ModAttachments.GRAB_STAGE.get()));
        assertFalse(loaded.has(ModAttachments.GLIDE_UNTIL.get()));
    }

    @Test
    void deathCopiesOnlyDeclaredFieldsAndDoesNotShareMutableStateOrAppearance() {
        BlacklightAttachments source = progressedPlayer();
        source.set(ModAttachments.CONSUMING.get(), true);
        BlacklightAttachments respawned = new BlacklightAttachments();
        respawned.copyPersistentFrom(source, true);

        assertEquals(Set.of("player_state", "growth", "disguise_identity"), respawned.serializeNBT().getAllKeys());
        assertFalse(respawned.has(ModAttachments.INFECTION_START.get()));
        assertFalse(respawned.has(ModAttachments.UTILITY_STATE.get()));
        assertFalse(respawned.has(ModAttachments.CONSUMING.get()));
        assertEquals(UtilityState.EMPTY, respawned.get(ModAttachments.UTILITY_STATE.get()));
        assertNotSame(source.get(ModAttachments.PLAYER_STATE.get()), respawned.get(ModAttachments.PLAYER_STATE.get()));
        respawned.get(ModAttachments.PLAYER_STATE.get()).addBiomass(100);
        assertEquals(350, source.get(ModAttachments.PLAYER_STATE.get()).getBiomass());
        assertEquals(450, respawned.get(ModAttachments.PLAYER_STATE.get()).getBiomass());

        CompoundTag originalAppearance = source.get(ModAttachments.DISGUISE_IDENTITY.get()).appearance();
        CompoundTag newAppearance = respawned.get(ModAttachments.DISGUISE_IDENTITY.get()).appearance();
        assertNotSame(originalAppearance, newAppearance);
        newAppearance.putInt("Color", 1);
        assertEquals(12, originalAppearance.getInt("Color"));
    }

    @Test
    void nonDeathCloneRetainsAllPersistentFieldsWhileDimensionResetClearsTransientState() {
        BlacklightAttachments source = progressedPlayer();
        source.set(ModAttachments.TARGET_LOCK_ID.get(), 18);
        BlacklightAttachments cloned = new BlacklightAttachments();
        cloned.copyPersistentFrom(source, false);
        assertEquals(source.serializeNBT(), cloned.serializeNBT());
        assertFalse(cloned.has(ModAttachments.TARGET_LOCK_ID.get()));
        assertNotSame(source.get(ModAttachments.PLAYER_STATE.get()), cloned.get(ModAttachments.PLAYER_STATE.get()));

        CompoundTag beforeReset = source.serializeNBT();
        source.clearTransient();
        assertFalse(source.has(ModAttachments.TARGET_LOCK_ID.get()));
        assertEquals(beforeReset, source.serializeNBT());
    }

    @Test
    void providerInvalidationNotifiesObserversAndRevivedLookupReturnsSameStorage() {
        BlacklightAttachments storage = progressedPlayer();
        var original = storage.getCapability(BlacklightAttachments.CAPABILITY, null);
        boolean[] notified = {false};
        original.addListener(ignored -> notified[0] = true);
        storage.invalidate();
        assertTrue(notified[0]);
        assertFalse(original.isPresent());

        var revived = storage.getCapability(BlacklightAttachments.CAPABILITY, null);
        assertNotSame(original, revived);
        assertSame(storage, revived.orElseThrow(AssertionError::new));
        assertEquals(350, revived.orElseThrow(AssertionError::new).get(ModAttachments.PLAYER_STATE.get()).getBiomass());
        storage.remove(ModAttachments.PLAYER_STATE.get());
        assertFalse(storage.has(ModAttachments.PLAYER_STATE.get()));
    }

    private static BlacklightAttachments progressedPlayer() {
        BlacklightAttachments storage = new BlacklightAttachments();
        var state = storage.get(ModAttachments.PLAYER_STATE.get());
        state.infect(); state.addBiomass(100); state.addEvolutionPoints(120);
        state.setPrototypeMode(true);
        storage.set(ModAttachments.GROWTH.get(), new GrowthProgress(7, 42));
        storage.set(ModAttachments.UTILITY_STATE.get(), new UtilityState(33, 555, 999, 1, 77));
        storage.set(ModAttachments.INFECTION_START.get(), 111L);
        CompoundTag appearance = new CompoundTag();
        appearance.putInt("Color", 12);
        storage.set(ModAttachments.DISGUISE_IDENTITY.get(), new DisguiseIdentity("minecraft:sheep", "animal", appearance));
        return storage;
    }
}
