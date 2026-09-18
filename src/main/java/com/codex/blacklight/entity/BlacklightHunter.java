package com.codex.blacklight.entity;

import com.codex.blacklight.infection.BlacklightInfectionSystem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.EnumSet;

/** A separate species, with an original full-body rig and server-timed heavy swipe. */
public final class BlacklightHunter extends Monster implements GeoEntity {
    private static final EntityDataAccessor<Integer> ATTACK = SynchedEntityData.defineId(BlacklightHunter.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CONSUMER = SynchedEntityData.defineId(BlacklightHunter.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CONSUME_TICKS = SynchedEntityData.defineId(BlacklightHunter.class, EntityDataSerializers.INT);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private boolean previousNoAi;
    private int attackCooldown;
    private LivingEntity swipeTarget;

    public BlacklightHunter(EntityType<? extends BlacklightHunter> type, Level level) { super(type, level); xpReward = 12; }
    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 48).add(Attributes.MOVEMENT_SPEED, .29)
                .add(Attributes.ATTACK_DAMAGE, 7).add(Attributes.FOLLOW_RANGE, 24)
                .add(Attributes.KNOCKBACK_RESISTANCE, .55).add(Attributes.STEP_HEIGHT, 1);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b); b.define(ATTACK, 0); b.define(CONSUMER, -1); b.define(CONSUME_TICKS, 0);
    }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new SwipeGoal());
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, .7));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 20, true, false,
                e -> (e instanceof net.minecraft.world.entity.animal.Animal || e instanceof net.minecraft.world.entity.npc.AbstractVillager)
                        && !BlacklightInfectionSystem.isInfected(e)));
    }
    @Override public void tick() {
        super.tick();
        if (level().isClientSide || isBeingConsumed()) return;
        if (attackCooldown > 0) attackCooldown--;
        int attack = entityData.get(ATTACK);
        if (attack > 0) {
            getNavigation().stop();
            if (attack == 15 && swipeTarget != null && swipeTarget.isAlive()
                    && distanceToSqr(swipeTarget) < 6.25 && hasLineOfSight(swipeTarget)) {
                doHurtTarget(swipeTarget);
            }
            entityData.set(ATTACK, attack - 1);
            if (attack == 1) swipeTarget = null;
        }
    }
    public boolean isBeingConsumed() { return entityData.get(CONSUMER) >= 0; }
    public int consumerId() { return entityData.get(CONSUMER); }
    public int consumeTicks() { return entityData.get(CONSUME_TICKS); }
    public void setConsumeTicks(int ticks) { entityData.set(CONSUME_TICKS, ticks); }
    public boolean claim(Player player) {
        if (isBeingConsumed() || !isAlive()) return false;
        previousNoAi = isNoAi(); setNoAi(true); getNavigation().stop(); setTarget(null);
        entityData.set(ATTACK, 0); swipeTarget = null;
        entityData.set(CONSUMER, player.getId()); entityData.set(CONSUME_TICKS, 0); return true;
    }
    public void release() {
        if (!isBeingConsumed()) return;
        setNoAi(previousNoAi); entityData.set(CONSUMER, -1); entityData.set(CONSUME_TICKS, 0);
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        // The transient grab must never persist as a permanently disabled AI after restart.
        if (isBeingConsumed()) tag.putBoolean("NoAI", previousNoAi);
    }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.RAVAGER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.RAVAGER_DEATH; }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        c.add(new AnimationController<>(this, "body", 3, s -> s.setAndContinue(RawAnimation.begin().thenLoop(
                isBeingConsumed() ? "consume" : entityData.get(ATTACK) > 0 ? "swipe" : s.isMoving() ? "run" : "idle"))));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    private final class SwipeGoal extends Goal {
        SwipeGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        @Override public boolean canUse() { return !isBeingConsumed() && getTarget() != null && getTarget().isAlive(); }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void tick() {
            LivingEntity target = getTarget(); if (target == null) return;
            getLookControl().setLookAt(target, 30, 30);
            if (entityData.get(ATTACK) > 0) return;
            getNavigation().moveTo(target, 1.15);
            if (attackCooldown == 0 && distanceToSqr(target) < 5.3 && hasLineOfSight(target)) {
                swipeTarget = target; entityData.set(ATTACK, 24); attackCooldown = 36;
                playSound(SoundEvents.RAVAGER_ATTACK, .7F, 1.3F);
            }
        }
        @Override public void stop() { getNavigation().stop(); }
    }
}
