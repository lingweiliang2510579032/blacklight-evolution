package com.codex.blacklight.infection;

import com.codex.blacklight.entity.BlacklightHive;
import com.codex.blacklight.entity.BlacklightInfectedHost;
import com.codex.blacklight.registry.ModBlocks;
import com.codex.blacklight.world.ColossalHiveLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.phys.AABB;

/** Physical nursery supply. Breaking nodes stops feeding; no hidden virtual energy source. */
public final class DevourerHabitat {
    public static final int REQUIRED_NODES = 2;
    private DevourerHabitat() {}

    /** Unknown supply must pause progression, never count as destruction. */
    public static boolean suppliesLoaded(BlacklightInfectedHost host) {
        if (!(host.level() instanceof ServerLevel level)) return false;
        BlockPos floor = DevourerFeedingSystem.home(host).below();
        if (!level.hasChunkAt(floor.offset(ColossalHiveLayout.CORE_X, ColossalHiveLayout.CORE_Y,
                ColossalHiveLayout.CORE_Z))) return false;
        for (int[] offset : ColossalHiveLayout.NUTRIENT_NODES)
            if (!level.hasChunkAt(floor.offset(offset[0], offset[1], offset[2]))) return false;
        return true;
    }

    public static int liveNodes(BlacklightInfectedHost host) {
        if (!(host.level() instanceof ServerLevel level) || !DevourerFeedingSystem.isDevourer(host)) return 0;
        BlockPos floor = DevourerFeedingSystem.home(host).below();
        int count = 0;
        for (int[] offset : ColossalHiveLayout.NUTRIENT_NODES) {
            BlockPos pos = floor.offset(offset[0], offset[1], offset[2]);
            if (level.hasChunkAt(pos) && level.getBlockState(pos).is(ModBlocks.NUTRIENT_NODE.get())) count++;
        }
        return count;
    }

    public static boolean canFeed(BlacklightInfectedHost host) {
        if (!(host.level() instanceof ServerLevel level) || level.getDifficulty() == Difficulty.PEACEFUL
                || !host.isAlive() || !suppliesLoaded(host) || liveNodes(host) < REQUIRED_NODES) return false;
        BlockPos home = DevourerFeedingSystem.home(host);
        return !level.getEntitiesOfClass(BlacklightHive.class, new AABB(home).inflate(48),
                hive -> hive.isAlive() && !hive.isNoAi() && !hive.isInvulnerable()
                        && hive.getPersistentData().getBoolean("BlacklightColossalHive")
                        && hive.getPersistentData().getLong("BlacklightHiveHome") == home.asLong()
                        && hive.distanceToSqr(home.getX()+.5, home.getY(), home.getZ()+.5) <= 48*48).isEmpty();
    }
}
