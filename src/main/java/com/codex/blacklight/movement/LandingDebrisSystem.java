package com.codex.blacklight.movement;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Real 3D ground blocks blown out of the impact ring by heavy landings. The chunks
 * are block-display entities on hand-driven ballistic arcs, discarded a few ticks
 * after the toss: display entities can never place a block, drop an item, or touch
 * protected terrain, yet render exactly like the block that was shaken loose.
 */
public final class LandingDebrisSystem {
    private static final double GRAVITY = 0.04D;
    private static final int MAX_TRACKED = 64;

    private static final List<Entry> TRACKED = new ArrayList<>();

    private LandingDebrisSystem() {
    }

    /** Tosses one surface block chunk on a short outward arc. */
    public static void toss(ServerLevel level, double x, double y, double z,
                            BlockState state, double vx, double vy, double vz,
                            int lifetimeTicks) {
        if (TRACKED.size() >= MAX_TRACKED) return;
        Display.BlockDisplay chunk = new Display.BlockDisplay(EntityType.BLOCK_DISPLAY, level);
        // BlockDisplay's state setter and data accessor are private in 1.21.1, so the
        // chunk receives its block look through the public NBT load path instead.
        CompoundTag tag = new CompoundTag();
        tag.put("block_state", NbtUtils.writeBlockState(state));
        chunk.load(tag);
        chunk.moveTo(x, y, z, 0.0F, 0.0F);
        if (!level.addFreshEntity(chunk)) return;
        TRACKED.add(new Entry(chunk, state, vx, vy, vz, lifetimeTicks));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (TRACKED.isEmpty()) return;
        Iterator<Entry> iterator = TRACKED.iterator();
        while (iterator.hasNext()) {
            Entry entry = iterator.next();
            Display.BlockDisplay chunk = entry.chunk;
            if (!chunk.isAlive()) {
                iterator.remove();
                continue;
            }
            entry.ticksLeft--;
            if (entry.ticksLeft <= 0) {
                iterator.remove();
                shatter(chunk, entry.state);
                continue;
            }
            entry.vy -= GRAVITY;
            Vec3 position = chunk.position();
            chunk.setPos(position.x + entry.vx, position.y + entry.vy, position.z + entry.vz);
            chunk.hasImpulse = true;
        }
    }

    /** A small puff of the chunk's own material masks the mid-air despawn. */
    private static void shatter(Display.BlockDisplay chunk, BlockState state) {
        if (chunk.level() instanceof ServerLevel level) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                    chunk.getX(), chunk.getY(), chunk.getZ(),
                    6, 0.18D, 0.18D, 0.18D, 0.06D);
            level.sendParticles(ParticleTypes.CLOUD, chunk.getX(), chunk.getY(),
                    chunk.getZ(), 2, 0.12D, 0.10D, 0.12D, 0.015D);
        }
        chunk.discard();
    }

    private static final class Entry {
        private final Display.BlockDisplay chunk;
        private final BlockState state;
        private double vx;
        private double vy;
        private double vz;
        private int ticksLeft;

        private Entry(Display.BlockDisplay chunk, BlockState state,
                      double vx, double vy, double vz, int ticksLeft) {
            this.chunk = chunk;
            this.state = state;
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
            this.ticksLeft = ticksLeft;
        }
    }
}
