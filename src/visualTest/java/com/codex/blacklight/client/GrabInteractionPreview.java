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
public final class GrabInteractionPreview {
    private static final PowerForm[] FORMS={PowerForm.NONE,PowerForm.CLAWS,PowerForm.HAMMERFISTS,PowerForm.BLADE,PowerForm.WHIPFIST,Boolean.getBoolean("blacklight.test.bladeThrow")?PowerForm.BLADE:PowerForm.CLAWS,PowerForm.WHIPFIST};
    private static final String[] NAMES={"normal_cow","claws_zombie","hammer_villager","blade_hunter","whip_pig",Boolean.getBoolean("blacklight.test.bladeThrow")?"blade_throw":"claws_throw","whip_throw"};
    private static double bladeGap; private static int bladeSamples;
    private static int ticks,scene; private static volatile int cameraId=-1,victimId=-1;
    private static boolean ready,done;private static String pending;
    private static int oldFov;private static boolean oldGui,oldPause,oldGuide;private static CameraType oldCamera;
    private static final com.google.gson.JsonArray results=new com.google.gson.JsonArray();
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        String output=System.getProperty("blacklight.test.grabPreview");if(output==null||done)return;
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null)return;
        var server=mc.getSingleplayerServer();
        if(!server.getWorldPath(LevelResource.ROOT).toString().contains("Blacklight Consume QA r10"))throw new IllegalStateException("Requires isolated R10 save");
        if(++ticks<90)return;
        if(!ready) {
            ready=true;scene=Integer.getInteger("blacklight.test.grabStart",0)*185;new File(output,"screenshots").mkdirs();
            oldGuide=yesman.epicfight.config.ClientConfig.enableTargetEntityGuide;yesman.epicfight.config.ClientConfig.enableTargetEntityGuide=false;
            oldFov=mc.options.fov().get();oldGui=mc.options.hideGui;oldCamera=mc.options.getCameraType();oldPause=mc.options.pauseOnLostFocus;
            mc.options.fov().set(50);mc.options.hideGui=true;mc.options.pauseOnLostFocus=false;mc.options.setCameraType(CameraType.FIRST_PERSON);
            var uuid=mc.player.getUUID();server.execute(()->{
                var level=server.overworld();var p=server.getPlayerList().getPlayer(uuid);
                level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false,server);
                level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_NATURAL_REGENERATION).set(false,server);
                level.setDayTime(6000);level.setWeatherParameters(6000,0,false,false);
                for(var e:level.getEntities(p,new net.minecraft.world.phys.AABB(10,215,10,34,230,34)))e.discard();
                for(int x=12;x<37;x++)for(int z=2;z<33;z++)for(int y=219;y<227;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),(y==219?Blocks.SMOOTH_QUARTZ:y==226?Blocks.SEA_LANTERN:Blocks.AIR).defaultBlockState());
                p.getInventory().clearContent();p.setGameMode(GameType.SURVIVAL);p.teleportTo(20,220,22);
                var camera=EntityType.ARMOR_STAND.create(level);camera.moveTo(27,220.1,21,90,6);camera.setInvisible(true);camera.setNoGravity(true);camera.setInvulnerable(true);level.addFreshEntity(camera);cameraId=camera.getId();
            });return;
        }
        var camera=mc.level.getEntity(cameraId);if(camera==null)return;mc.setCameraEntity(camera);
        camera.setYRot(90);camera.yRotO=90;camera.setXRot(6);camera.xRotO=6;camera.setYHeadRot(90);if(camera instanceof LivingEntity e)e.yHeadRotO=90;
        if(mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen)mc.setScreen(null);
        mc.player.setYRot(180);mc.player.yBodyRot=180;mc.player.yHeadRot=180;mc.player.setXRot(0);
        int index=scene/185,phase=scene%185;scene++;
        if(index==3 && Boolean.getBoolean("blacklight.test.bladeStudy")) {
            // Compare the actual attached mesh from three idle camera positions.
            float studyYaw=90;
            double studyX=26,studyZ=22;
            if(phase>=23 && phase<26){studyX=24;studyZ=18;studyYaw=45;}
            if(phase>=26 && phase<30){studyX=16;studyZ=26;studyYaw=225;}
            camera.setPos(studyX,220.1,studyZ);
            camera.xo=studyX;camera.yo=220.1;camera.zo=studyZ;
            camera.setYRot(studyYaw);camera.yRotO=studyYaw;
            camera.setYHeadRot(studyYaw);
            if(camera instanceof LivingEntity e)e.yHeadRotO=studyYaw;
        }
        if(index>=Integer.getInteger("blacklight.test.grabCases",FORMS.length)) {
            verifySharedModel(mc, output);
            if(Boolean.getBoolean("blacklight.test.bladeStudy")) {
                try { java.nio.file.Files.writeString(java.nio.file.Path.of(output,"blade-contact-check.json"),"{\"samples\":"+bladeSamples+",\"maxGap\":"+bladeGap+"}"); } catch(Exception e){throw new IllegalStateException(e);}
                if(bladeSamples<30||bladeGap>.18)throw new IllegalStateException("Blade contact gap "+bladeGap+" samples "+bladeSamples);
            }
            done=true;yesman.epicfight.config.ClientConfig.enableTargetEntityGuide=oldGuide;
            mc.setCameraEntity(mc.player);mc.options.fov().set(oldFov);mc.options.hideGui=oldGui;mc.options.setCameraType(oldCamera);mc.options.pauseOnLostFocus=oldPause;mc.stop();return;
        }
        var uuid=mc.player.getUUID();
        if(phase==0)server.execute(()->{
            var p=server.getPlayerList().getPlayer(uuid);ConsumeSystem.cancel(p);
            var prior=server.overworld().getEntity(victimId);if(prior!=null)prior.discard();
            server.overworld().getEntity(cameraId).setPos(index==4||index==6?38:index>=5?34:27,220.1,index==4||index==6?16:index>=5?17:21);
            p.teleportTo(20,220,22);p.setYRot(180);p.setXRot(0);p.setOnGround(true);p.setHealth(10);
            var state=p.getData(ModAttachments.PLAYER_STATE);state.cure();state.infect();state.setPrototypeMode(true);state.unlock(FORMS[index]);state.activate(FORMS[index]);
            p.setData(ModAttachments.PLAYER_STATE,state);p.syncData(ModAttachments.PLAYER_STATE);
            p.getData(ModAttachments.CLAW_COMBAT).reset();p.getData(ModAttachments.MOVEMENT_STATE).reset();
            EntityType<? extends Mob> type=switch(index){case 0->EntityType.COW;case 1->EntityType.ZOMBIE;case 2->EntityType.VILLAGER;case 3->ModEntities.BLACKLIGHT_HUNTER.get();case 5,6->EntityType.VILLAGER;default->EntityType.PIG;};
            var mob=type.create(p.level());mob.moveTo(20,220,(index==4||index==6)?10:20,0,0);mob.setNoAi(true);mob.setPersistenceRequired();
            if(index==1)mob.setHealth(4);if(index==3)mob.setHealth(12);
            p.serverLevel().addFreshEntity(mob);victimId=mob.getId();
        });
        if(phase==30) {
            // Exercise the real E binding and client -> server payload path.
            ClientClawEvents.onKey(new net.neoforged.neoforge.client.event.InputEvent.Key(org.lwjgl.glfw.GLFW.GLFW_KEY_E,0,org.lwjgl.glfw.GLFW.GLFW_PRESS,0));
        }
        if(phase>=74&&phase<87)server.execute(()->{
            var p=server.getPlayerList().getPlayer(uuid);p.teleportTo(p.getX()+.035,p.getY(),p.getZ());
        });
        if(phase==96)server.execute(()->{
            var p=server.getPlayerList().getPlayer(uuid);var v=server.overworld().getEntity(victimId);
            if(!ConsumeSystem.holding(p)||v==null||p.getHealth()!=10)throw new IllegalStateException("Grab must wait for a decision: "+NAMES[index]);
        });
        if(phase==100)net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.codex.blacklight.network.ClawActionPayload(
                index<5?com.codex.blacklight.network.ClawActionPayload.Action.CONSUME:
                index==5?com.codex.blacklight.network.ClawActionPayload.Action.THROW:com.codex.blacklight.network.ClawActionPayload.Action.GRAB));
        if(phase>=20&&phase<=169)pending=String.format("%s_%03d.png",NAMES[index],phase-20);
        if(phase==174)server.execute(()->{
            var p=server.getPlayerList().getPlayer(uuid);var data=new com.google.gson.JsonObject();data.addProperty("case",NAMES[index]);data.addProperty("grabDistance",index==4||index==6?12:2);
            data.addProperty("consumed",server.overworld().getEntity(victimId)==null);
            var victim=server.overworld().getEntity(victimId);
            data.addProperty("thrown",index>=5&&victim!=null&&!victim.getData(ModAttachments.CONSUME_VISUAL).active());
            if(index>=5) {
                double distance=victim==null?0:victim.distanceTo(p);data.addProperty("throwDistance",distance);
                if(distance<4)throw new IllegalStateException("Throw did not displace the target: "+distance);
            }
            if(index<5&&victim!=null||index>=5&&(p.getHealth()!=10||p.getData(ModAttachments.PLAYER_STATE).getEvolutionPoints()!=0))throw new IllegalStateException("Wrong grab outcome "+NAMES[index]);data.addProperty("health",p.getHealth());data.addProperty("biomass",p.getData(ModAttachments.PLAYER_STATE).getBiomass());data.addProperty("ep",p.getData(ModAttachments.PLAYER_STATE).getEvolutionPoints());
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
        if(name.startsWith("whip_") && mc.player!=null) {
            var hand=com.codex.blacklight.client.renderer.WhipTetherRenderer.capturedHand(mc.player.getId());
            if(hand==null||hand.distanceToSqr(mc.player.position())>9)throw new IllegalStateException("Whip detached from rendered arm: "+hand);
        }
        if(Boolean.getBoolean("blacklight.test.bladeStudy")&&mc.player!=null&&mc.level!=null) {
            var mob=mc.level.getEntity(victimId);
            var contact=com.codex.blacklight.client.renderer.WhipTetherRenderer.capturedGrip(mc.player.getId());
            if(mob instanceof LivingEntity living && contact!=null && scene%185>=55 && scene%185<140) {
                float partial=mc.getTimer().getGameTimeDeltaPartialTick(false);
                var delta=contact.subtract(living.getPosition(partial));
                var inv=com.codex.blacklight.client.animation.ConsumeVictimPose.bodyTransform(living,partial).invert();
                var local=inv.transformPosition(new org.joml.Vector3f((float)delta.x,(float)delta.y,(float)delta.z));
                double radius=mob.getBbWidth()*.5;
                double dx=Math.max(0,Math.abs(local.x)-radius),dz=Math.max(0,Math.abs(local.z)-radius),dy=Math.max(0,Math.max(-local.y,local.y-mob.getBbHeight()));
                double gap=Math.sqrt(dx*dx+dy*dy+dz*dz);
                if(!Double.isFinite(gap))throw new IllegalStateException("Non-finite blade contact");
                bladeGap=Math.max(bladeGap,gap);bladeSamples++;
            }
        }
        Screenshot.grab(new File(System.getProperty("blacklight.test.grabPreview")),name,mc.getMainRenderTarget(),m->{});
    }
}
