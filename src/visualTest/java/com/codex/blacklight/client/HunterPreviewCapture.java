package com.codex.blacklight.client;
import com.codex.blacklight.BlacklightNeoMod;
import com.codex.blacklight.combat.ConsumeSystem;
import com.codex.blacklight.entity.BlacklightHunter;
import com.codex.blacklight.infection.BlacklightInfectionSystem;
import com.codex.blacklight.registry.ModAttachments;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import java.io.File;

/** Isolated R9 visual acceptance: conversion and full consume sequence. */
@EventBusSubscriber(modid=BlacklightNeoMod.MOD_ID,value=Dist.CLIENT)
public final class HunterPreviewCapture {
    private static int ticks,scene; private static volatile int zombieId=-1,cameraId=-1,hunterId=-1;
    private static boolean ready,done;private static String pending;
    private static int oldFov;private static boolean oldGui,oldPause,oldTargetGuide;private static CameraType oldCamera;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        String output=System.getProperty("blacklight.test.hunterCapture");if(output==null||done)return;
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null)return;
        var server=mc.getSingleplayerServer();
        if(!server.getWorldPath(LevelResource.ROOT).toString().contains("Blacklight Hunter QA r9"))throw new IllegalStateException("Requires isolated R9 save");
        if(++ticks<90)return;
        if(!ready) {
            ready=true;new File(output,"screenshots").mkdirs();
            oldTargetGuide=yesman.epicfight.config.ClientConfig.enableTargetEntityGuide;yesman.epicfight.config.ClientConfig.enableTargetEntityGuide=false;
            oldFov=mc.options.fov().get();oldGui=mc.options.hideGui;oldCamera=mc.options.getCameraType();oldPause=mc.options.pauseOnLostFocus;
            mc.options.fov().set(48);mc.options.hideGui=true;mc.options.pauseOnLostFocus=false;mc.options.setCameraType(CameraType.FIRST_PERSON);
            var uuid=mc.player.getUUID();server.execute(()->{
                var level=server.overworld();var p=server.getPlayerList().getPlayer(uuid);
                level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false,server);
                level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_NATURAL_REGENERATION).set(false,server);
                level.setDayTime(6000);level.setWeatherParameters(6000,0,false,false);
                for(var e:level.getEntities(p,new net.minecraft.world.phys.AABB(12,215,12,30,230,30)))e.discard();
                for(int x=13;x<29;x++)for(int z=13;z<30;z++)for(int y=219;y<227;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),(y==219?Blocks.SMOOTH_QUARTZ:y==226?Blocks.SEA_LANTERN:Blocks.AIR).defaultBlockState());
                p.getInventory().clearContent();p.setGameMode(GameType.SURVIVAL);p.teleportTo(20,220,22);p.setYRot(180);p.setXRot(0);
                var state=p.getData(ModAttachments.PLAYER_STATE);state.cure();state.infect();state.setPrototypeMode(true);p.setData(ModAttachments.PLAYER_STATE,state);p.syncData(ModAttachments.PLAYER_STATE);p.setHealth(10);
                var z=EntityType.ZOMBIE.create(level);z.moveTo(20,220,20,0,0);z.setNoAi(true);z.setInvulnerable(true);z.setPersistenceRequired();z.addTag("hunter_r9_qa");level.addFreshEntity(z);zombieId=z.getId();
                var camera=EntityType.ARMOR_STAND.create(level);camera.moveTo(23.5,220.1,24,139,7);camera.setInvisible(true);camera.setNoGravity(true);camera.setInvulnerable(true);level.addFreshEntity(camera);cameraId=camera.getId();
            });return;
        }
        var camera=mc.level.getEntity(cameraId);if(camera==null)return;mc.setCameraEntity(camera);camera.setYRot(139);camera.yRotO=139;camera.setXRot(7);camera.xRotO=7;camera.setYHeadRot(139);if(camera instanceof net.minecraft.world.entity.LivingEntity living) living.yHeadRotO=139;
        if(mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen)mc.setScreen(null);
        mc.player.setYRot(180);mc.player.yBodyRot=180;mc.player.yHeadRot=180;mc.player.setXRot(0);
        scene++;
        if(scene==30)server.execute(()->{var z=server.overworld().getEntity(zombieId);if(z instanceof net.minecraft.world.entity.LivingEntity e)BlacklightInfectionSystem.infect(e,null);});
        if(scene>=25&&scene<=100)pending=String.format("infection_%03d.png",scene-25);
        if(scene==110)server.execute(()->{for(var e:server.overworld().getEntitiesOfClass(BlacklightHunter.class,new net.minecraft.world.phys.AABB(17,218,17,24,224,24)))if(e.getTags().contains("hunter_r9_qa")){hunterId=e.getId();e.setInvulnerable(false);e.setHealth(12);}});
        if(scene==130)pending="hunter.png";
        if(scene==150)server.execute(()->{var p=server.getPlayerList().getPlayer(mc.player.getUUID());p.teleportTo(20,220,22);p.setYRot(180);p.setXRot(0);p.setOnGround(true);p.getData(ModAttachments.CLAW_COMBAT).reset();p.getData(ModAttachments.MOVEMENT_STATE).reset();ConsumeSystem.request(p);
            BlacklightNeoMod.LOGGER.info("R9 consume requested lock={} hunter={} pos={} yaw={} health={}",p.getData(ModAttachments.CONSUMING),hunterId,p.position(),p.getYRot(),p.getHealth());});
        if(scene>=145&&scene<=205)pending=String.format("consume_%03d.png",scene-145);
        if(scene==215)server.execute(()->{
            var p=server.getPlayerList().getPlayer(mc.player.getUUID());var data=new com.google.gson.JsonObject();
            data.addProperty("convertedEntityId",hunterId);data.addProperty("originalZombieRemoved",server.overworld().getEntity(zombieId)==null);
            data.addProperty("consumed",server.overworld().getEntity(hunterId)==null);data.addProperty("health",p.getHealth());
            data.addProperty("biomass",p.getData(ModAttachments.PLAYER_STATE).getBiomass());data.addProperty("ep",p.getData(ModAttachments.PLAYER_STATE).getEvolutionPoints());
            var interrupted=com.codex.blacklight.registry.ModEntities.BLACKLIGHT_HUNTER.get().create(p.level());
            interrupted.moveTo(p.getX(),p.getY(),p.getZ()-1.5,0,0);interrupted.setHealth(10);interrupted.setNoAi(true);p.serverLevel().addFreshEntity(interrupted);
            p.setYRot(180);p.setXRot(0);p.setOnGround(true);p.getData(ModAttachments.CLAW_COMBAT).reset();ConsumeSystem.request(p);
            boolean claimed=p.getData(ModAttachments.CONSUMING);p.hurt(p.damageSources().generic(),1);
            data.addProperty("playerDamageCancels",claimed&&!p.getData(ModAttachments.CONSUMING)&&!interrupted.isBeingConsumed());interrupted.discard();
            try{java.nio.file.Files.writeString(java.nio.file.Path.of(output,"runtime-check.json"),data.toString());}catch(Exception e){throw new IllegalStateException(e);}
        });
        if(scene>235){yesman.epicfight.config.ClientConfig.enableTargetEntityGuide=oldTargetGuide;done=true;mc.setCameraEntity(mc.player);mc.options.fov().set(oldFov);mc.options.hideGui=oldGui;mc.options.setCameraType(oldCamera);mc.options.pauseOnLostFocus=oldPause;mc.stop();}
    }
    @SubscribeEvent public static void frame(RenderFrameEvent.Post event) {
        if(pending==null||done)return;String name=pending;pending=null;var mc=Minecraft.getInstance();
        Screenshot.grab(new File(System.getProperty("blacklight.test.hunterCapture")),name,mc.getMainRenderTarget(),m->{});
    }
}
