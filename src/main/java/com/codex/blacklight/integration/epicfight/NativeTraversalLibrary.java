package com.codex.blacklight.integration.epicfight;

import com.codex.blacklight.combat.LocalVisualMotion;
import com.codex.blacklight.combat.MotionResources;
import java.util.List;

/** Original-derived tracks are privately installed, never substituted with authored poses. */
public final class NativeTraversalLibrary {
    public static final List<String> IDS=java.util.Arrays.stream((
            "idle idle_claw idle_blade idle_hammer idle_whip walk run sprint sprint_fast sprint_fastest "
            +"wall_run_up wall_run_left wall_run_right wall_run_diag_left wall_run_diag_right "
            +"wall_climb_up wall_climb_down wall_climb_left wall_climb_right wall_cling brake sprint_brake turn "
            +"jump_charge jump_rise jump_tactical jump_running fall wall_leap wall_mantle wall_vault wall_top parkour_small "
            +"air_dash air_dash_brake glide_forward glide_backward glide_left glide_right "
            +"land_light land_moving land_heavy land_devastating").split(" ")).map(id->"basic_"+id).toList();
    private NativeTraversalLibrary(){}
    public static void requireInstalled(){
        if (!MotionResources.contains("armor-growth.data")) throw new IllegalStateException("Incomplete motion profile: armor-growth.data");
        var missing=java.util.stream.Stream.concat(IDS.stream(), java.util.stream.Stream.of("utility_shield_deploy", "utility_shield_hit", "utility_shield_run", "utility_shield_sprint", "utility_armor_transform", "utility_armor_idle")).filter(id->!MotionResources.contains(id+".json")).toList();
        if(!missing.isEmpty())throw new IllegalStateException("Incomplete motion profile: "+missing
                +". Install the bundled-motion build or set blacklight.localWeaponMotions to a complete profile.");
    }
    public static LocalVisualMotion.Definition require(String id){
        return LocalVisualMotion.find(id).orElseThrow(()->new IllegalStateException("Missing original traversal motion: "+id));
    }
}
