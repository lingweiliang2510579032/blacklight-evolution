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
    private static final Map<Mob, Thrown> THROWN = new HashMap<>();
    private record Thrown(ServerPlayer owner, int remaining, Vec3 velocity) {}
    private static final class Session {
        final Mob victim; final PowerForm form; final float heal; final int biomass, ep;
        Vec3 start; int started, stage=ConsumeVisual.CAPTURE;
        Session(Mob v, ServerPlayer p, PowerForm f) {
            victim=v; form=f; start=p.position(); started=p.tickCount;
            boolean hunter=v instanceof BlacklightHunter;
            heal=hunter?8:Math.clamp(v.getMaxHealth()*.2F,2,6);
            biomass=hunter?120:Math.clamp((int)v.getMaxHealth()*2,10,60);
            ep=hunter?30:v.getType().is(PASSIVE)?2:10;
        }
        Mob victim(){return victim;} PowerForm form(){return form;}
        Vec3 start(){return start;} int started(){return started;}
        int duration(){return ConsumeVisual.duration(form);}
        float heal(){return heal;} int biomass(){return biomass;} int ep(){return ep;}
    }
    public static boolean holding(ServerPlayer player) {
        var session=ACTIVE.get(player);return session!=null&&session.stage==ConsumeVisual.HOLD;
    }
    private static void phase(ServerPlayer p,Session s,int stage) {
        s.stage=stage;s.started=p.tickCount;s.start=p.position();
        int duration=stage==ConsumeVisual.CAPTURE?ConsumeVisual.captureDuration(s.form):stage==ConsumeVisual.THROW?14:s.duration();
        s.victim.setData(ModAttachments.CONSUME_VISUAL,new ConsumeVisual(p.getId(),p.level().getGameTime(),duration,s.form.ordinal(),stage));
        s.victim.syncData(ModAttachments.CONSUME_VISUAL);
        p.setData(ModAttachments.GRAB_STAGE,stage);p.syncData(ModAttachments.GRAB_STAGE);
    }
    private static void play(ServerPlayer p,yesman.epicfight.api.animation.AnimationManager.AnimationAccessor<? extends yesman.epicfight.api.animation.types.StaticAnimation> animation) {
        var patch=EpicFightCapabilities.getEntityPatch(p,ServerPlayerPatch.class);
        if(patch!=null)patch.playAnimationSynchronized(animation,0);
    }
    /** Right click confirms a held target. It can never acquire or auto-consume a new target. */
    public static void consume(ServerPlayer player) {
        var s=ACTIVE.get(player);if(s==null||s.stage!=ConsumeVisual.HOLD||!valid(player,s))return;
        phase(player,s,ConsumeVisual.CONSUME);play(player,BlacklightEpicFightAnimations.consume(s.form));
    }
    /** E again or primary attack starts a throw; release is synchronized to the arm swing. */
    public static void throwTarget(ServerPlayer player) {
        var s=ACTIVE.get(player);if(s==null||s.stage!=ConsumeVisual.HOLD||!valid(player,s))return;
        phase(player,s,ConsumeVisual.THROW);play(player,BlacklightEpicFightAnimations.GRAB_THROW);
    }
    private static boolean valid(ServerPlayer p,Session s) {
        var v=s.victim;var state=p.getData(ModAttachments.PLAYER_STATE);
        return p.isAlive()&&!p.isSpectator()&&!p.isPassenger()&&state.isInfected()&&state.isPrototypeMode()
                &&state.getActivePower()==s.form&&v.isAlive()&&!v.isRemoved()&&v.level()==p.level()
                &&!v.isInvulnerable()&&!v.isPassenger()&&!v.isVehicle()&&!v.isLeashed()
                &&v.getData(ModAttachments.CONSUME_VISUAL).consumerId()==p.getId()
                &&p.distanceToSqr(v)<=(s.form==PowerForm.WHIPFIST?42.25:12.25)&&p.hasLineOfSight(v);
    }
    private static Vec3 anchor(ServerPlayer p,Mob victim) {
        Vec3 forward=Vec3.directionFromRotation(0,p.getYRot());
        double lift=Math.clamp(1.3-victim.getBbHeight()*.72,0,.65);
        return p.position().add(forward.scale(1.05)).add(forward.z*.28,lift,-forward.x*.28);
    }
    private static void carry(ServerPlayer p,Mob v,double speed) {
        Vec3 delta=anchor(p,v).subtract(v.position());
        v.move(MoverType.SELF,delta.length()>speed?delta.normalize().scale(speed):delta);
        v.setDeltaMovement(Vec3.ZERO);v.fallDistance=0;
        v.setYRot(p.getYRot()+180);v.setYHeadRot(v.getYRot());v.yBodyRot=v.getYRot();
    }

    public static boolean eligible(Mob mob) {
        return !THROWN.containsKey(mob) && mob.isAlive() && !mob.isRemoved() && !mob.isInvulnerable() && mob.getType().is(CONSUMABLE)
                && !mob.getData(ModAttachments.CONSUME_VISUAL).active()
                && !(mob instanceof BlacklightHunter h && h.isBeingConsumed())
                && !mob.isPassenger() && !mob.isVehicle() && !mob.isLeashed()
                && !(mob instanceof TamableAnimal pet && pet.isTame())
                && !(mob instanceof net.minecraft.world.entity.animal.horse.AbstractHorse horse && horse.isTamed());
    }
    private ConsumeSystem() {}

    public static void request(ServerPlayer player) {
        if (holding(player)) { throwTarget(player); return; }
        var state = player.getData(ModAttachments.PLAYER_STATE);
        if (!player.isAlive() || player.isSpectator() || !state.isInfected() || !state.isPrototypeMode()
                || ACTIVE.containsKey(player) || player.isPassenger() || !player.onGround()
                || player.getData(ModAttachments.CLAW_COMBAT).action() != ClawCombatState.Action.IDLE
                || player.getData(ModAttachments.MOVEMENT_STATE).chargeTicks() > 0
                || player.getData(ModAttachments.MOVEMENT_STATE).dashTicks() > 0
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
        Session session=new Session(target,player,form);ACTIVE.put(player,session);
        target.getNavigation().stop();target.setTarget(null);
        phase(player,session,ConsumeVisual.CAPTURE);
        player.getData(ModAttachments.MOVEMENT_STATE).reset();
        player.setData(ModAttachments.CONSUMING, true); player.syncData(ModAttachments.CONSUMING);
        var direction = target.position().subtract(player.position());
        player.setYRot((float) Math.toDegrees(Math.atan2(-direction.x, direction.z)));
        player.setYHeadRot(player.getYRot()); player.yBodyRot = player.getYRot();
        target.setYRot(player.getYRot()+180); target.setYHeadRot(target.getYRot()); target.yBodyRot=target.getYRot();
        play(player,form==PowerForm.WHIPFIST?BlacklightEpicFightAnimations.GRAB_WHIP:BlacklightEpicFightAnimations.GRAB_REACH);
        player.level().playSound(null, target.blockPosition(), SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.PLAYERS, .9F, .65F);
    }

    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Session session = ACTIVE.get(player); if (session == null) return;
        Mob target = session.victim();
        if (!valid(player,session) || (session.stage!=ConsumeVisual.HOLD && player.position().distanceToSqr(session.start)>1)) {
            cancel(player);return;
        }
        int elapsed=player.tickCount-session.started;
        if(session.stage!=ConsumeVisual.HOLD)player.setDeltaMovement(0,player.getDeltaMovement().y,0);
        target.setDeltaMovement(Vec3.ZERO);
        if(session.stage==ConsumeVisual.CAPTURE) {
            if(session.form!=PowerForm.WHIPFIST||elapsed>=14)carry(player,target,session.form==PowerForm.WHIPFIST?.30:.24);
            if(elapsed>=ConsumeVisual.captureDuration(session.form)) {
                if(target.position().distanceToSqr(anchor(player,target))>.36){cancel(player);return;}
                phase(player,session,ConsumeVisual.HOLD);message(player,"grab_held");
            }
            return;
        }
        if(session.stage==ConsumeVisual.HOLD) {
            carry(player,target,.65);
            if(target.position().distanceToSqr(anchor(player,target))>1.0){cancel(player);return;}
            return;
        }
        if(session.stage==ConsumeVisual.THROW) {
            carry(player,target,.65);
            if(elapsed>=6) {
                ACTIVE.remove(player);release(target);unlock(player);
                target.setOnGround(false);target.setDeltaMovement(player.getLookAngle().scale(1.55).add(0,.28,0));target.hurtMarked=true;
                THROWN.put(target,new Thrown(player,40,target.getDeltaMovement()));
                target.setData(ModAttachments.CONSUME_VISUAL,new ConsumeVisual(player.getId(),player.level().getGameTime(),40,session.form.ordinal(),ConsumeVisual.FLIGHT));
                target.syncData(ModAttachments.CONSUME_VISUAL);
                player.level().playSound(null,target.blockPosition(),SoundEvents.PLAYER_ATTACK_SWEEP,SoundSource.PLAYERS,.8F,.7F);
            }
            return;
        }
        if(target instanceof BlacklightHunter hunter)hunter.setConsumeTicks(elapsed);
        // Lower the held body back to contact before the authored execution, respecting blocks.
        if(elapsed<8) {
            Vec3 delta=player.position().subtract(target.position()).multiply(1,0,1);
            if(delta.length()>1.05)target.move(MoverType.SELF,delta.normalize().scale(Math.min(.18,delta.length()-1.05)));
            target.move(MoverType.SELF,new Vec3(0,-.14,0));
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
        release(session.victim());
        unlock(player);
        var patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (patch != null && player.isAlive() && player.getData(ModAttachments.PLAYER_STATE).isPrototypeMode())
            patch.playAnimationSynchronized(BlacklightEpicFightAnimations.CLAW_IDLE, 0);
        message(player, "consume_cancelled");
    }
    private static void release(Mob victim) {
        if(victim instanceof BlacklightHunter hunter)hunter.release();
        victim.setData(ModAttachments.CONSUME_VISUAL,ConsumeVisual.EMPTY);
        if(!victim.isRemoved())victim.syncData(ModAttachments.CONSUME_VISUAL);
    }
    private static void unlock(ServerPlayer p) {
        p.setData(ModAttachments.CONSUMING,false);p.syncData(ModAttachments.CONSUMING);
        p.setData(ModAttachments.GRAB_STAGE,ConsumeVisual.NONE);p.syncData(ModAttachments.GRAB_STAGE);
    }
    /** Own the brief ballistic step so navigation and pre-existing NoAI cannot swallow launch velocity. */
    @SubscribeEvent public static void thrown(net.neoforged.neoforge.event.tick.EntityTickEvent.Pre event) {
        if(!(event.getEntity() instanceof Mob mob)||mob.level().isClientSide)return;
        var shot=THROWN.get(mob);if(shot==null)return;
        if(mob.isRemoved()||!mob.isAlive()||shot.remaining()<=0||shot.owner().level()!=mob.level()) {
            THROWN.remove(mob);release(mob);return;
        }
        event.setCanceled(true);
        Vec3 before=mob.position();
        mob.move(MoverType.SELF,shot.velocity());mob.setDeltaMovement(shot.velocity());mob.hurtMarked=true;mob.fallDistance=0;
        Vec3 step=mob.position().subtract(before);
        Vec3 from=before.add(0,mob.getBbHeight()*.5,0),to=mob.position().add(0,mob.getBbHeight()*.5,0);
        var hits=mob.level().getEntitiesOfClass(LivingEntity.class,mob.getBoundingBox().expandTowards(step.scale(-1)).inflate(.1),
                e->e!=shot.owner()&&e!=mob&&e.isAlive()&&!e.isSpectator()&&mob.hasLineOfSight(e)
                &&(!(e instanceof net.minecraft.world.entity.player.Player other)||shot.owner().canHarmPlayer(other)));
        var hit=hits.stream().filter(e->e.getBoundingBox().inflate(mob.getBbWidth()*.5).contains(from)
                ||e.getBoundingBox().inflate(mob.getBbWidth()*.5).clip(from,to).isPresent()).findFirst();
        if(hit.isPresent()||mob.horizontalCollision||mob.verticalCollision) {
            THROWN.remove(mob);release(mob);mob.setDeltaMovement(Vec3.ZERO);
            hit.ifPresent(e->e.hurt(mob.damageSources().thrown(mob,shot.owner()),6));
            mob.hurt(mob.damageSources().thrown(mob,shot.owner()),6);
            mob.level().playSound(null,mob.blockPosition(),SoundEvents.GENERIC_BIG_FALL,SoundSource.PLAYERS,.8F,.8F);
        } else THROWN.put(mob,new Thrown(shot.owner(),shot.remaining()-1,shot.velocity().add(0,-.08,0).scale(.98)));
    }
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
        if(visual.stage()==ConsumeVisual.FLIGHT) return;
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
        THROWN.remove(event.getEntity());
        for (var entry : List.copyOf(ACTIVE.entrySet()))
            if (entry.getValue().victim() == event.getEntity() || entry.getKey() == event.getEntity()) cancel(entry.getKey());
    }
    @SubscribeEvent public static void stopping(net.neoforged.neoforge.event.server.ServerStoppingEvent event) {
        for (var player : List.copyOf(ACTIVE.keySet())) cancel(player);
        THROWN.clear();
    }
}
