package com.codex.blacklight.combat;

import com.codex.blacklight.power.GrowthUpgrade;
import com.codex.blacklight.registry.ModAttachments;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** One progression scale shared by server contact, environment probes and visible attack sweeps. */
public final class AttackReach {
    private AttackReach(){}
    public static int tier(Player p){
        var growth=p.getData(ModAttachments.GROWTH);
        return growth.owns(GrowthUpgrade.REACH_III)?3:growth.owns(GrowthUpgrade.REACH_II)?2:growth.owns(GrowthUpgrade.REACH_I)?1:0;
    }
    public static double scale(Entity actor){return actor instanceof Player p?1.25+.10*tier(p):1;}
    public static double reach(Player p,WeaponMove move){return move.reach*scale(p);}
    public static LocalWeaponMotion.ContactBounds bounds(Player p,WeaponMove move,int tick){
        var b=move.contactBounds(tick);if(b==null)return null;double s=scale(p);
        return new LocalWeaponMotion.ContactBounds(b.minRight()*s,b.minUp(),b.minForward()*s,b.maxRight()*s,b.maxUp(),b.maxForward()*s);
    }
    public static Vec3 visualTip(Player p,Vec3 feet,Vec3 tip){
        var delta=tip.subtract(feet);double s=scale(p);return feet.add(delta.x*s,delta.y,delta.z*s);
    }
}
