package com.codex.blacklight.combat;

import com.codex.blacklight.entity.BlacklightHunter;
import com.codex.blacklight.integration.epicfight.BlacklightEpicFightAnimations;
import com.codex.blacklight.registry.ModAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.phys.Vec3;
import com.codex.blacklight.power.PowerForm;
import net.minecraft.world.entity.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import java.util.*;

/** Server owns claims and rewards. Clients send intent only, never a target ID or reward. */
public final class ConsumeSystem {
    public static final int DURATION = 46;
    public static final float HEALTH_LIMIT = .30F;
    private static final Map<ServerPlayer, Session> ACTIVE = new HashMap<>();
    public static final TagKey<EntityType<?>> CONSUMABLE = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("blacklight", "consumable"));
    public static final TagKey<EntityType<?>> PASSIVE = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("blacklight", "consumable_without_weakening"));
    private record Session(Mob victim, Vec3 start, int started, PowerForm form, int duration, float heal, int biomass, int ep) {}

    public static boolean eligible(Mob mob) {
        return mob.isAlive() && !mob.isRemoved() && !mob.isInvulnerable() && mob.getType().is(CONSUMABLE)
                && !mob.getData(ModAttachments.CONSUME_VISUAL).active()
                && !(mob instanceof BlacklightHunter h && h.isBeingConsumed())
                && !mob.isPassenger() && !mob.isVehicle() && !mob.isLeashed()
                && !(mob instanceof TamableAnimal pet && pet.isTame())
                && !(mob instanceof net.minecraft.world.entity.animal.horse.AbstractHorse horse && horse.isTamed());
    }
    private ConsumeSystem() {}

    public static void request(ServerPlayer player) {
        var state = player.getData(ModAttachments.PLAYER_STATE);
        if (!player.isAlive() || player.isSpectator() || !state.isInfected() || !state.isPrototypeMode()
                || ACTIVE.containsKey(player) || player.isPassenger() || !player.onGround()
                || player.getData(ModAttachments.CLAW_COMBAT).action() != ClawCombatState.Action.IDLE
                || player.getData(ModAttachments.MOVEMENT_STATE).wallTraversal().attached()) return;
        var form = state.getActivePower();
        double range = form == PowerForm.WHIPFIST ? 6 : 3;
        var target = player.serverLevel().getEntitiesOfClass(Mob.class,
                player.getBoundingBox().inflate(range), h -> eligible(h)
                        && player.distanceToSqr(h) <= range * range && player.hasLineOfSight(h)
                        && player.getLookAngle().dot(h.getBoundingBox().getCenter().subtract(player.getEyePosition()).normalize()) > .65)
                .stream().min(Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
        if (target == null) { message(player, "consume_no_target"); return; }
        if (!target.getType().is(PASSIVE) && target.getHealth() > target.getMaxHealth() * HEALTH_LIMIT) { message(player, "consume_weaken"); return; }
        if (target instanceof BlacklightHunter hunter && !hunter.claim(player)) return;
        int duration = ConsumeVisual.duration(form);
        boolean hunter = target instanceof BlacklightHunter;
        ACTIVE.put(player, new Session(target, player.position(), player.tickCount, form, duration,
                hunter ? 8 : Math.clamp(target.getMaxHealth() * .2F, 2, 6),
                hunter ? 120 : Math.clamp((int) target.getMaxHealth() * 2, 10, 60),
                hunter ? 30 : target.getType().is(PASSIVE) ? 2 : 10));
        target.getNavigation().stop(); target.setTarget(null);
        target.setData(ModAttachments.CONSUME_VISUAL, new ConsumeVisual(player.getId(), player.level().getGameTime(), duration, form.ordinal()));
        target.syncData(ModAttachments.CONSUME_VISUAL);
        player.getData(ModAttachments.MOVEMENT_STATE).reset();
        player.setData(ModAttachments.CONSUMING, true); player.syncData(ModAttachments.CONSUMING);
        var direction = target.position().subtract(player.position());
        player.setYRot((float) Math.toDegrees(Math.atan2(-direction.x, direction.z)));
        player.setYHeadRot(player.getYRot()); player.yBodyRot = player.getYRot();
        target.setYRot(player.getYRot()+180); target.setYHeadRot(target.getYRot()); target.yBodyRot=target.getYRot();
        var patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (patch != null) patch.playAnimationSynchronized(BlacklightEpicFightAnimations.consume(form), 0);
        player.level().playSound(null, target.blockPosition(), SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.PLAYERS, .9F, .65F);
    }

    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Session session = ACTIVE.get(player); if (session == null) return;
        Mob target = session.victim();
        if (!player.isAlive() || player.isSpectator() || !player.getData(ModAttachments.PLAYER_STATE).isPrototypeMode()
                || !target.isAlive() || target.isRemoved() || target.level() != player.level()
                || target.isInvulnerable() || target.isPassenger() || target.isVehicle()
                || player.getData(ModAttachments.PLAYER_STATE).getActivePower() != session.form()
                || target.getData(ModAttachments.CONSUME_VISUAL).consumerId() != player.getId()
                || player.distanceToSqr(target) > (session.form() == PowerForm.WHIPFIST ? 42.25 : 12.25)
                || player.position().distanceToSqr(session.start()) > 1 || !player.hasLineOfSight(target)) {
            cancel(player); return;
        }
        player.setDeltaMovement(0, player.getDeltaMovement().y, 0);
        target.setDeltaMovement(0, target.getDeltaMovement().y, 0);
        int elapsed = player.tickCount - session.started();
        if (target instanceof BlacklightHunter hunter) hunter.setConsumeTicks(elapsed);
        if (session.form() == PowerForm.WHIPFIST ? elapsed >= 14 && elapsed < 40 : elapsed < 12) {
            Vec3 toward = player.position().subtract(target.position()).multiply(1,0,1);
            double distance = toward.length();
            if (distance > 1.05) target.move(net.minecraft.world.entity.MoverType.SELF,
                    toward.normalize().scale(Math.min(session.form() == PowerForm.WHIPFIST ? .30 : .18, distance - 1.05)));
        }
        if (session.form() == PowerForm.HAMMERFISTS && elapsed == 18) {
            var floor=target.blockPosition().below();
            player.serverLevel().sendParticles(new net.minecraft.core.particles.BlockParticleOption(
                    net.minecraft.core.particles.ParticleTypes.BLOCK,player.level().getBlockState(floor)),
                    target.getX(),target.getY()+.08,target.getZ(),26,.45,.08,.45,.12);
            player.level().playSound(null,target.blockPosition(),SoundEvents.GENERIC_BIG_FALL,SoundSource.PLAYERS,1.15F,.6F);
        }
        // A wall or obstruction may stop the collision-aware pull; never consume remotely through it.
        if (elapsed >= session.duration() / 2 && player.distanceToSqr(target) > 6.25) { cancel(player); return; }
        if (elapsed < session.duration()) return;
        // Remove the session before applying rewards: reentrant events/duplicate packets cannot pay twice.
        ACTIVE.remove(player);
        target.discard();
        unlock(player);
        var state = player.getData(ModAttachments.PLAYER_STATE);
        state.addBiomass(session.biomass()); state.addEvolutionPoints(session.ep());
        player.setData(ModAttachments.PLAYER_STATE, state); player.syncData(ModAttachments.PLAYER_STATE);
        player.heal(session.heal());
        player.level().playSound(null, player.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, .8F, .55F);
        player.displayClientMessage(Component.translatable("message.blacklight.consume_complete", session.heal() / 2, session.biomass(), session.ep()), true);
    }
    public static void cancel(ServerPlayer player) {
        Session session = ACTIVE.remove(player);
        if (session == null) return;
        if (session.victim() instanceof BlacklightHunter hunter) hunter.release();
        session.victim().setData(ModAttachments.CONSUME_VISUAL, ConsumeVisual.EMPTY);
        if (!session.victim().isRemoved()) session.victim().syncData(ModAttachments.CONSUME_VISUAL);
        unlock(player);
        var patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (patch != null && player.isAlive() && player.getData(ModAttachments.PLAYER_STATE).isPrototypeMode())
            patch.playAnimationSynchronized(BlacklightEpicFightAnimations.CLAW_IDLE, 0);
        message(player, "consume_cancelled");
    }
    private static void unlock(ServerPlayer p) { p.setData(ModAttachments.CONSUMING, false); p.syncData(ModAttachments.CONSUMING); }
    private static void message(ServerPlayer p, String key) { p.displayClientMessage(Component.translatable("message.blacklight." + key), true); }
    @SubscribeEvent public static void hurt(LivingDamageEvent.Post event) {
        if (event.getNewDamage() <= 0) return;
        if (event.getEntity() instanceof ServerPlayer p) cancel(p);
        if (event.getEntity().getData(ModAttachments.CONSUME_VISUAL).active()) {
            for (var entry : List.copyOf(ACTIVE.entrySet())) if (entry.getValue().victim() == event.getEntity()) cancel(entry.getKey());
        }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e) { if (e.getEntity() instanceof ServerPlayer p) cancel(p); }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e) { if (e.getEntity() instanceof ServerPlayer p) cancel(p); }
    /** Freeze the captured mob without writing NoAI, gravity, or invulnerability into its saved NBT. */
    @SubscribeEvent public static void freeze(net.neoforged.neoforge.event.tick.EntityTickEvent.Pre event) {
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof Mob mob)) return;
        var visual = mob.getData(ModAttachments.CONSUME_VISUAL);
        if (!visual.active()) return;
        boolean claimed = ACTIVE.values().stream().anyMatch(session -> session.victim() == mob);
        if (claimed) event.setCanceled(true);
        else {
            mob.setData(ModAttachments.CONSUME_VISUAL, ConsumeVisual.EMPTY); mob.syncData(ModAttachments.CONSUME_VISUAL);
            if (mob instanceof BlacklightHunter hunter) hunter.release();
        }
    }
    @SubscribeEvent public static void removed(net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        for (var entry : List.copyOf(ACTIVE.entrySet()))
            if (entry.getValue().victim() == event.getEntity() || entry.getKey() == event.getEntity()) cancel(entry.getKey());
    }
    @SubscribeEvent public static void stopping(net.neoforged.neoforge.event.server.ServerStoppingEvent event) {
        for (var player : List.copyOf(ACTIVE.keySet())) cancel(player);
    }
}
