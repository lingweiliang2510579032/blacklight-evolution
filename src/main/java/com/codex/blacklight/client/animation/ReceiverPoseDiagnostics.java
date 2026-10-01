package com.codex.blacklight.client.animation;

import com.codex.blacklight.BlacklightNeoMod;
import com.codex.blacklight.registry.ModAttachments;
import net.minecraft.world.entity.LivingEntity;
import java.util.Map;
import java.util.WeakHashMap;

/** Report the rendered receiver, not merely the server's selected actor clip. */
public final class ReceiverPoseDiagnostics {
    private static final Map<LivingEntity, Map<Class<?>, String>> LAST = new WeakHashMap<>();
    private ReceiverPoseDiagnostics() {}

    public static void record(LivingEntity entity, Object model, String clip, int bound) {
        var visual = entity.getData(ModAttachments.CONSUME_VISUAL);
        String key = visual.started()+":"+visual.stage()+":"+clip+":"+bound;
        var models = LAST.computeIfAbsent(entity, e -> new java.util.HashMap<>());
        if (key.equals(models.put(model.getClass(), key))) return;
        if (bound == 0) BlacklightNeoMod.LOGGER.warn("Receiver pose unbound: target={} model={} clip={}",
                entity.getType().getDescriptionId(), model.getClass().getSimpleName(), clip);
        else BlacklightNeoMod.LOGGER.info("Receiver pose applied: target={} model={} clip={} joints={}",
                entity.getType().getDescriptionId(), model.getClass().getSimpleName(), clip, bound);
    }
}
