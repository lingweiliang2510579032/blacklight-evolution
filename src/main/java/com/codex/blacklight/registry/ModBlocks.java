package com.codex.blacklight.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.bus.api.IEventBus;

/** Building-scale infection, kept distinct from ordinary terrain blocks. */
public final class ModBlocks {
    private static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks("blacklight");
    public static final DeferredBlock<Block> HIVE_FLESH=BLOCKS.registerSimpleBlock("hive_flesh",BlockBehaviour.Properties.of().strength(3,6).sound(SoundType.SLIME_BLOCK));
    public static final DeferredBlock<Block> HIVE_CARAPACE=BLOCKS.registerSimpleBlock("hive_carapace",BlockBehaviour.Properties.of().strength(5,12).sound(SoundType.NETHER_WOOD));
    public static final DeferredBlock<Block> HIVE_SAC=BLOCKS.registerSimpleBlock("hive_sac",BlockBehaviour.Properties.of().strength(2,4).sound(SoundType.SLIME_BLOCK).lightLevel(s->5));
    public static void register(IEventBus bus){BLOCKS.register(bus);}
}
