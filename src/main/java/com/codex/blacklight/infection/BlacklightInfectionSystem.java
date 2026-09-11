package com.codex.blacklight.infection;

import com.codex.blacklight.registry.ModAttachments;
import com.codex.blacklight.registry.ModEffects;
import com.codex.blacklight.state.BlacklightState;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Comparator;

public final class BlacklightInfectionSystem {
    private static final double HUNT_RADIUS = 14.0D;
    private static final float CONTACT_SPREAD_CHANCE = 0.35F;

    private BlacklightInfectionSystem() {
    }

    public static boolean infect(LivingEntity target, Entity source) {
        if (!target.isAlive()) return false;

        if (target instanceof Player player) {
            if (player.isCreative() || player.isSpectator() || !player.isAlive()) return false;
            BlacklightState state = player.getData(ModAttachments.PLAYER_STATE);
            if (state.isInfected()) return false;
            state.infect();
            player.setData(ModAttachments.PLAYER_STATE, state);
            player.syncData(ModAttachments.PLAYER_STATE);
            player.displayClientMessage(Component.translatable("message.blacklight.infected"), false);
            return true;
        }

        if (target.hasEffect(ModEffects.BLACKLIGHT_INFECTION)) return false;
        return target.addEffect(new MobEffectInstance(
                ModEffects.BLACKLIGHT_INFECTION,
                MobEffectInstance.INFINITE_DURATION,
                0,
                false,
                true,
                false), source);
    }

    public static boolean isInfected(LivingEntity entity) {
        if (entity instanceof Player player) {
            return player.getData(ModAttachments.PLAYER_STATE).isInfected();
        }
        return entity.hasEffect(ModEffects.BLACKLIGHT_INFECTION);
    }

    public static void notifyBurstOwner(Entity owner, int exposedCount) {
        if (owner instanceof Player player) {
            player.displayClientMessage(Component.translatable(
                    "message.blacklight.vial_burst", exposedCount), true);
        }
    }

    @SubscribeEvent
    public static void onContactDamage(LivingDamageEvent.Post event) {
        if (event.getNewDamage() <= 0.0F || isInfected(event.getEntity())) return;
        Entity directSource = event.getSource().getEntity();
        if (!(directSource instanceof LivingEntity attacker) || !isInfected(attacker)) return;
        if (attacker.getRandom().nextFloat() < CONTACT_SPREAD_CHANCE) {
            infect(event.getEntity(), attacker);
        }
    }

    @SubscribeEvent
    public static void onInfectedMobTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob)
                || !(mob.level() instanceof ServerLevel serverLevel)
                || !isInfected(mob)
                || mob.tickCount % 20 != 0) return;

        LivingEntity target = mob.getTarget();
        if (!isValidTarget(mob, target)) {
            target = serverLevel.getEntitiesOfClass(
                            LivingEntity.class,
                            mob.getBoundingBox().inflate(HUNT_RADIUS, HUNT_RADIUS * 0.5D, HUNT_RADIUS),
                            candidate -> isValidTarget(mob, candidate))
                    .stream()
                    .min(Comparator.comparingDouble(mob::distanceToSqr))
                    .orElse(null);
        }

        if (target == null) return;
        mob.setTarget(target);
        mob.getNavigation().moveTo(target, 1.2D);
        if (mob.distanceToSqr(target) <= 5.0D && mob.hasLineOfSight(target)
                && mob.tickCount % 40 == 0) {
            mob.swing(InteractionHand.MAIN_HAND);
            target.hurt(mob.damageSources().mobAttack(mob), 3.0F);
        }
    }

    private static boolean isValidTarget(Mob attacker, LivingEntity candidate) {
        if (candidate == null || candidate == attacker || !candidate.isAlive() || isInfected(candidate)) return false;
        return !(candidate instanceof Player player)
                || (!player.isCreative() && !player.isSpectator()
                    && (attacker.level().getServer() == null
                        || !attacker.level().getServer().isPvpAllowed()));
    }
}
