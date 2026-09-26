package com.codex.blacklight.client;

import com.codex.blacklight.BlacklightNeoMod;
import com.codex.blacklight.integration.epicfight.BlacklightEpicFightAnimations;
import com.codex.blacklight.registry.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch;

import java.util.Map;
import java.util.WeakHashMap;

/** Installs and restores Prototype locomotion without restarting the active animation. */
@EventBusSubscriber(modid = BlacklightNeoMod.MOD_ID, value = Dist.CLIENT)
public final class ClientClawMotionEvents {
    private static final Map<AbstractClientPlayer, SavedMotions> SAVED = new WeakHashMap<>();

    private ClientClawMotionEvents() { }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            SAVED.clear();
            return;
        }

        for (AbstractClientPlayer player : minecraft.level.players()) {
            AbstractClientPlayerPatch<?> patch = EpicFightCapabilities.getEntityPatch(
                    player, AbstractClientPlayerPatch.class);
            if (patch == null) continue;
            var state = player.getData(ModAttachments.PLAYER_STATE);
            boolean prototype = state.isInfected() && state.isPrototypeMode();
            if (prototype) install(player, patch);
            else restore(player, patch);
        }
    }

    private static void install(AbstractClientPlayer player, AbstractClientPlayerPatch<?> patch) {
        if (BlacklightEpicFightAnimations.CLAW_WALK == null
                || BlacklightEpicFightAnimations.CLAW_SPRINT == null
                || BlacklightEpicFightAnimations.CHARGED_JUMP == null
                || BlacklightEpicFightAnimations.JUMP_RISE == null
                || BlacklightEpicFightAnimations.JUMP_FALL == null
                || BlacklightEpicFightAnimations.GLIDE == null
                || BlacklightEpicFightAnimations.WALL_RUN == null) return;
        Animator animator = patch.getAnimator();
        Map<LivingMotion, AssetAccessor<? extends StaticAnimation>> motions =
                animator.getLivingAnimations();
        SavedMotions saved = SAVED.get(player);
        if (saved == null) {
            saved = new SavedMotions(motions.get(LivingMotions.WALK),
                    motions.get(LivingMotions.RUN));
            SAVED.put(player, saved);
        }
        // ClientAnimator.addLivingAnimation immediately restarts the clip when its motion
        // is already active. Installing unconditionally each tick therefore pinned WALK
        // and RUN to frame zero and produced visible foot sliding. Reinstall only after
        // Epic Fight/equipment has actually replaced one of our mappings.
        // Do not replace IDLE while the player is still joining. Epic Fight's current
        // animation can be its zero-duration EMPTY clip at that point; transitioning
        // away from it makes ConcurrentLinkAnimation calculate NaN and spin forever in
        // AnimationClip's binary search, leaving the client on "Loading terrain".
        var base=animator.getPlayerFor(BlacklightEpicFightAnimations.CLAW_IDLE);
        if(player.tickCount>20&&base!=null&&base.getRealAnimation()!=null
                &&base.getRealAnimation().get().getTotalTime()>0){
            installIfChanged(animator,motions,LivingMotions.IDLE,BlacklightEpicFightAnimations.CLAW_IDLE);
        }
        installIfChanged(animator, motions, LivingMotions.WALK,
                BlacklightEpicFightAnimations.CLAW_WALK);
        installIfChanged(animator, motions, LivingMotions.RUN,
                BlacklightEpicFightAnimations.CLAW_SPRINT);
        installIfChanged(animator, motions, LivingMotions.KNEEL,
                BlacklightEpicFightAnimations.CHARGED_JUMP);
        installIfChanged(animator, motions, LivingMotions.JUMP,
                switch(ClientWallMotion.modeFor(player)){
                    case MANTLE -> BlacklightEpicFightAnimations.WALL_MANTLE;
                    case VAULT -> BlacklightEpicFightAnimations.WALL_VAULT;
                    default -> BlacklightEpicFightAnimations.JUMP_RISE;
                });
        installIfChanged(animator, motions, LivingMotions.FALL,
                BlacklightEpicFightAnimations.JUMP_FALL);
        installIfChanged(animator, motions, LivingMotions.CLIMB,
                switch (ClientWallMotion.modeFor(player)) {
                    case RUN -> BlacklightEpicFightAnimations.WALL_RUN;
                    case CLING -> BlacklightEpicFightAnimations.WALL_CLING;
                    case LEAP -> BlacklightEpicFightAnimations.WALL_LEAP;
                    case MANTLE -> BlacklightEpicFightAnimations.WALL_MANTLE;
                    case VAULT -> BlacklightEpicFightAnimations.WALL_VAULT;
                    default -> BlacklightEpicFightAnimations.WALL_CLIMB;
                });
        installIfChanged(animator, motions, LivingMotions.FLY,
                BlacklightEpicFightAnimations.GLIDE);
    }

    private static void installIfChanged(
            Animator animator,
            Map<LivingMotion, AssetAccessor<? extends StaticAnimation>> motions,
            LivingMotion motion,
            AssetAccessor<? extends StaticAnimation> animation) {
        if (motions.get(motion) != animation) animator.addLivingAnimation(motion, animation);
    }

    private static void restore(AbstractClientPlayer player, AbstractClientPlayerPatch<?> patch) {
        SavedMotions saved = SAVED.remove(player);
        if (saved == null) return;
        // The saved walk/run accessors are kept purely as an "install happened"
        // marker: Epic Fight's resetLivingAnimations rebuilds the equipment-dependent
        // defaults, so per-motion restoration is unnecessary (and its immutable
        // living-animation map cannot be mutated anyway).
        patch.getAnimator().resetLivingAnimations();
        patch.updateMotion(true);
    }

    private record SavedMotions(AssetAccessor<? extends StaticAnimation> walk,
                                AssetAccessor<? extends StaticAnimation> run) { }
}
