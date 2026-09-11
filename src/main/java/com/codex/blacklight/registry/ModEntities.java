package com.codex.blacklight.registry;

import com.codex.blacklight.BlacklightNeoMod;
import com.codex.blacklight.entity.BlacklightVirusVialEntity;
import com.codex.blacklight.entity.GroundSpikeEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    private static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, BlacklightNeoMod.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<GroundSpikeEntity>> GROUND_SPIKE =
            ENTITIES.register("ground_spike", () -> EntityType.Builder
                    .<GroundSpikeEntity>of(GroundSpikeEntity::new, MobCategory.MISC)
                    .sized(3.6F, 5.4F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .noSave()
                    .noSummon()
                    .build("ground_spike"));

    public static final DeferredHolder<EntityType<?>, EntityType<BlacklightVirusVialEntity>> BLACKLIGHT_VIRUS_VIAL =
            ENTITIES.register("blacklight_virus_vial", () -> EntityType.Builder
                    .<BlacklightVirusVialEntity>of(BlacklightVirusVialEntity::new, MobCategory.MISC)
                    .sized(0.28F, 0.42F)
                    .clientTrackingRange(8)
                    .updateInterval(10)
                    .build("blacklight_virus_vial"));

    private ModEntities() {
    }

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }
}
