package com.codex.blacklight.registry;

import com.codex.blacklight.BlacklightNeoMod;
import com.codex.blacklight.item.BlacklightSampleItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BlacklightNeoMod.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BlacklightNeoMod.MOD_ID);

    public static final DeferredItem<BlacklightSampleItem> BLACKLIGHT_SAMPLE = ITEMS.registerItem(
            "blacklight_sample", BlacklightSampleItem::new,
            new Item.Properties().stacksTo(4).rarity(Rarity.EPIC).fireResistant());

    public static final DeferredItem<net.neoforged.neoforge.common.DeferredSpawnEggItem> HUNTER_SPAWN_EGG = ITEMS.registerItem(
            "blacklight_hunter_spawn_egg", props -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(
                    ModEntities.BLACKLIGHT_HUNTER, 0x242626, 0x9d302b, props), new Item.Properties());

    public static final DeferredItem<BlockItem> HIVE_FLESH = ITEMS.registerSimpleBlockItem(ModBlocks.HIVE_FLESH);
    public static final DeferredItem<BlockItem> HIVE_CARAPACE = ITEMS.registerSimpleBlockItem(ModBlocks.HIVE_CARAPACE);
    public static final DeferredItem<BlockItem> HIVE_SAC = ITEMS.registerSimpleBlockItem(ModBlocks.HIVE_SAC);
    public static final DeferredItem<BlockItem> INFECTED_SOIL = ITEMS.registerSimpleBlockItem(ModBlocks.INFECTED_SOIL);
    public static final DeferredItem<BlockItem> INFECTED_STONE = ITEMS.registerSimpleBlockItem(ModBlocks.INFECTED_STONE);
    public static final DeferredItem<BlockItem> TISSUE_FIBER = ITEMS.registerSimpleBlockItem(ModBlocks.TISSUE_FIBER);
    public static final DeferredItem<BlockItem> HIVE_VEIN = ITEMS.registerSimpleBlockItem(ModBlocks.HIVE_VEIN);
    public static final DeferredItem<BlockItem> HIVE_MEMBRANE = ITEMS.registerSimpleBlockItem(ModBlocks.HIVE_MEMBRANE);
    public static final DeferredItem<BlockItem> HIVE_BONE = ITEMS.registerSimpleBlockItem(ModBlocks.HIVE_BONE);
    public static final DeferredItem<BlockItem> NUTRIENT_NODE = ITEMS.registerSimpleBlockItem(ModBlocks.NUTRIENT_NODE);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register(
            "blacklight", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.blacklight"))
                    .icon(() -> BLACKLIGHT_SAMPLE.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(BLACKLIGHT_SAMPLE.get()); output.accept(HUNTER_SPAWN_EGG.get());
                        output.accept(INFECTED_SOIL.get()); output.accept(INFECTED_STONE.get());
                        output.accept(TISSUE_FIBER.get()); output.accept(HIVE_FLESH.get());
                        output.accept(HIVE_CARAPACE.get()); output.accept(HIVE_VEIN.get());
                        output.accept(HIVE_MEMBRANE.get()); output.accept(HIVE_BONE.get());
                        output.accept(HIVE_SAC.get()); output.accept(NUTRIENT_NODE.get());
                    })
                    .build());

    private ModItems() {
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        TABS.register(bus);
    }
}
