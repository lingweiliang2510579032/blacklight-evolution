package com.codex.blacklight.world;

import com.codex.blacklight.registry.ModEntities;
import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** New-terrain encounters only; never replace player blocks or force-load chunks. */
public final class HiveFeature extends Feature<NoneFeatureConfiguration> {
    public HiveFeature(Codec<NoneFeatureConfiguration> codec){super(codec);}
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context){
        var level=context.level();var origin=context.origin();
        int highest=origin.getY(),lowest=origin.getY();
        for(int x=-7;x<=7;x++)for(int z=-7;z<=7;z++){
            int height=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG,origin.getX()+x,origin.getZ()+z);
            highest=Math.max(highest,height);lowest=Math.min(lowest,height);
        }
        if(highest-lowest>3)return false;
        origin=new net.minecraft.core.BlockPos(origin.getX(),highest,origin.getZ());
        if(!HiveBuilding.clearSite(level,origin))return false;
        var hive=ModEntities.BLACKLIGHT_HIVE.get().create(level.getLevel());
        if(hive==null)return false;
        HiveBuilding.build(level,origin);
        hive.moveTo(origin.getX()+.5,origin.getY()+1,origin.getZ()+.5,0,0);
        return level.addFreshEntity(hive);
    }
}
