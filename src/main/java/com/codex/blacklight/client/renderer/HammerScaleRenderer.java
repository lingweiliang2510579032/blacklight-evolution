package com.codex.blacklight.client.renderer;

import com.codex.blacklight.combat.*;
import com.codex.blacklight.power.PowerForm;
import com.codex.blacklight.registry.ModAttachments;
import net.minecraft.client.player.AbstractClientPlayer;
import java.util.*;

/** Shares the active body's animation clock; no independent charge timer or cumulative mesh edits. */
public final class HammerScaleRenderer {
    private record Frame(float time,HammerScales.Shape right,HammerScales.Shape left) {}
    private static final Map<AbstractClientPlayer,Frame> states=new WeakHashMap<>();
    private HammerScaleRenderer() {}
    public static HammerScales.Shape[] sample(AbstractClientPlayer player,float partial) {
        var state=com.codex.blacklight.registry.ModAttachments.getData(player, ModAttachments.PLAYER_STATE);
        var identity=HammerScales.Shape.IDENTITY;
        if(!player.isAlive()||!state.isPrototypeMode()||state.getActivePower()!=PowerForm.HAMMERFISTS) {
            states.remove(player);return new HammerScales.Shape[]{identity,identity};
        }
        var visual=com.codex.blacklight.registry.ModAttachments.getData(player, ModAttachments.WEAPON_VISUAL);var move=visual.spec();
        String key="";float clipTime=0;
        if(move!=null&&move.form==PowerForm.HAMMERFISTS) {
            key=visual.charging()?LocalWeaponMotion.find(move.id).flatMap(LocalWeaponMotion.Definition::charge)
                    .map(LocalWeaponMotion.Charge::animation).orElse(""):move.id;
            clipTime=Math.max(visual.clipOffset(),(player.level().getGameTime()-visual.started()+partial)/20F-.05F);
        }
        if(!HammerScales.contains(key))key="";
        var right=HammerScales.sample(key,clipTime,false);var left=HammerScales.sample(key,clipTime,true);
        float now=player.tickCount+partial;var old=states.get(player);
        if(key.isEmpty()&&old!=null&&now>=old.time&&now-old.time<5) {
            // Only interrupted/finished actions blend back. Active source keys stay on
            // the exact body clock, including the hold-to-charged-release blend.
            float blend=net.minecraft.util.Mth.clamp((now-old.time)/1.5F,0,1);
            right=old.right.blend(right,blend);left=old.left.blend(left,blend);
        }
        states.put(player,new Frame(now,right,left));
        return new HammerScales.Shape[]{right,left};
    }
}
