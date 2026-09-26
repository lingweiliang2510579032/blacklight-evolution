package com.codex.blacklight.client;

import com.codex.blacklight.combat.*;
import com.codex.blacklight.power.*;
import com.codex.blacklight.registry.*;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import java.nio.file.*;

/** Opt-in screenshots in the dedicated QA world only. Never shipped in the mod. */
@EventBusSubscriber(modid="blacklight",value=Dist.CLIENT)
public final class GameplayPreview {
    private static int warmup,scene=-1;
    private static volatile int cameraId=-1;
    private static boolean done;
    private static String pending;
    @SubscribeEvent public static void tick(ClientTickEvent.Post e)throws Exception{
        String output=System.getProperty("blacklight.test.gameplayPreview");if(output==null||done)return;
        var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();if(mc.player==null||mc.level==null||server==null||++warmup<90)return;
        if(!server.getWorldPath(LevelResource.ROOT).toString().contains("Blacklight Consume QA r10"))throw new IllegalStateException("Isolated QA save required");
        var uuid=mc.player.getUUID();
        if(scene<0){
            if(Boolean.getBoolean("blacklight.test.adaptedReview"))auditAdaptedClips(Path.of(output));
            if(Boolean.getBoolean("blacklight.test.adaptAuditOnly")){done=true;mc.stop();return;}
            scene=0;Files.createDirectories(Path.of(output));mc.options.pauseOnLostFocus=false;mc.options.fov().set(55);mc.options.guiScale().set(2);mc.options.setCameraType(CameraType.FIRST_PERSON);
            server.execute(()->{
                var level=server.overworld();var p=server.getPlayerList().getPlayer(uuid);
                level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false,server);level.setDayTime(6000);level.setWeatherParameters(6000,0,false,false);
                for(var entity:level.getEntities(p,new net.minecraft.world.phys.AABB(0,215,0,70,245,65)))entity.discard();
                for(int x=0;x<66;x++)for(int z=0;z<60;z++)for(int y=219;y<236;y++)level.setBlock(new BlockPos(x,y,z),(y==219?Blocks.SMOOTH_QUARTZ:Blocks.AIR).defaultBlockState(),2);
                p.setGameMode(GameType.SURVIVAL);p.setInvulnerable(true);p.teleportTo(20,220,22);p.setYRot(180);p.setXRot(0);p.getInventory().clearContent();
                var state=p.getData(ModAttachments.PLAYER_STATE);state.infect();state.setPrototypeMode(true);for(var f:PowerForm.values())state.unlock(f);state.activate(PowerForm.BLADE);p.syncData(ModAttachments.PLAYER_STATE);
                var cam=EntityType.ARMOR_STAND.create(level);cam.moveTo(27,222,24,110,8);cam.setInvisible(true);cam.setNoGravity(true);level.addFreshEntity(cam);cameraId=cam.getId();
            });return;
        }
        var camera=mc.level.getEntity(cameraId);if(camera==null)return;mc.setCameraEntity(camera);
        boolean pack=Boolean.getBoolean("blacklight.test.packReview");
        boolean adapted=Boolean.getBoolean("blacklight.test.adaptedReview");
        boolean review=pack||adapted||Boolean.getBoolean("blacklight.test.attackReview");
        int sequence=scene/90,phase=scene%90;scene++;
        int[] reviewed=pack?new int[]{1,2,3,4,6,7,8,9,16,17,18,19}:adapted?new int[]{1,2,6,7,16,17,18,19,20}:new int[]{1,2,3,4,10,13};
        String[] packClips={"wom:biped/combat/agony_auto_1","wom:biped/combat/agony_auto_2","wom:biped/combat/agony_auto_3","wom:biped/combat/agony_auto_4",
            "wom:biped/combat/torment_auto_1","wom:biped/combat/torment_auto_2","wom:biped/combat/torment_auto_3","wom:biped/combat/torment_charged_attack_1",
            "epicfight:biped/combat/fist_auto1","epicfight:biped/combat/fist_auto2","epicfight:biped/combat/fist_auto3","epicfight:biped/combat/fist_dash"};
        final int index=review?(sequence>=reviewed.length*2?28:reviewed[sequence%reviewed.length]):sequence;
        final int shot=index;
        if(index<=20||index>=23){camera.setYRot(110);camera.yRotO=110;camera.setXRot(8);camera.xRotO=8;}
        camera.setYHeadRot(camera.getYRot());if(camera instanceof LivingEntity living)living.yHeadRotO=camera.getYRot();
        if(review&&sequence>=reviewed.length&&index<28){
            camera.setYRot(30);camera.yRotO=30;camera.setYHeadRot(30);if(camera instanceof LivingEntity living)living.yHeadRotO=30;
            if(phase==0)server.execute(()->server.overworld().getEntity(cameraId).setPos(24,222,15));
        }
        if(index>=28){done=true;mc.setScreen(null);mc.setCameraEntity(mc.player);mc.options.hideGui=false;Files.writeString(Path.of(output,"capture-complete.txt"),pack?"12 dependency animations captured from 2 angles; QA only":adapted?"9 adapted attacks captured from 2 angles":review?"6 authored attacks captured from 2 angles":"wheel, 20 weapon moves, mutant lineup, hive building, armour, shield, senses and progression captured");mc.stop();return;}
        if(mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen)mc.setScreen(null);
        mc.options.hideGui=index!=0&&index<26;
        mc.player.setYRot(180);mc.player.setXRot(0);
        mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot(180,0,mc.player.onGround()));
        if(index==0){
            if(phase==20)mc.setScreen(new com.codex.blacklight.client.screen.PowerWheelScreen(new KeyMapping("qa",org.lwjgl.glfw.GLFW.GLFW_KEY_Z,"qa")));
            if(phase==50)pending="wheel.png";if(phase==80)mc.setScreen(null);return;
        }
        if(index<=20){
            var move=WeaponMove.values()[index-1];
            if(phase==0)server.execute(()->{
                var p=server.getPlayerList().getPlayer(uuid);WeaponCombatSystem.cancel(p);p.getData(ModAttachments.CLAW_COMBAT).reset();p.getData(ModAttachments.MOVEMENT_STATE).reset();
                p.teleportTo(20,220,22);p.setYRot(180);p.setOnGround(true);p.setSprinting(false);
                var state=p.getData(ModAttachments.PLAYER_STATE);state.activate(move.form);p.syncData(ModAttachments.PLAYER_STATE);
                for(var entity:server.overworld().getEntities(p,new net.minecraft.world.phys.AABB(15,220,10,25,225,23)))if(entity instanceof Mob)entity.discard();
                if(!pack){var target=EntityType.IRON_GOLEM.create(server.overworld());target.moveTo(adapted?18.8:20,220,adapted?19.8:move.form==PowerForm.WHIPFIST?16:20,0,0);target.setNoAi(true);server.overworld().addFreshEntity(target);}
            });
            // First two basic hits are sampled as actual first/second combo requests.
            if(!pack&&phase==12&&index%5==2)server.execute(()->WeaponCombatSystem.request(server.getPlayerList().getPlayer(uuid),false));
            int start=!pack&&index%5==2?40:20;
            if(phase==start)server.execute(()->{
                var p=server.getPlayerList().getPlayer(uuid);p.setSprinting(move.rush());
                if(pack){
                    p.setSprinting(false);
                    String key=packClips[sequence%reviewed.length];
                    var animation=yesman.epicfight.api.animation.AnimationManager.byKey(key);
                    if(animation==null)throw new IllegalStateException("Missing reference animation: "+key);
                    yesman.epicfight.world.capabilities.EpicFightCapabilities.getServerPlayerPatch(p).playAnimationSynchronized(animation,0);
                    System.out.println("PACK_REVIEW "+key);return;
                }
                if(index%5==0){p.teleportTo(20,224,22);p.setOnGround(false);}
                WeaponCombatSystem.request(p,index%5==3);
            });
            if(phase==start+move.start+1)pending=move.id+".png";
            if(review&&phase>=12&&phase<=80&&phase%2==0)pending=String.format("r20_%s_%s_%03d.png",sequence<reviewed.length?"side":"front",move.id,(phase-12)/2);
            if(pack&&phase>=12&&phase<=80&&phase%2==0)pending=String.format("pack_%s_%s_%03d.png",sequence<reviewed.length?"side":"front",packClips[sequence%reviewed.length].replace(':','_').replace('/','_'),(phase-12)/2);
            if(adapted&&phase>=12&&phase<=80&&phase%2==0)pending=String.format("adapted_%s_%s_%03d.png",sequence<reviewed.length?"side":"front",move.id,(phase-12)/2);
        }else if(index==21){
            if(phase==0)server.execute(()->{
                var p=server.getPlayerList().getPlayer(uuid);WeaponCombatSystem.cancel(p);p.teleportTo(35,220,24);
                for(var entity:server.overworld().getEntities(p,new net.minecraft.world.phys.AABB(15,220,10,25,225,23)))if(entity instanceof Mob)entity.discard();
                var types=java.util.List.of(ModEntities.BLACKLIGHT_STALKER,ModEntities.BLACKLIGHT_SPITTER,ModEntities.BLACKLIGHT_BRUTE);
                for(int i=0;i<types.size();i++){var mob=types.get(i).get().create(server.overworld());mob.moveTo(17+i*4,220,20,0,0);mob.setNoAi(true);server.overworld().addFreshEntity(mob);}
                var cam=server.overworld().getEntity(cameraId);cam.setPos(21,222,31);
            });
            camera.setYRot(180);camera.yRotO=180;camera.setXRot(5);camera.xRotO=5;
            camera.setYHeadRot(180);if(camera instanceof LivingEntity living)living.yHeadRotO=180;
            if(phase==60)pending="infected-lineup.png";
        }else if(index==22){
            if(phase==0)server.execute(()->{
                var level=server.overworld();var origin=new BlockPos(45,220,35);com.codex.blacklight.world.HiveBuilding.build(level,origin);
                var hive=ModEntities.BLACKLIGHT_HIVE.get().create(level);hive.moveTo(45.5,221,35.5,0,0);level.addFreshEntity(hive);
                server.getPlayerList().getPlayer(uuid).teleportTo(43,220,24);var cam=level.getEntity(cameraId);cam.setPos(28,227,15);
            });
            camera.setYRot(-42);camera.yRotO=-42;camera.setXRot(2);camera.xRotO=2;
            camera.setYHeadRot(-42);if(camera instanceof LivingEntity living)living.yHeadRotO=-42;
            if(phase==60)pending="hive-building.png";
        }else{
            if(phase==0)server.execute(()->{
                var p=server.getPlayerList().getPlayer(uuid);p.teleportTo(20,220,22);p.setYRot(180);p.setOnGround(true);p.setSprinting(false);
                server.overworld().getEntity(cameraId).setPos(27,222,24);
                var state=p.getData(ModAttachments.PLAYER_STATE);state.activate(PowerForm.BLADE);state.addBiomass(750);p.syncData(ModAttachments.PLAYER_STATE);
                if(shot==23)UtilityPowerSystem.toggle(p,PowerForm.ARMOR);
                if(shot==24)UtilityPowerSystem.toggle(p,PowerForm.SHIELD);
                if(shot==25)UtilityPowerSystem.toggle(p,PowerForm.INFECTED_VISION);
            });
            if(index==26&&phase==20){var wheel=new com.codex.blacklight.client.screen.PowerWheelScreen(new KeyMapping("qa",org.lwjgl.glfw.GLFW.GLFW_KEY_Z,"qa"));mc.setScreen(wheel);wheel.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_TAB,0,0);}
            if(index==27&&phase==20){var upgrade=new com.codex.blacklight.client.screen.UpgradeScreen();mc.setScreen(upgrade);upgrade.mouseClicked(30,150,0);}
            if(phase==60)pending=switch(index){case 23->"armour.png";case 24->"shield.png";case 25->"infected-vision.png";case 26->"utility-wheel.png";default->"growth.png";};
            if(phase==80)mc.setScreen(null);
        }
    }
    @SubscribeEvent public static void frame(RenderFrameEvent.Post e){if(pending==null)return;var mc=Minecraft.getInstance();Screenshot.grab(new java.io.File(System.getProperty("blacklight.test.gameplayPreview")),pending,mc.getMainRenderTarget(),m->{});pending=null;}
    private static void auditAdaptedClips(Path output)throws Exception{
        var report=new com.google.gson.JsonArray();
        var rig=yesman.epicfight.gameasset.Armatures.BIPED.get();
        for(var move:WeaponMove.values()){
            if(WeaponMotionProfile.forMove(move)==null)continue;
            String model=switch(move.form){case BLADE->"blade";case HAMMERFISTS->"hammerfists";default->"muscle_mass";};
            var path=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("blacklight","models/form_"+model+".json");
            var resource=Minecraft.getInstance().getResourceManager().getResource(path);if(resource.isEmpty())continue;
            com.google.gson.JsonObject joints;
            try(var reader=resource.get().openAsReader()){joints=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("joints");}
            var points=new java.util.HashMap<String,java.util.List<yesman.epicfight.api.utils.math.Vec3f>>();
            for(var entry:joints.entrySet()){
                if(!entry.getKey().startsWith("Hand_"))continue;
                var unique=new java.util.HashSet<String>();var list=new java.util.ArrayList<yesman.epicfight.api.utils.math.Vec3f>();
                for(var face:entry.getValue().getAsJsonArray())for(var point:face.getAsJsonObject().getAsJsonArray("p")){
                    if(!unique.add(point.toString()))continue;var a=point.getAsJsonArray();
                    list.add(new yesman.epicfight.api.utils.math.Vec3f(a.get(0).getAsFloat(),a.get(1).getAsFloat(),a.get(2).getAsFloat()));
                }points.put(entry.getKey(),list);
            }
            var animation=com.codex.blacklight.integration.epicfight.BlacklightEpicFightAnimations.weapon(move).get();
            var dump=new com.google.gson.JsonArray();
            for(var entry:animation.getAnimationClip().getJointTransforms().entrySet()){
                var track=new com.google.gson.JsonObject();track.addProperty("name",entry.getKey());
                var times=new com.google.gson.JsonArray();var transforms=new com.google.gson.JsonArray();
                for(var frame:entry.getValue().getKeyframes()){
                    times.add(frame.time());var jt=frame.transform();var tr=new com.google.gson.JsonObject();
                    var loc=new com.google.gson.JsonArray();loc.add(jt.translation().x);loc.add(jt.translation().y);loc.add(jt.translation().z);tr.add("loc",loc);
                    var rot=new com.google.gson.JsonArray();rot.add(jt.rotation().w);rot.add(jt.rotation().x);rot.add(jt.rotation().y);rot.add(jt.rotation().z);tr.add("rot",rot);
                    var sca=new com.google.gson.JsonArray();sca.add(jt.scale().x);sca.add(jt.scale().y);sca.add(jt.scale().z);tr.add("sca",sca);transforms.add(tr);
                }track.add("time",times);track.add("transform",transforms);dump.add(track);
            }
            var clipDump=new com.google.gson.JsonObject();clipDump.add("animation",dump);Files.createDirectories(output.resolve("audit-clips"));Files.writeString(output.resolve("audit-clips/"+move.id+".json"),clipDump.toString());
            double minimum=99;float closest=0;
            for(int frame=0;frame<(move.duration-1)*3;frame++){
                float time=frame/60F;var pose=animation.getRawPose(time);
                var head=rig.getBoundTransformFor(pose,rig.searchJointByName("Head"));
                var inverse=yesman.epicfight.api.utils.math.OpenMatrix4f.invert(head,null);
                for(var entry:points.entrySet()){
                    var joint=rig.getBoundTransformFor(pose,rig.searchJointByName(entry.getKey()));
                    var relative=yesman.epicfight.api.utils.math.OpenMatrix4f.mul(inverse,joint,null);
                    for(var vertex:entry.getValue()){
                        var v=yesman.epicfight.api.utils.math.OpenMatrix4f.transform3v(relative,vertex,null);
                        double x=Math.max(0,Math.abs(v.x)-.25),y=Math.max(0,Math.abs(v.y-.25)-.25),z=Math.max(0,Math.abs(v.z)-.25);
                        double distance=Math.sqrt(x*x+y*y+z*z);
                        if(distance<minimum){minimum=distance;closest=time;}
                    }
                }
            }
            var result=new com.google.gson.JsonObject();result.addProperty("move",move.id);result.addProperty("handHeadClearance",minimum);result.addProperty("time",closest);report.add(result);
        }
        Files.createDirectories(output);Files.writeString(output.resolve("adapted-clearance.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report));
    }
}
