package com.codex.blacklight.infection;

import com.codex.blacklight.entity.BlacklightHive;
import com.codex.blacklight.entity.BlacklightInfectedHost;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;

/** Administrator opt-in for one nursery. UUID lookups never load chunks or scan entities. */
public final class HiveAcceleration {
    private static final String RATE = "BlacklightHiveAcceleration";
    private static final String HOST = "BlacklightHiveAccelerationHost";
    private static final String CORE = "BlacklightDevourAccelerationCore";
    private HiveAcceleration() {}

    public static boolean belongsTo(BlacklightInfectedHost host, BlacklightHive hive) {
        return DevourerFeedingSystem.isDevourer(host) && host.level() == hive.level()
                && hive.getPersistentData().getBoolean("BlacklightColossalHive")
                && hive.getPersistentData().contains("BlacklightHiveHome", Tag.TAG_LONG)
                && host.getPersistentData().contains("BlacklightDevourHome", Tag.TAG_LONG)
                && hive.getPersistentData().getLong("BlacklightHiveHome") == DevourerFeedingSystem.home(host).asLong();
    }

    public static int multiplier(BlacklightHive hive) {
        if (!hive.isAlive() || !hive.getPersistentData().getBoolean("BlacklightColossalHive")
                || hive.getPersistentData().getCompound("BlacklightColossusEvolution").getBoolean("Spent")) return 1;
        return Mth.clamp(hive.getPersistentData().getInt(RATE), 1, 20);
    }

    public static int multiplier(BlacklightInfectedHost host) {
        if (!host.isAlive() || host.giantBoss() || !(host.level() instanceof ServerLevel level)) return 1;
        CompoundTag data = host.getPersistentData();
        if (!data.hasUUID(CORE) || !(level.getEntity(data.getUUID(CORE)) instanceof BlacklightHive hive)
                || !belongsTo(host, hive)) return 1;
        CompoundTag core = hive.getPersistentData();
        return core.hasUUID(HOST) && core.getUUID(HOST).equals(host.getUUID()) ? multiplier(hive) : 1;
    }

    public static void configure(BlacklightHive hive, BlacklightInfectedHost host, int multiplier) {
        clear(hive);
        if (multiplier <= 1) return;
        hive.getPersistentData().putInt(RATE, Mth.clamp(multiplier, 1, 20));
        hive.getPersistentData().putUUID(HOST, host.getUUID());
        host.getPersistentData().putUUID(CORE, hive.getUUID());
        DevourerFeedingSystem.adjustMealWait(host, 1, multiplier(host));
    }

    public static void clear(BlacklightHive hive) {
        CompoundTag data = hive.getPersistentData();
        int before = Mth.clamp(data.getInt(RATE), 1, 20);
        if (hive.level() instanceof ServerLevel level && data.hasUUID(HOST)
                && level.getEntity(data.getUUID(HOST)) instanceof BlacklightInfectedHost host) {
            CompoundTag linked = host.getPersistentData();
            if (linked.hasUUID(CORE) && linked.getUUID(CORE).equals(hive.getUUID())) {
                linked.remove(CORE);
                DevourerFeedingSystem.adjustMealWait(host, before, 1);
            }
        }
        data.remove(RATE);
        data.remove(HOST);
    }
}
