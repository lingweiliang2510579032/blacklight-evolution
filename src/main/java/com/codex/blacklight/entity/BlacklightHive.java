package com.codex.blacklight.entity;

import com.codex.blacklight.registry.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.damagesource.DamageSource;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

/** A persistent encounter core. Three finite reinforcement waves survive reloads. */
public final class BlacklightHive extends Monster implements GeoEntity {
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent boss=new ServerBossEvent(Component.translatable("entity.blacklight.blacklight_hive"),BossEvent.BossBarColor.RED,BossEvent.BossBarOverlay.NOTCHED_10);
    private int waves,cooldown=80;
    private boolean rewarded;
    public BlacklightHive(EntityType<? extends BlacklightHive> type,Level level){super(type,level);setPersistenceRequired();xpReward=40;}
    public static AttributeSupplier.Builder attributes(){return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,160).add(Attributes.MOVEMENT_SPEED,0).add(Attributes.KNOCKBACK_RESISTANCE,1);}
    @Override protected void registerGoals(){}
    @Override protected boolean shouldDespawnInPeaceful(){return false;}
    @Override public boolean isPushable(){return false;}
    @Override public void knockback(double strength,double x,double z){}
    @Override public void tick(){
        super.tick();if(level().isClientSide)return;
        boss.setProgress(getHealth()/getMaxHealth());
        if(tickCount%20==0){
            var nearby=((net.minecraft.server.level.ServerLevel)level()).getPlayers(p->p.isAlive()&&!p.isSpectator()&&p.distanceToSqr(this)<32*32);
            for(var p:java.util.List.copyOf(boss.getPlayers()))if(!nearby.contains(p))boss.removePlayer(p);
            for(var p:nearby)boss.addPlayer(p);
        }
        if(!isAlive()||boss.getPlayers().isEmpty()||level().getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL)return;
        if(cooldown>0){cooldown--;return;}
        if(waves>=3)return;
        int alive=level().getEntitiesOfClass(Mob.class,getBoundingBox().inflate(24),e->e instanceof BlacklightMutant||e instanceof BlacklightHunter).size();
        if(alive>=6){cooldown=80;return;}
        int spawned=0;
        for(int i=0;i<Math.min(2,6-alive);i++){
            var type=waves==2?ModEntities.BLACKLIGHT_BRUTE: i==0?ModEntities.BLACKLIGHT_STALKER:ModEntities.BLACKLIGHT_SPITTER;
            var mob=type.get().create(level());double a=random.nextDouble()*Math.PI*2;double x=getX()+Math.cos(a)*8,z=getZ()+Math.sin(a)*8;
            var at=net.minecraft.core.BlockPos.containing(x,getY()-1,z);
            if(!level().hasChunkAt(at)||!level().getBlockState(at.below()).isSolid())continue;
            mob.moveTo(x,getY()-1,z,random.nextFloat()*360,0);if(!level().noCollision(mob))continue;
            mob.restrictTo(blockPosition(),20);level().addFreshEntity(mob);spawned++;
        }
        if(spawned>0)waves++;cooldown=240;
    }
    @Override public void die(DamageSource source){
        if(!level().isClientSide&&!rewarded){
            rewarded=true;
            spawnAtLocation(ModItems.BLACKLIGHT_SAMPLE.get(),2);spawnAtLocation(net.minecraft.world.item.Items.EMERALD,3);
            if(source.getEntity() instanceof ServerPlayer player){
                var state=player.getData(ModAttachments.PLAYER_STATE);
                if(state.isInfected()){state.addEvolutionPoints(180);state.addBiomass(250);player.setData(ModAttachments.PLAYER_STATE,state);player.syncData(ModAttachments.PLAYER_STATE);}
                com.codex.blacklight.power.GrowthSystem.award(player,"hive_breaker");
                player.displayClientMessage(Component.translatable("message.blacklight.hive_cleared"),false);
            }
            boss.removeAllPlayers();
        }
        super.die(source);
    }
    @Override public void remove(RemovalReason reason){boss.removeAllPlayers();super.remove(reason);}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putBoolean("BlacklightRewarded",rewarded);tag.putInt("BlacklightWaves",waves);tag.putInt("BlacklightWaveCooldown",cooldown);}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);rewarded=tag.getBoolean("BlacklightRewarded");waves=Math.clamp(tag.getInt("BlacklightWaves"),0,3);cooldown=Math.clamp(tag.getInt("BlacklightWaveCooldown"),0,240);}
    public int waves(){return waves;}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){c.add(new AnimationController<>(this,"core",0,s->s.setAndContinue(RawAnimation.begin().thenLoop("idle"))));}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
