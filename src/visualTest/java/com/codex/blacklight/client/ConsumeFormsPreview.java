package com.codex.blacklight.client;

import com.codex.blacklight.BlacklightNeoMod;
import com.codex.blacklight.combat.ConsumeSystem;
import com.codex.blacklight.power.PowerForm;
import com.codex.blacklight.registry.*;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import java.io.File;

/** Opt-in acceptance on a dedicated QA save, never a user's gameplay world. */
@EventBusSubscriber(modid=BlacklightNeoMod.MOD_ID,value=Dist.CLIENT)
public final class ConsumeFormsPreview {
    private static final PowerForm[] FORMS={PowerForm.NONE,PowerForm.CLAWS,PowerForm.HAMMERFISTS,PowerForm.BLADE,PowerForm.WHIPFIST,PowerForm.BLADE,PowerForm.HAMMERFISTS};
    private static final String[] NAMES={"normal_cow","claws_zombie","hammer_villager","blade_hunter","whip_pig","blade_zombie","hammer_sheep"};
    private static int ticks,scene; private static volatile int cameraId=-1,victimId=-1;
    private static boolean ready,done;private static String pending;
    private static int oldFov;private static boolean oldGui,oldPause,oldGuide;private static CameraType oldCamera;
    private static final com.google.gson.JsonArray results=new com.google.gson.JsonArray();
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        String output=System.getProperty("blacklight.test.consumeForms");if(output==null||done)return;
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null)return;
        var server=mc.getSingleplayerServer();
        if(!server.getWorldPath(LevelResource.ROOT).toString().contains("Blacklight Consume QA r10"))throw new IllegalStateException("Requires isolated R10 save");
        if(++ticks<90)return;
        if(!ready) {
            ready=true;scene=Integer.getInteger("blacklight.test.consumeStart",0)*130;new File(output,"screenshots").mkdirs();
            oldGuide=yesman.epicfight.config.ClientConfig.enableTargetEntityGuide;yesman.epicfight.config.ClientConfig.enableTargetEntityGuide=false;
            oldFov=mc.options.fov().get();oldGui=mc.options.hideGui;oldCamera=mc.options.getCameraType();oldPause=mc.options.pauseOnLostFocus;
            mc.options.fov().set(50);mc.options.hideGui=true;mc.options.pauseOnLostFocus=false;mc.options.setCameraType(CameraType.FIRST_PERSON);
            var uuid=mc.player.getUUID();server.execute(()->{
                var level=server.overworld();var p=server.getPlayerList().getPlayer(uuid);
                level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false,server);
                level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_NATURAL_REGENERATION).set(false,server);
                level.setDayTime(6000);level.setWeatherParameters(6000,0,false,false);
                for(var e:level.getEntities(p,new net.minecraft.world.phys.AABB(10,215,10,34,230,34)))e.discard();
                for(int x=12;x<33;x++)for(int z=12;z<33;z++)for(int y=219;y<227;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),(y==219?Blocks.SMOOTH_QUARTZ:y==226?Blocks.SEA_LANTERN:Blocks.AIR).defaultBlockState());
                p.getInventory().clearContent();p.setGameMode(GameType.SURVIVAL);p.teleportTo(20,220,22);
                var camera=EntityType.ARMOR_STAND.create(level);camera.moveTo(27,220.1,21,90,6);camera.setInvisible(true);camera.setNoGravity(true);camera.setInvulnerable(true);level.addFreshEntity(camera);cameraId=camera.getId();
            });return;
        }
        var camera=mc.level.getEntity(cameraId);if(camera==null)return;mc.setCameraEntity(camera);
        camera.setYRot(90);camera.yRotO=90;camera.setXRot(6);camera.xRotO=6;camera.setYHeadRot(90);if(camera instanceof LivingEntity e)e.yHeadRotO=90;
        if(mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen)mc.setScreen(null);
        mc.player.setYRot(180);mc.player.yBodyRot=180;mc.player.yHeadRot=180;mc.player.setXRot(0);
        int index=scene/130,phase=scene%130;scene++;
        if(index>=Integer.getInteger("blacklight.test.consumeCases",FORMS.length)) {
            verifySharedModel(mc, output);
            done=true;yesman.epicfight.config.ClientConfig.enableTargetEntityGuide=oldGuide;
            mc.setCameraEntity(mc.player);mc.options.fov().set(oldFov);mc.options.hideGui=oldGui;mc.options.setCameraType(oldCamera);mc.options.pauseOnLostFocus=oldPause;mc.stop();return;
        }
        var uuid=mc.player.getUUID();
        if(phase==0)server.execute(()->{
            var p=server.getPlayerList().getPlayer(uuid);ConsumeSystem.cancel(p);
            var prior=server.overworld().getEntity(victimId);if(prior!=null)prior.discard();
            server.overworld().getEntity(cameraId).setPos(27,220.1,index==4?19.8:21);
            p.teleportTo(20,220,22);p.setYRot(180);p.setXRot(0);p.setOnGround(true);p.setHealth(10);
            var state=p.getData(ModAttachments.PLAYER_STATE);state.cure();state.infect();state.setPrototypeMode(true);state.unlock(FORMS[index]);state.activate(FORMS[index]);
            p.setData(ModAttachments.PLAYER_STATE,state);p.syncData(ModAttachments.PLAYER_STATE);
            p.getData(ModAttachments.CLAW_COMBAT).reset();p.getData(ModAttachments.MOVEMENT_STATE).reset();
            EntityType<? extends Mob> type=switch(index){case 0->EntityType.COW;case 1,5->EntityType.ZOMBIE;case 2->EntityType.VILLAGER;case 3->ModEntities.BLACKLIGHT_HUNTER.get();case 6->EntityType.SHEEP;default->EntityType.PIG;};
            var mob=type.create(p.level());mob.moveTo(20,220,index==4?17:20,0,0);mob.setNoAi(true);mob.setPersistenceRequired();
            if(index==1||index==5)mob.setHealth(4);if(index==3)mob.setHealth(12);
            p.serverLevel().addFreshEntity(mob);victimId=mob.getId();
        });
        if(phase==30)server.execute(()->{var p=server.getPlayerList().getPlayer(uuid);p.setYRot(180);p.setXRot(0);p.setOnGround(true);p.getData(ModAttachments.CLAW_COMBAT).reset();ConsumeSystem.request(p);
            BlacklightNeoMod.LOGGER.info("R10 {} capture started={}",NAMES[index],p.getData(ModAttachments.CONSUMING));});
        if(phase>=20&&phase<=101)pending=String.format("%s_%03d.png",NAMES[index],phase-20);
        if(phase==108)server.execute(()->{
            var p=server.getPlayerList().getPlayer(uuid);var data=new com.google.gson.JsonObject();data.addProperty("case",NAMES[index]);
            data.addProperty("consumed",server.overworld().getEntity(victimId)==null);data.addProperty("health",p.getHealth());data.addProperty("biomass",p.getData(ModAttachments.PLAYER_STATE).getBiomass());data.addProperty("ep",p.getData(ModAttachments.PLAYER_STATE).getEvolutionPoints());
            data.addProperty("unlocked",!p.getData(ModAttachments.CONSUMING));results.add(data);
            try{java.nio.file.Files.writeString(java.nio.file.Path.of(output,"runtime-check.json"),results.toString());}catch(Exception e){throw new IllegalStateException(e);}
        });
    }
    private static void verifySharedModel(Minecraft mc,String output) {
        var cow=EntityType.COW.create(mc.level);
        var renderer=(net.minecraft.client.renderer.entity.LivingEntityRenderer<?, ?>)mc.getEntityRenderDispatcher().getRenderer(cow);
        var model=renderer.getModel();var access=(com.codex.blacklight.mixin.client.AgeableModelAccessor)model;
        var parts=new java.util.ArrayList<net.minecraft.client.model.geom.ModelPart>();
        access.blacklight$heads().forEach(parts::add);access.blacklight$body().forEach(parts::add);
        var before=parts.stream().map(part->new float[]{part.xRot,part.yRot,part.zRot}).toList();
        cow.setData(ModAttachments.CONSUME_VISUAL,new com.codex.blacklight.combat.ConsumeVisual(mc.player.getId(),mc.level.getGameTime()-19,56,PowerForm.HAMMERFISTS.ordinal()));
        com.codex.blacklight.client.animation.ConsumeVictimPose.vanilla(model,cow,0);
        boolean changed=false;for(int i=0;i<parts.size();i++){var part=parts.get(i);changed|=!java.util.Arrays.equals(before.get(i),new float[]{part.xRot,part.yRot,part.zRot});}
        com.codex.blacklight.client.animation.ConsumeVictimPose.restore(model);
        boolean restored=true;for(int i=0;i<parts.size();i++){var part=parts.get(i);restored&=java.util.Arrays.equals(before.get(i),new float[]{part.xRot,part.yRot,part.zRot});}
        if(!changed||!restored)throw new IllegalStateException("Victim pose failed to animate or restore");
        try{java.nio.file.Files.writeString(java.nio.file.Path.of(output,"pose-restore-check.json"),"{\"jointPoseChanged\":true,\"sharedModelRestored\":true}");}catch(Exception e){throw new IllegalStateException(e);}
    }
    @SubscribeEvent public static void frame(RenderFrameEvent.Post event) {
        if(pending==null||done)return;String name=pending;pending=null;var mc=Minecraft.getInstance();
        if(name.startsWith("whip_pig") && mc.player!=null) {
            var hand=com.codex.blacklight.client.renderer.WhipTetherRenderer.capturedHand(mc.player.getId());
            if(hand==null||hand.distanceToSqr(mc.player.position())>9)throw new IllegalStateException("Whip detached from rendered arm: "+hand);
        }
        Screenshot.grab(new File(System.getProperty("blacklight.test.consumeForms")),name,mc.getMainRenderTarget(),m->{});
    }
}
