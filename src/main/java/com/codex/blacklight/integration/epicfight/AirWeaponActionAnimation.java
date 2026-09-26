package com.codex.blacklight.integration.epicfight;

import com.codex.blacklight.combat.WeaponMove;
import com.codex.blacklight.registry.ModAttachments;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.ActionAnimation;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/** The descent holds the raised weapon. The server rebases the clock at landing. */
public final class AirWeaponActionAnimation extends ActionAnimation {
    private final AnimationAccessor<? extends ActionAnimation> accessor;
    private final WeaponMove move;
    public AirWeaponActionAnimation(AnimationAccessor<? extends ActionAnimation> accessor,WeaponMove move){super(.06F,accessor,Armatures.BIPED);this.accessor=accessor;this.move=move;}
    @Override public void tick(LivingEntityPatch<?> patch){
        super.tick(patch);
        var entity=patch.getOriginal();var visual=entity.getData(ModAttachments.WEAPON_VISUAL);
        if(visual.spec()!=move)return;
        float time=(entity.level().getGameTime()-visual.started())/20F;
        if(!entity.onGround())time=Math.min(time,move.start/20F-.10F);
        time=Math.clamp(time,0,(move.duration-1)/20F);
        patch.getAnimator().getPlayerFor(accessor).setElapsedTime(time,time);
    }
    @Override public float getPlaySpeed(LivingEntityPatch<?> patch,DynamicAnimation animation){return 0;}
}
