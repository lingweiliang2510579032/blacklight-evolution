package com.codex.blacklight.integration.epicfight;

import com.codex.blacklight.combat.LocalVisualMotion;
import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.JsonAssetLoader;
import yesman.epicfight.gameasset.Armatures;

/** Pose-only local source clips: ImpactControl exclusively owns the target's physics. */
public final class NativeHitAnimation extends StaticAnimation {
    public static final String[] KEYS={"reaction_body","reaction_left","reaction_right","reaction_backhand","reaction_up","reaction_fall","reaction_down","reaction_rise"};
    private final String id;
    public NativeHitAnimation(AnimationAccessor<? extends StaticAnimation> accessor,String id){super(.04F,false,accessor,Armatures.BIPED);this.id=id;}
    @Override public void loadAnimation(){
        var data=LocalVisualMotion.find(id).orElseThrow(()->new IllegalStateException("Missing local reaction: "+id));
        animationClip=new JsonAssetLoader(data.animation().deepCopy(),ResourceLocation.fromNamespaceAndPath("blacklight","local/"+id)).loadClipForAnimation(this);
        animationClip.bakeKeyframes();
    }
}
