package com.codex.blacklight.world;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.npc.VillagerDataHolder;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

/** Replace the snowy apprentice's ocean map slot, retaining other biomes' trades. */
public final class ResearchStationTrades {
    private static final TagKey<Structure> STATIONS = TagKey.create(Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath("blacklight", "on_research_station_maps"));
    private static final VillagerTrades.ItemListing STATION_MAP = new VillagerTrades.TreasureMapForEmeralds(
            12, STATIONS, "filled_map.blacklight.research_station", MapDecorationTypes.RED_X, 12, 5);

    private ResearchStationTrades() {}

    @SubscribeEvent
    public static void register(VillagerTradesEvent event) {
        if (event.getType() != VillagerProfession.CARTOGRAPHER) return;
        var trades = event.getTrades().get(2);
        for (int i = 0; i < trades.size(); i++) {
            var original = trades.get(i);
            if (!(original instanceof VillagerTrades.TreasureMapForEmeralds)) continue;
            trades.set(i, (trader, random) -> {
                if (trader instanceof VillagerDataHolder villager
                        && villager.getVillagerData().getType() == VillagerType.SNOW) {
                    var offer = STATION_MAP.getOffer(trader, random);
                    if (offer != null) return offer;
                }
                return original.getOffer(trader, random);
            });
        }
    }
}
