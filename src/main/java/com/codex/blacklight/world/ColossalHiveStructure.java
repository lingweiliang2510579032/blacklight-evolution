package com.codex.blacklight.world;

import com.codex.blacklight.registry.ModWorldgen;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import java.util.Arrays;
import java.util.Optional;

/** A surface structure split by Minecraft's normal per-chunk structure placement. */
public final class ColossalHiveStructure extends Structure {
    public static final MapCodec<ColossalHiveStructure> CODEC=simpleCodec(ColossalHiveStructure::new);
    public ColossalHiveStructure(StructureSettings settings) { super(settings); }
    @Override public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int x=context.chunkPos().getMiddleBlockX(),z=context.chunkPos().getMiddleBlockZ();
        int[] heights=new int[49];int count=0;
        for(int dx=-48;dx<=48;dx+=16)for(int dz=-48;dz<=48;dz+=16) {
            int ground=context.chunkGenerator().getBaseHeight(x+dx,z+dz,Heightmap.Types.OCEAN_FLOOR_WG,
                    context.heightAccessor(),context.randomState());
            int surface=context.chunkGenerator().getBaseHeight(x+dx,z+dz,Heightmap.Types.WORLD_SURFACE_WG,
                    context.heightAccessor(),context.randomState());
            // Reject coastlines, rivers, submerged terrain and steep mountain faces.
            if(ground<surface||ground<context.chunkGenerator().getSeaLevel()+1)return Optional.empty();
            heights[count++]=ground;
        }
        Arrays.sort(heights);
        if(heights[48]-heights[0]>12)return Optional.empty();
        int floor=heights[24]-1;
        if(floor+ColossalHiveLayout.MIN_Y<=context.heightAccessor().getMinBuildHeight()
                ||floor+ColossalHiveLayout.MAX_Y>=context.heightAccessor().getMaxBuildHeight())return Optional.empty();
        BlockPos centre=new BlockPos(x,floor,z);
        long shapeSeed=context.random().nextLong();
        return Optional.of(new GenerationStub(centre,builder->builder.addPiece(new ColossalHivePiece(centre,shapeSeed))));
    }
    @Override public StructureType<?> type() { return ModWorldgen.COLOSSAL_HIVE.get(); }
}
