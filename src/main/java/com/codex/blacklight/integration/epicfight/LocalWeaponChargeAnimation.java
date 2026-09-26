package com.codex.blacklight.integration.epicfight;

import com.codex.blacklight.combat.LocalVisualMotion;
import com.codex.blacklight.combat.LocalWeaponMotion;
import com.codex.blacklight.combat.WeaponMove;
import com.codex.blacklight.registry.ModAttachments;
import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.property.AnimationProperty.ActionAnimationProperty;
import yesman.epicfight.api.animation.types.ActionAnimation;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.asset.JsonAssetLoader;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/** Held windup pose; only the authoritative release starts the separate damaging action. */
public final class LocalWeaponChargeAnimation extends ActionAnimation {
    private final WeaponMove move;
    private final String motionId;
    private final LocalVisualMotion.Definition definition;

    public LocalWeaponChargeAnimation(AnimationAccessor<? extends ActionAnimation> accessor, WeaponMove move) {
        super(chargeMotion(move).transitionSeconds(), accessor, Armatures.BIPED);
        this.move = move;
        this.motionId = LocalWeaponMotion.find(move.id).orElseThrow().charge().orElseThrow().animation();
        this.definition = chargeMotion(move);
        addProperty(ActionAnimationProperty.MOVE_ON_LINK, false);
        addProperty(ActionAnimationProperty.MOVE_VERTICAL, false);
        addProperty(ActionAnimationProperty.STOP_MOVEMENT, !move.aerial());
    }

    private static LocalVisualMotion.Definition chargeMotion(WeaponMove move) {
        String id = LocalWeaponMotion.find(move.id).orElseThrow().charge().orElseThrow().animation();
        return LocalVisualMotion.find(id).orElseThrow(() -> new IllegalStateException("Missing charge motion: " + id));
    }

    @Override public void loadAnimation() {
        var loader = new JsonAssetLoader(definition.animation().deepCopy(),
                ResourceLocation.fromNamespaceAndPath("blacklight", "local/" + motionId));
        animationClip = loader.loadClipForAnimation(this);
        animationClip.bakeKeyframes();
    }

    @Override public void tick(LivingEntityPatch<?> patch) {
        var entity = patch.getOriginal();
        var visual = entity.getData(ModAttachments.WEAPON_VISUAL);
        if (visual.spec() == move && visual.charging()) {
            float clock = (entity.level().getGameTime() - visual.started()) / 20F - definition.transitionSeconds();
            // Keep the action alive at its final pose while waiting for server release.
            float time = Math.clamp(clock, 0, Math.max(0, getTotalTime() - .0001F));
            patch.getAnimator().getPlayerFor(getAccessor()).setElapsedTime(time, time);
        }
        super.tick(patch);
    }

    @Override public float getPlaySpeed(LivingEntityPatch<?> patch, DynamicAnimation animation) {
        // ActionAnimation computes link duration from speed; zero here would divide by zero.
        return animation.isLinkAnimation() ? 1F : 0F;
    }

    @Override protected boolean validateMovement(LivingEntityPatch<?> patch,
                                                 AssetAccessor<? extends DynamicAnimation> animation) {
        // Charge Coord is a pose reference. Replaying it would launch the actor before release.
        return false;
    }
}
