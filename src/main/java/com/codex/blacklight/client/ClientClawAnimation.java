package com.codex.blacklight.client;

import com.codex.blacklight.BlacklightNeoMod;
import com.codex.blacklight.integration.epicfight.BlacklightEpicFightAnimations;
import com.codex.blacklight.network.ClawAnimationPayload;
import net.minecraft.client.Minecraft;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

/** Starts server-approved claw visuals locally and reports the actual animator layer. */
public final class ClientClawAnimation {
    private ClientClawAnimation() { }

    public static void accept(ClawAnimationPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        AbstractClientPlayerPatch<?> patch = EpicFightCapabilities.getEntityPatch(
                minecraft.player, AbstractClientPlayerPatch.class);
        if (patch == null) {
            BlacklightNeoMod.LOGGER.warn("Claw visual {} arrived before the client player patch",
                    payload.animation());
            return;
        }

        AnimationAccessor<? extends StaticAnimation> animation = switch (payload.animation()) {
            case TRANSFORM -> BlacklightEpicFightAnimations.TRANSFORM_CLAWS;
            case AUTO_1 -> BlacklightEpicFightAnimations.CLAW_AUTO_1;
            case AUTO_2 -> BlacklightEpicFightAnimations.CLAW_AUTO_2;
            case AUTO_3 -> BlacklightEpicFightAnimations.CLAW_AUTO_3;
            case GROUND_SPIKE -> BlacklightEpicFightAnimations.CLAW_GROUNDSPIKE;
            case SPRINT_ATTACK -> BlacklightEpicFightAnimations.CLAW_SPRINT_ATTACK;
            case DASHING_SLICE -> BlacklightEpicFightAnimations.CLAW_DASHING_SLICE;
            case CHARGED_JUMP -> BlacklightEpicFightAnimations.CHARGED_JUMP;
        };
        if (animation == null) return;

        Animator animator = patch.getAnimator();
        boolean synchronizedPlayAlreadyArrived = animator.isPlaying(animation);
        if (!synchronizedPlayAlreadyArrived) animator.playAnimation(animation, 0.0F);
        BlacklightNeoMod.LOGGER.debug(
                "Claw visual accepted={} alreadyPlaying={} layer={} priority={} clipTime={}",
                payload.animation(), synchronizedPlayAlreadyArrived,
                animation.get().getLayerType(), animation.get().getPriority(),
                animation.get().getTotalTime());
    }
}
