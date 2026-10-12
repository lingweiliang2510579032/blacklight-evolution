package com.codex.blacklight.forge;

import com.codex.blacklight.network.AttachmentSyncPayload;
import com.codex.blacklight.registry.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Login and tracking updates may precede vanilla entity creation by a few client ticks. */
@Mod.EventBusSubscriber(modid = "blacklight", value = Dist.CLIENT)
public final class ClientAttachmentSync {
    private static final int MAX_PENDING_ENTITIES = 256;
    private static final int MAX_PENDING_TICKS = 200;
    private static final Map<Key, Pending> PENDING = new LinkedHashMap<>();
    private static long clock;

    private ClientAttachmentSync() {}

    public static void accept(AttachmentSyncPayload packet) {
        Key key = new Key(packet.entityId(), packet.uuid(), packet.dimension());
        Pending existing = PENDING.get(key);
        Map<String, byte[]> fields = new LinkedHashMap<>();
        if (existing != null) fields.putAll(existing.packet.fields());
        fields.putAll(packet.fields());
        AttachmentSyncPayload merged = new AttachmentSyncPayload(packet.entityId(), packet.uuid(), packet.dimension(), Map.copyOf(fields));
        if (apply(merged)) {
            PENDING.remove(key);
            return;
        }
        PENDING.put(key, new Pending(merged, clock));
        while (PENDING.size() > MAX_PENDING_ENTITIES) PENDING.remove(PENDING.keySet().iterator().next());
    }

    private static boolean apply(AttachmentSyncPayload packet) {
        var level = Minecraft.getInstance().level;
        if (level == null || !level.dimension().location().equals(packet.dimension())) return false;
        Entity entity = level.getEntity(packet.entityId());
        if (entity == null) return false;
        // A different UUID means vanilla has already reused the id; discard the outdated packet.
        if (entity.getUUID().equals(packet.uuid())) ModAttachments.applyClientUpdate(entity, packet.fields());
        return true;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        clock++;
        Iterator<Pending> iterator = PENDING.values().iterator();
        while (iterator.hasNext()) {
            Pending pending = iterator.next();
            if (clock - pending.created > MAX_PENDING_TICKS || apply(pending.packet)) iterator.remove();
        }
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { PENDING.clear(); clock = 0; }

    private record Key(int entityId, UUID uuid, ResourceLocation dimension) {}
    private record Pending(AttachmentSyncPayload packet, long created) {}
}
