package com.codex.blacklight.client.renderer;

import com.codex.blacklight.combat.*;
import com.codex.blacklight.integration.epicfight.BlacklightEpicFightAnimations;
import com.codex.blacklight.power.PowerForm;
import com.codex.blacklight.registry.ModAttachments;
import net.minecraft.client.player.AbstractClientPlayer;
import yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch;
import java.util.*;

/** Shares the active body's animation clock; no independent charge timer or cumulative mesh edits. */
public final class HammerScaleRenderer {
    private record Frame(float time,HammerScales.Shape right,HammerScales.Shape left) {}
    private static final Map<AbstractClientPlayer,Frame> states=new WeakHashMap<>();
    private HammerScaleRenderer() {}
    public static HammerScales.Shape[] sample(AbstractClientPlayerPatch<AbstractClientPlayer> patch,float partial) {
        var player=patch.getOriginal();var state=player.getData(ModAttachments.PLAYER_STATE);
        var identity=HammerScales.Shape.IDENTITY;
        if(!player.isAlive()||!state.isPrototypeMode()||state.getActivePower()!=PowerForm.HAMMERFISTS) {
            states.remove(player);return new HammerScales.Shape[]{identity,identity};
        }
        var visual=player.getData(ModAttachments.WEAPON_VISUAL);var move=visual.spec();
        String key="";float clipTime=0;
        if(move!=null&&move.form==PowerForm.HAMMERFISTS) {
            key=visual.charging()?LocalWeaponMotion.find(move.id).flatMap(LocalWeaponMotion.Definition::charge)
                    .map(LocalWeaponMotion.Charge::animation).orElse(""):move.id;
            var accessor=visual.charging()?BlacklightEpicFightAnimations.weaponCharge(move):BlacklightEpicFightAnimations.weapon(move);
            var animation=accessor==null?null:patch.getAnimator().getPlayerFor(accessor);
            if(animation!=null&&animation.getRealAnimation()!=null&&animation.getRealAnimation().get()==accessor.get()
                    &&!animation.getAnimation().get().isLinkAnimation()) {
                clipTime=net.minecraft.util.Mth.lerp(partial,animation.getPrevElapsedTime(),animation.getElapsedTime());
            }else {
                // During a link, seek the destination's entry/charge offset.
                clipTime=Math.max(visual.clipOffset(),(player.level().getGameTime()-visual.started()+partial)/20F-.05F);
            }
        }
        if(!HammerScales.contains(key))key="";
        var right=HammerScales.sample(key,clipTime,false);var left=HammerScales.sample(key,clipTime,true);
        float now=player.tickCount+partial;var old=states.get(player);
        if(key.isEmpty()&&old!=null&&now>=old.time&&now-old.time<5) {
            // Only interrupted/finished actions blend back. Active source keys stay on
            // the exact body clock, including the hold-to-charged-release blend.
            float blend=Math.clamp((now-old.time)/1.5F,0,1);
            right=old.right.blend(right,blend);left=old.left.blend(left,blend);
        }
        states.put(player,new Frame(now,right,left));
        return new HammerScales.Shape[]{right,left};
    }
}
