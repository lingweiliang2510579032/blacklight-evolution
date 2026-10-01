package com.codex.blacklight.infection;

import com.codex.blacklight.combat.ConsumeSequence;
import com.codex.blacklight.combat.HitReaction;
import com.codex.blacklight.combat.ImpactControl;
import com.codex.blacklight.entity.*;
import com.codex.blacklight.power.CombatScale;
import com.codex.blacklight.registry.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Finite live prey, an interruptible paired sequence, and credit only for an accepted kill. */
@EventBusSubscriber(modid="blacklight")
public final class DevourerFeedingSystem {
    public static final int FEED_TICKS=80,FEED_INTERVAL=500,MAX_BIOMASS=60;
    private static final String FLAG="BlacklightDevourer",HOME="BlacklightDevourHome";
    private static final String BIOMASS="BlacklightDevourBiomass",MEALS="BlacklightDevourMeals";
    private static final String NEXT="BlacklightDevourNextFeed",CLAIM="BlacklightDevourClaim";
    private static final String RETRY="BlacklightDevourRetryAfter";
    private static final Map<BlacklightInfectedHost,Session> ACTIVE=new IdentityHashMap<>();
    private static final Map<Mob,Session> CAPTURED=new IdentityHashMap<>();
    private static final class Session {
        final BlacklightInfectedHost host;final Mob prey;final long acquired;
        long started=-1;float yaw;Vec3 origin;boolean captured;
        Session(BlacklightInfectedHost host,Mob prey,long now){this.host=host;this.prey=prey;acquired=now;}
    }
    private DevourerFeedingSystem() {}

    public static boolean isDevourer(BlacklightInfectedHost host) {
        return host.speciesId().equals("minecraft:iron_golem")&&host.getPersistentData().getBoolean(FLAG);
    }
    public static int biomass(BlacklightInfectedHost host) { return Mth.clamp(host.getPersistentData().getInt(BIOMASS),0,MAX_BIOMASS); }
    public static int meals(BlacklightInfectedHost host) { return Math.max(0,host.getPersistentData().getInt(MEALS)); }
    public static int availableGrowthTicks(BlacklightInfectedHost host) { return biomass(host)*300; }
    public static BlockPos home(BlacklightInfectedHost host) {
        return host.getPersistentData().contains(HOME,Tag.TAG_LONG)?BlockPos.of(host.getPersistentData().getLong(HOME)):host.blockPosition();
    }
    private static Vec3 center(BlacklightInfectedHost host) { return Vec3.atBottomCenterOf(home(host)); }
    private static double roamRadius(BlacklightInfectedHost host) {
        return Math.max(2,23-InfectedBossProfile.IRON_GOLEM.visualWidth()*host.bodyScale()*.5-2);
    }
    public static boolean withinTerritory(BlacklightInfectedHost host,Entity target) {
        Vec3 d=target.position().subtract(center(host));double radius=roamRadius(host);
        return d.horizontalDistanceSqr()<=radius*radius&&Math.abs(d.y)<=24;
    }
    /** Exclusive MOVE/LOOK ownership; ordinary target AI may still select an intruding player. */
    public static boolean controls(BlacklightInfectedHost host) {
        return isDevourer(host)&&(ACTIVE.containsKey(host)||recovering(host)||host.getTarget()==null||!withinTerritory(host,host));
    }
    private static boolean recovering(BlacklightInfectedHost host) {
        return host.feedingTargetId()<0&&host.feedingStarted()>=0&&host.level().getGameTime()-host.feedingStarted()<96;
    }
    public static void tick(BlacklightInfectedHost host) {
        if(!(host.level() instanceof ServerLevel level))return;
        if(!isDevourer(host)){release(host);return;}
        if(!host.getPersistentData().contains(HOME,Tag.TAG_LONG))host.getPersistentData().putLong(HOME,host.blockPosition().asLong());
        boolean disabled=!host.isAlive()||host.isNoAi()||host.isPassenger()||host.isVehicle()||host.isLeashed()
                ||host.getData(ModAttachments.CONSUME_VISUAL).active()||ImpactControl.stunned(host);
        if(disabled){release(host);return;}
        if(recovering(host)){host.getNavigation().stop();host.setXxa(0);host.setZza(0);return;}
        if(host.feedingTargetId()<0&&host.feedingStarted()>=0)host.setFeedingVisual(-1,-1);
        Session session=ACTIVE.get(host);
        if(!DevourerHabitat.canFeed(host)||host.giantBoss()||biomass(host)>=MAX_BIOMASS||level.getDifficulty()==Difficulty.PEACEFUL) {
            release(host);returnHome(host);return;
        }
        if(session!=null){advance(level,session);return;}
        returnHome(host);
        long now=level.getGameTime();CompoundTag data=host.getPersistentData();
        if(!data.contains(NEXT)){data.putLong(NEXT,now+100);return;}
        if(now<data.getLong(NEXT)||!withinTerritory(host,host)||host.getTarget()!=null
                ||availableGrowthTicks(host)>host.evolutionTicks()+1200)return;
        // Failed searches are bounded to one per second, successful meals to one per 25 seconds.
        data.putLong(NEXT,now+20);
        Vec3 home=center(host);
        Mob prey=level.getEntitiesOfClass(Mob.class,new AABB(home,home).inflate(24),mob->eligible(host,mob,now))
                .stream().filter(host::hasLineOfSight).min(Comparator.comparingDouble(host::distanceToSqr)).orElse(null);
        if(prey==null)return;
        if(prey.getPersistentData().contains(CLAIM))clearClaim(prey);
        CompoundTag claim=new CompoundTag();claim.putUUID("Owner",host.getUUID());claim.putLong("Until",now+200);
        prey.getPersistentData().put(CLAIM,claim);ACTIVE.put(host,new Session(host,prey,now));
        host.setTarget(null);host.getNavigation().stop();
    }
    private static boolean eligible(BlacklightInfectedHost host,Mob prey,long now) {
        if(prey==host||!prey.isAlive()||prey.isRemoved()||prey.level()!=host.level()||prey.isNoAi()||prey.isInvulnerable()
                ||prey.hasCustomName()||prey.isLeashed()||prey.isPassenger()||prey.isVehicle()
                ||prey.getData(ModAttachments.CONSUME_VISUAL).active()||prey.getData(ModAttachments.CONSUMING)
                ||prey.getData(ModAttachments.GRAB_STAGE)!=0||ConsumeSequence.boss(prey)
                ||prey instanceof BlacklightHive||prey instanceof BlacklightHunter||prey instanceof BlacklightMutant
                ||prey instanceof BlacklightInfectedHost infected&&(infected.hasMultipartBody()||infected.hostData().getBoolean("Tamed"))
                ||InfectedBossProfile.forSpecies(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(prey.getType()).toString())!=null
                ||prey instanceof TamableAnimal tame&&tame.isTame()
                ||prey instanceof AbstractHorse horse&&horse.isTamed())return false;
        if(prey.getPersistentData().getLong(RETRY)>now)return false;
        if(prey.position().distanceToSqr(center(host))>24*24)return false;
        CompoundTag claim=prey.getPersistentData().getCompound(CLAIM);
        if(claim.hasUUID("Owner")&&!claim.getUUID("Owner").equals(host.getUUID())&&claim.getLong("Until")>now)return false;
        return true;
    }
    private static void returnHome(BlacklightInfectedHost host) {
        if(!withinTerritory(host,host))host.setTarget(null);
        if(host.getTarget()!=null)return;
        Vec3 destination=center(host);
        if(host.position().subtract(destination).horizontalDistanceSqr()>2.5*2.5) {
            if(host.tickCount%10==0||host.getNavigation().isDone())host.getNavigation().moveTo(destination.x,destination.y,destination.z,.9);
        } else host.getNavigation().stop();
    }
    private static void advance(ServerLevel level,Session session) {
        var host=session.host;var prey=session.prey;long now=level.getGameTime();
        CompoundTag claim=prey.getPersistentData().getCompound(CLAIM);
        if(!eligible(host,prey,now)||!claim.hasUUID("Owner")||!claim.getUUID("Owner").equals(host.getUUID())
                ||!host.hasLineOfSight(prey)||host.position().distanceToSqr(center(host))>32*32
                ||session.started<0&&now-session.acquired>240) { abort(session);return; }
        claim.putLong("Until",now+200);host.setTarget(null);
        if(session.started<0) {
            host.getLookControl().setLookAt(prey,30,30);
            double reach=2.5+10*host.bodyScale();
            if(host.distanceToSqr(prey)>reach*reach) {
                Vec3 offset=prey.position().subtract(center(host));double radius=roamRadius(host);
                Vec3 horizontal=new Vec3(offset.x,0,offset.z);
                if(horizontal.lengthSqr()>radius*radius)horizontal=horizontal.normalize().scale(radius);
                Vec3 destination=center(host).add(horizontal);
                if(host.tickCount%10==0||host.getNavigation().isDone())host.getNavigation().moveTo(destination.x,prey.getY(),destination.z,1.05);
                return;
            }
            session.started=now;session.origin=prey.position();
            session.yaw=(float)(Mth.atan2(prey.getZ()-host.getZ(),prey.getX()-host.getX())*Mth.RAD_TO_DEG)-90;
            host.setFeedingVisual(prey.getId(),now);
            level.playSound(null,host.blockPosition(),SoundEvents.IRON_GOLEM_ATTACK,SoundSource.HOSTILE,.9F,.6F);
        }
        int age=(int)(now-session.started);
        host.getNavigation().stop();host.setXxa(0);host.setZza(0);
        host.setDeltaMovement(host.getDeltaMovement().multiply(0,1,0));
        host.setYRot(session.yaw);host.yBodyRot=session.yaw;host.yHeadRot=session.yaw;
        double tether=4+18*host.bodyScale();
        if(host.distanceToSqr(prey)>tether*tether){abort(session);return;}
        if(age>=18&&!session.captured) {
            session.captured=true;session.origin=prey.position();
            claim.putBoolean("HadGravityFlag",true);claim.putBoolean("NoGravity",prey.isNoGravity());
            prey.setNoGravity(true);ImpactControl.cancel(prey);prey.getNavigation().stop();prey.setTarget(null);
            prey.walkAnimation.setSpeed(0);
            CAPTURED.put(prey,session);recoil(prey,now,host.getYRot());
        }
        if(session.captured) {
            Vec3 desired=capturePosition(session,age);Vec3 movement=desired.subtract(prey.position());
            double limit=.32+host.bodyScale()*.25;
            if(movement.lengthSqr()>limit*limit)movement=movement.normalize().scale(limit);
            Vec3 before=prey.position();prey.move(MoverType.SELF,movement);prey.setDeltaMovement(Vec3.ZERO);prey.hurtMarked=true;prey.fallDistance=0;
            prey.yBodyRot=session.yaw+180;prey.yHeadRot=prey.yBodyRot;prey.setYRot(prey.yBodyRot);
            // Never drag through terrain. A blocked lift releases the still-living victim.
            if(movement.lengthSqr()>.025&&prey.position().distanceToSqr(before)<.0004){abort(session);return;}
            if(age==42||age==61)recoil(prey,now,session.yaw);
            if(age>=48&&age%5==0)level.sendParticles(ParticleTypes.SCULK_SOUL,prey.getX(),prey.getY()+prey.getBbHeight()*.5,prey.getZ(),3,.25,.3,.25,.01);
        }
        if(age>=FEED_TICKS)complete(level,session);
    }
    private static Vec3 capturePosition(Session s,int age) {
        float scale=s.host.bodyScale();float lift=smooth((age-18)/36F),close=smooth((age-52)/24F);
        Vec3 forward=new Vec3(-Math.sin(s.yaw*Mth.DEG_TO_RAD),0,Math.cos(s.yaw*Mth.DEG_TO_RAD));
        double distance=Mth.lerp(close,Math.max(1.1,9*scale),Math.max(.9,5*scale));
        double height=Mth.lerp(close,Math.max(1.1,14*scale),Math.max(1.4,19*scale))-s.prey.getBbHeight()*.35;
        Vec3 grip=s.host.position().add(forward.scale(distance)).add(0,height,0);
        Vec3 target=s.origin.lerp(grip,lift);
        if(age>45)target=target.add(forward.scale(Math.sin((age-45)*1.15)*.055));
        return target;
    }
    private static float smooth(float value) { float t=Mth.clamp(value,0,1);return t*t*(3-2*t); }
    private static void recoil(Mob prey,long now,float yaw) {
        prey.setData(ModAttachments.HIT_REACTION,new HitReaction(now,19,HitReaction.HEAVY,yaw));prey.syncData(ModAttachments.HIT_REACTION);
    }
    private static void complete(ServerLevel level,Session session) {
        var host=session.host;var prey=session.prey;
        // Snapshot before death hooks alter attributes or remove the entity.
        int units=Mth.clamp((int)Math.floor(CombatScale.originalMaximum(prey)/5),1,8);
        float lethal=Math.min(1_000_000,(prey.getMaxHealth()+prey.getAbsorptionAmount()+10)*20/CombatScale.units(prey));
        boolean accepted=prey.isAlive()&&prey.hurt(host.damageSources().mobAttack(host),lethal);
        // DYING is set after LivingDeath vetoes. A refused damage/death event earns nothing.
        boolean killed=accepted&&!prey.isAlive()&&prey.getPose()==Pose.DYING;
        release(host);host.getPersistentData().putLong(NEXT,level.getGameTime()+FEED_INTERVAL);
        if(!killed)return;
        host.setFeedingVisual(-1,session.started);
        CompoundTag data=host.getPersistentData();data.putInt(BIOMASS,Math.min(MAX_BIOMASS,biomass(host)+units));
        data.putInt(MEALS,(int)Math.min(Integer.MAX_VALUE,(long)meals(host)+1));
        // CombatScale multiplies healing events by recipient units; divide once here.
        host.heal(host.getMaxHealth()*.1F/CombatScale.units(host));
        level.playSound(null,host.blockPosition(),SoundEvents.SCULK_CATALYST_BLOOM,SoundSource.HOSTILE,1.2F,.55F);
        level.sendParticles(ParticleTypes.SCULK_SOUL,prey.getX(),prey.getY()+prey.getBbHeight()*.5,prey.getZ(),24,.6,.7,.6,.045);
        level.sendParticles(ParticleTypes.SMOKE,prey.getX(),prey.getY()+prey.getBbHeight()*.5,prey.getZ(),14,.5,.5,.5,.02);
    }
    private static void abort(Session session) {
        session.prey.getPersistentData().putLong(RETRY,session.host.level().getGameTime()+600);
        session.host.getPersistentData().putLong(NEXT,session.host.level().getGameTime()+100);release(session.host);
    }
    public static void release(BlacklightInfectedHost host) {
        Session session=ACTIVE.remove(host);host.setFeedingVisual(-1,-1);
        if(session==null)return;
        CAPTURED.remove(session.prey);host.getNavigation().stop();
        CompoundTag claim=session.prey.getPersistentData().getCompound(CLAIM);
        if(claim.hasUUID("Owner")&&claim.getUUID("Owner").equals(host.getUUID()))clearClaim(session.prey);
    }
    private static void clearClaim(Mob mob) {
        CompoundTag claim=mob.getPersistentData().getCompound(CLAIM);
        if(claim.getBoolean("HadGravityFlag"))mob.setNoGravity(claim.getBoolean("NoGravity"));
        mob.getPersistentData().remove(CLAIM);mob.setDeltaMovement(Vec3.ZERO);mob.hurtMarked=true;
    }
    @SubscribeEvent public static void freeze(EntityTickEvent.Pre event) {
        if(!(event.getEntity() instanceof Mob prey)||prey.level().isClientSide)return;
        Session session=CAPTURED.get(prey);if(session==null)return;
        if(!session.host.isAlive()||!prey.isAlive()||prey.getData(ModAttachments.CONSUME_VISUAL).active()) {abort(session);return;}
        // Keep interpolation endpoints current while AI/gravity are held for the paired motion.
        prey.xo=prey.getX();prey.yo=prey.getY();prey.zo=prey.getZ();
        prey.xOld=prey.getX();prey.yOld=prey.getY();prey.zOld=prey.getZ();
        prey.yBodyRotO=prey.yBodyRot;prey.yHeadRotO=prey.yHeadRot;
        event.setCanceled(true);
    }
    @SubscribeEvent public static void join(EntityJoinLevelEvent event) {
        if(!event.getLevel().isClientSide&&event.getEntity() instanceof Mob mob&&mob.getPersistentData().contains(CLAIM))clearClaim(mob);
    }
    @SubscribeEvent public static void leave(EntityLeaveLevelEvent event) {
        if(event.getLevel().isClientSide)return;
        if(event.getEntity() instanceof BlacklightInfectedHost host)release(host);
        for(Session session:List.copyOf(ACTIVE.values()))if(session.prey==event.getEntity())abort(session);
    }
    @SubscribeEvent public static void stop(ServerStoppingEvent event) {
        for(var host:List.copyOf(ACTIVE.keySet()))release(host);
    }
}
