package com.codex.blacklight.entity;

import com.codex.blacklight.combat.ConsumeSequence;
import com.codex.blacklight.infection.BlacklightInfectionSystem;
import com.codex.blacklight.registry.ModAttachments;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** A separate infected entity with saved host anatomy, variants and locomotion family. */
public final class BlacklightInfectedHost extends Monster {
    private static final EntityDataAccessor<CompoundTag> HOST = SynchedEntityData.defineId(BlacklightInfectedHost.class,EntityDataSerializers.COMPOUND_TAG);
    private Mob appearance;
    private int appearanceAnimationTick = Integer.MIN_VALUE;
    private boolean navigationReady;
    private int attackCooldown;
    public BlacklightInfectedHost(EntityType<? extends BlacklightInfectedHost> type,Level level){super(type,level);xpReward=8;}
    public static AttributeSupplier.Builder attributes(){return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH,30).add(Attributes.ATTACK_DAMAGE,5).add(Attributes.MOVEMENT_SPEED,.30)
            .add(Attributes.FLYING_SPEED,.35).add(Attributes.FOLLOW_RANGE,24).add(Attributes.STEP_HEIGHT,1);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(HOST,new CompoundTag());}
    public CompoundTag hostData(){return entityData.get(HOST);}
    public String family(){return hostData().getString("Family");}
    public boolean humanoid(){return hostData().getBoolean("Humanoid");}
    public boolean quadruped(){return hostData().getBoolean("Quadruped");}
    public boolean creeper(){return hostData().getString("Type").equals("minecraft:creeper");}
    public void inherit(Mob source){
        CompoundTag data=new CompoundTag();
        data.putString("Type",BuiltInRegistries.ENTITY_TYPE.getKey(source.getType()).toString());
        String family=source instanceof net.minecraft.world.entity.animal.WaterAnimal?"water":
                source instanceof net.minecraft.world.entity.FlyingMob||source instanceof net.minecraft.world.entity.animal.FlyingAnimal
                ||source instanceof net.minecraft.world.entity.ambient.Bat||source instanceof net.minecraft.world.entity.monster.Vex?"air":"ground";
        data.putString("Family",family);data.putBoolean("Humanoid",ConsumeSequence.humanoid(source));
        data.putBoolean("Quadruped",ConsumeSequence.quadruped(source));
        data.putFloat("Width",source.getBbWidth()*1.12F);data.putFloat("Height",source.getBbHeight()*1.12F);
        // Only visual variants cross into the new creature. No UUID, passengers,
        // equipment, brain, inventory or entity attachments in the client proxy.
        CompoundTag saved=new CompoundTag(),visual=new CompoundTag();source.saveWithoutId(saved);
        for(String key:new String[]{"Age","IsBaby","Variant","variant","Color","Sheared","CollarColor","CatType",
                "RabbitType","Size","Type","Profession","VillagerData","PuffState","MooshroomType","MainGene","HiddenGene"})
            if(saved.contains(key))visual.put(key,saved.get(key).copy());
        data.put("Visual",visual);entityData.set(HOST,data);refreshDimensions();
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(Math.max(18,source.getMaxHealth()*1.4));
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(Math.clamp(source.getMaxHealth()*.14,4,18));
        setHealth(Math.max(1,getMaxHealth()*source.getHealth()/source.getMaxHealth()));
        setPersistenceRequired();setupNavigation();
    }
    @Override public EntityDimensions getDefaultDimensions(Pose pose){
        var data=hostData();
        if(!data.contains("Width"))return super.getDefaultDimensions(pose);
        return EntityDimensions.scalable(Math.clamp(data.getFloat("Width"),.2F,8F),Math.clamp(data.getFloat("Height"),.2F,8F));
    }
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){
        super.onSyncedDataUpdated(key);
        if(HOST.equals(key)){appearance=null;appearanceAnimationTick=Integer.MIN_VALUE;navigationReady=false;refreshDimensions();}
    }
    private EntityType<?> sourceType(){
        ResourceLocation id=ResourceLocation.tryParse(hostData().getString("Type"));
        if(id==null)return EntityType.PIG;
        EntityType<?> type=BuiltInRegistries.ENTITY_TYPE.get(id);
        return type==null||type==getType()?EntityType.PIG:type;
    }
    /** Detached, unticked render proxy; it never joins the level or becomes a damage target. */
    public Mob appearance(){
        if(appearance==null){
            Entity value=sourceType().create(level());
            if(!(value instanceof Mob))value=EntityType.PIG.create(level());
            appearance=(Mob)value;
            if(appearance!=null){appearance.readAdditionalSaveData(hostData().getCompound("Visual"));appearance.setNoAi(true);}
        }
        return appearance;
    }
    public void advanceAppearanceAnimation(){
        if(appearance==null||appearanceAnimationTick==tickCount)return;
        appearanceAnimationTick=tickCount;
        appearance.walkAnimation.update(walkAnimation.speed(),.4F);
    }
    @Override protected Component getTypeName(){return Component.translatable("entity.blacklight.infected_host_name",sourceType().getDescription());}
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(2,new MeleeAttackGoal(this,1.1,true){
            @Override public boolean canUse(){return family().equals("ground")&&super.canUse();}
            @Override public boolean canContinueToUse(){return !getData(ModAttachments.CONSUME_VISUAL).active()&&super.canContinueToUse();}
        });
        goalSelector.addGoal(6,new WaterAvoidingRandomStrollGoal(this,.8){@Override public boolean canUse(){return family().equals("ground")&&super.canUse();}});
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,LivingEntity.class,10,true,false,
                v->!BlacklightInfectionSystem.isInfected(v)&&!(v instanceof net.minecraft.world.entity.player.Player p&&(p.isCreative()||p.isSpectator()))));
    }
    private void setupNavigation(){
        if(navigationReady)return;navigationReady=true;
        if(family().equals("air")){
            moveControl=new FlyingMoveControl(this,20,true);
            navigation=new FlyingPathNavigation(this,level());setNoGravity(true);
        }else if(family().equals("water"))navigation=new WaterBoundPathNavigation(this,level());
    }
    @Override public void tick(){
        super.tick();if(level().isClientSide)return;setupNavigation();
        if(family().equals("water"))setAirSupply(getMaxAirSupply());
        if(getData(ModAttachments.CONSUME_VISUAL).active()||com.codex.blacklight.combat.ImpactControl.stunned(this)){getNavigation().stop();return;}
        if(attackCooldown>0)attackCooldown--;
        if(family().equals("ground"))return;
        var target=getTarget();if(target==null||!target.isAlive())return;
        if(family().equals("water")&&!isInWater())return;
        getLookControl().setLookAt(target,20,20);
        Vec3 direction=target.getBoundingBox().getCenter().subtract(getBoundingBox().getCenter());
        // Continuous steering respects block collisions, including aquatic and flying hosts.
        if(hasLineOfSight(target))setDeltaMovement(getDeltaMovement().scale(.85).add(direction.normalize().scale(.045)));
        if(direction.lengthSqr()<Math.pow(getBbWidth()+target.getBbWidth()+.6,2)&&hasLineOfSight(target)&&attackCooldown==0){
            swing(net.minecraft.world.InteractionHand.MAIN_HAND);doHurtTarget(target);attackCooldown=24;
        }
    }
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.put("BlacklightHost",hostData().copy());}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);entityData.set(HOST,tag.getCompound("BlacklightHost").copy());refreshDimensions();}
}
