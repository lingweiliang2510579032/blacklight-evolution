package com.codex.blacklight.disguise;

import com.codex.blacklight.registry.ModAttachments;
import com.codex.blacklight.power.PowerForm;
import com.codex.blacklight.combat.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.*;
import net.minecraft.core.particles.DustParticleOptions;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/** Latest consumed appearance, explicit switching, and bounded faction camouflage. */
public final class DisguiseSystem {
    private DisguiseSystem(){}
    public static boolean active(Player p){
        var s=p.getData(ModAttachments.PLAYER_STATE);
        return p.isAlive()&&s.isInfected()&&s.isPrototypeMode()&&s.isDisguised()
                &&p.getData(ModAttachments.DISGUISE_IDENTITY).valid();
    }
    public static boolean morphing(Player p){return p.getData(ModAttachments.DISGUISE_MORPH).active(p.level().getGameTime());}
    public static void captured(ServerPlayer p,DisguiseIdentity identity){
        if(!identity.valid())return;
        p.setData(ModAttachments.DISGUISE_IDENTITY,identity);p.syncData(ModAttachments.DISGUISE_IDENTITY);
        var state=p.getData(ModAttachments.PLAYER_STATE);state.captureIdentity(identity.type());
        p.setData(ModAttachments.PLAYER_STATE,state);p.syncData(ModAttachments.PLAYER_STATE);
        p.displayClientMessage(Component.translatable("message.blacklight.identity_captured",identity.entityType().getDescription()),true);
    }
    public static void request(ServerPlayer p){
        var s=p.getData(ModAttachments.PLAYER_STATE);
        if(!p.isAlive()||p.isSpectator()||!s.isInfected()||!s.isPrototypeMode())return;
        var identity=p.getData(ModAttachments.DISGUISE_IDENTITY);
        if(!identity.valid()){message(p,"disguise_missing");return;}
        if(morphing(p)||p.getData(ModAttachments.CONSUMING)||WeaponCombatSystem.busy(p)
                ||p.getData(ModAttachments.CLAW_COMBAT).action()!=ClawCombatState.Action.IDLE
                ||p.getData(ModAttachments.MOVEMENT_STATE).wallTraversal().attached()||p.isPassenger()){
            message(p,"disguise_busy");return;
        }
        boolean entering=!s.isDisguised();
        if(entering&&(s.getAlert()>25||watched(p))){message(p,"disguise_watched");return;}
        // A concealed identity cannot retain visible biological weapon/armor overlays.
        if(entering){
            s.activate(PowerForm.NONE);
            p.setData(ModAttachments.UTILITY_POWERS,0);p.syncData(ModAttachments.UTILITY_POWERS);
            p.setData(ModAttachments.POWER_MORPH,com.codex.blacklight.power.PowerMorph.NONE);p.syncData(ModAttachments.POWER_MORPH);
        }
        p.setData(ModAttachments.CONSUME_RECOVERY,ConsumeVisual.EMPTY);p.syncData(ModAttachments.CONSUME_RECOVERY);
        p.setData(ModAttachments.PLAYER_STATE,s);p.syncData(ModAttachments.PLAYER_STATE);
        p.setData(ModAttachments.DISGUISE_MORPH,new DisguiseMorph(p.level().getGameTime(),entering));p.syncData(ModAttachments.DISGUISE_MORPH);
        p.level().playSound(null,p.blockPosition(),SoundEvents.SCULK_CATALYST_BLOOM,SoundSource.PLAYERS,.65F,entering?.8F:1.15F);
    }
    public static void reveal(ServerPlayer p){
        var s=p.getData(ModAttachments.PLAYER_STATE);
        boolean visible=s.isDisguised()||morphing(p);
        if(!visible)return;
        s.setDisguised(false);s.raiseAlert(60);
        p.setData(ModAttachments.PLAYER_STATE,s);p.syncData(ModAttachments.PLAYER_STATE);
        p.setData(ModAttachments.DISGUISE_MORPH,DisguiseMorph.EMPTY);p.syncData(ModAttachments.DISGUISE_MORPH);
        message(p,"disguise_revealed");
    }
    private static boolean watched(ServerPlayer p){
        return p.serverLevel().getEntitiesOfClass(Mob.class,p.getBoundingBox().inflate(24),
                m->m.isAlive()&&m.getTarget()==p&&m.hasLineOfSight(p)).size()>0;
    }
    /** Same-faction undead/illagers are fooled; infected and unrelated hostile factions are not. */
    public static boolean fooled(Mob mob,Player p){
        if(!active(p)||p.getData(ModAttachments.PLAYER_STATE).getAlert()>25
                ||!net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).getNamespace().equals("minecraft"))return false;
        var id=p.getData(ModAttachments.DISGUISE_IDENTITY);
        return switch(id.family()){
            case "undead"->mob instanceof Zombie||mob instanceof AbstractSkeleton;
            case "illager"->mob instanceof AbstractIllager;
            case "animal"->mob instanceof Monster&&!(mob instanceof Guardian)&&!(mob instanceof Endermite)
                    &&!(mob instanceof Silverfish)&&!(mob instanceof PiglinBrute);
            default->mob.getType()==id.entityType();
        };
    }
    @SubscribeEvent public static void target(LivingChangeTargetEvent e){
        if(e.getEntity() instanceof Mob mob&&e.getNewAboutToBeSetTarget() instanceof Player p&&fooled(mob,p))
            e.setNewAboutToBeSetTarget(null);
    }
    @SubscribeEvent public static void damage(LivingIncomingDamageEvent e){
        if(e.isCanceled()||e.getAmount()<=0)return;
        if(e.getSource().getEntity() instanceof ServerPlayer p&&e.getEntity()!=p)reveal(p);
    }
    @SubscribeEvent public static void attack(AttackEntityEvent e){
        if(!e.isCanceled()&&e.getEntity() instanceof ServerPlayer p)reveal(p);
    }
    @SubscribeEvent public static void projectile(EntityJoinLevelEvent e){
        if(!e.getLevel().isClientSide()&&e.getEntity() instanceof net.minecraft.world.entity.projectile.Projectile shot
                &&shot.getOwner() instanceof ServerPlayer p)reveal(p);
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post e){
        if(!(e.getEntity() instanceof ServerPlayer p))return;
        var s=p.getData(ModAttachments.PLAYER_STATE);var m=p.getData(ModAttachments.DISGUISE_MORPH);
        long now=p.level().getGameTime();
        if(!p.isAlive()||!s.isInfected()||!s.isPrototypeMode()){
            if(s.isDisguised()){s.setDisguised(false);p.setData(ModAttachments.PLAYER_STATE,s);p.syncData(ModAttachments.PLAYER_STATE);}
            if(m.started()>=0){p.setData(ModAttachments.DISGUISE_MORPH,DisguiseMorph.EMPTY);p.syncData(ModAttachments.DISGUISE_MORPH);}
            if(!s.isInfected()&&p.getData(ModAttachments.DISGUISE_IDENTITY).valid()){
                p.setData(ModAttachments.DISGUISE_IDENTITY,DisguiseIdentity.EMPTY);p.syncData(ModAttachments.DISGUISE_IDENTITY);
            }
            return;
        }
        if(m.started()>=0){
            if(s.getActivePower()!=PowerForm.NONE||p.getData(ModAttachments.UTILITY_POWERS)!=0){reveal(p);return;}
            if(m.entering()&&now-m.started()<DisguiseMorph.CONTACT&&watched(p)){reveal(p);return;}
            if(now-m.started()>=DisguiseMorph.CONTACT&&s.isDisguised()!=m.entering()){
                s.setDisguised(m.entering());p.setData(ModAttachments.PLAYER_STATE,s);p.syncData(ModAttachments.PLAYER_STATE);
                message(p,m.entering()?"disguise_enabled":"disguise_disabled");
            }
            if(!m.active(now)){p.setData(ModAttachments.DISGUISE_MORPH,DisguiseMorph.EMPTY);p.syncData(ModAttachments.DISGUISE_MORPH);}
            else if(p.tickCount%2==0)p.serverLevel().sendParticles(new DustParticleOptions(new org.joml.Vector3f(.24F,.025F,.035F),.65F),
                    p.getX(),p.getY()+p.getBbHeight()*.55,p.getZ(),5,.25,.55,.25,0);
        }
        if(p.tickCount%10!=0)return;
        if(s.getAlert()>0&&!watched(p)){s.raiseAlert(-5);p.setData(ModAttachments.PLAYER_STATE,s);p.syncData(ModAttachments.PLAYER_STATE);}
        if(active(p))for(var mob:p.serverLevel().getEntitiesOfClass(Mob.class,p.getBoundingBox().inflate(24),v->v.getTarget()==p&&fooled(v,p))){
            mob.setTarget(null);if(mob.getLastHurtByMob()==p)mob.setLastHurtByMob(null);mob.getNavigation().stop();
        }
    }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent e){
        if(e.getEntity() instanceof ServerPlayer p){
            var s=p.getData(ModAttachments.PLAYER_STATE);s.setDisguised(false);s.raiseAlert(-100);
            p.setData(ModAttachments.PLAYER_STATE,s);p.syncData(ModAttachments.PLAYER_STATE);
            p.syncData(ModAttachments.DISGUISE_IDENTITY);
        }
    }
    private static void message(ServerPlayer p,String key){p.displayClientMessage(Component.translatable("message.blacklight."+key),true);}
}
