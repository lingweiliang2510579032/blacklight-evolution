package com.codex.blacklight.infection;

import com.codex.blacklight.world.ColossalHiveLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/** Shared server/client choreography. All anchors belong to terrain, never a moving body. */
public final class HiveFeedingMotion {
    public static final int CONTACT = 20, LIFT_END = 38, PULL_END = 96, END = 120;
    private HiveFeedingMotion() {}
    public static BlockPos node(BlockPos home, int index) {
        int[] n = ColossalHiveLayout.NUTRIENT_NODES[index];
        return home.below().offset(n[0], n[1], n[2]);
    }
    public static Vec3 mouth(BlockPos home, int index) {
        return Vec3.atBottomCenterOf(node(home, index)).add(0, 2.2, 0);
    }
    public static double smooth(double t) {
        t = com.codex.blacklight.util.MathCompat.clamp(t, 0, 1); return t * t * (3 - 2 * t);
    }
    public static double contraction(double age) { return 1 - .985 * smooth((age - PULL_END) / (END - PULL_END - 1)); }
    public static Vec3 destination(Vec3 origin, Vec3 mouth, double height, double age) {
        // Lift clear of the floor before crossing the node's surrounding bone blocks.
        Vec3 raised = origin.add(0, 1.7, 0);
        if (age < LIFT_END) return origin.lerp(raised, smooth((age - CONTACT) / (LIFT_END - CONTACT)));
        Vec3 end = mouth.add(0, Math.max(.5, height * .55), 0);
        Vec3 feet = end.add(0, -height * .55, 0);
        return raised.lerp(feet, smooth((age - LIFT_END) / (PULL_END - LIFT_END)));
    }
}
