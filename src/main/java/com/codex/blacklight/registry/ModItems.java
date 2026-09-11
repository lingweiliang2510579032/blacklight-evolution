package com.codex.blacklight.registry;

import com.codex.blacklight.BlacklightNeoMod;
import com.codex.blacklight.item.BlacklightSampleItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
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

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register(
            "blacklight", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.blacklight"))
                    .icon(() -> BLACKLIGHT_SAMPLE.get().getDefaultInstance())
                    .displayItems((parameters, output) -> output.accept(BLACKLIGHT_SAMPLE.get()))
                    .build());

    private ModItems() {
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        TABS.register(bus);
    }
}
