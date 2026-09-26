package com.codex.blacklight.client;

import com.codex.blacklight.BlacklightNeoMod;
import com.codex.blacklight.combat.*;
import com.codex.blacklight.power.PowerForm;
import com.codex.blacklight.registry.ModAttachments;
import com.codex.blacklight.client.renderer.WhipTetherRenderer;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import java.nio.file.*;

/** Contact verification deliberately does not force yBodyRot as the old side-view harness did. */
@EventBusSubscriber(modid=BlacklightNeoMod.MOD_ID,value=Dist.CLIENT)
public final class GrabContactPreview {
    private static final boolean BLADE=Boolean.getBoolean("blacklight.test.bladeContact");
    private static final String[] NAMES={"chicken_above","chicken_side","baby_chicken","pig","villager","chicken_turn"};
    private static int ticks,scene=-1;
    private static volatile int cameraId=-1,victimId=-1;
    private static boolean done;
    private static String pending;
    private static int current;
    private static final double[] MAX=new double[NAMES.length];
    private static final int[] SAMPLES=new int[NAMES.length];
    private static final double[] AIM_ERROR=new double[NAMES.length];
    private static final int[] AIM_SAMPLES=new int[NAMES.length];
    private static final com.google.gson.JsonArray RESULTS=new com.google.gson.JsonArray();
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) throws Exception {
        String output=System.getProperty("blacklight.test.contactPreview");
        if(output==null||done)return;
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null)return;
        var server=mc.getSingleplayerServer();
        if(!server.getWorldPath(LevelResource.ROOT).toString().contains("Blacklight Consume QA r10"))
            throw new IllegalStateException("Requires isolated contact QA save");
        if(++ticks<80)return;
        var uuid=mc.player.getUUID();
        if(scene<0) {
            scene=0;Files.createDirectories(Path.of(output,"screenshots"));
            mc.options.hideGui=true;mc.options.pauseOnLostFocus=false;mc.options.fov().set(50);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server.execute(()->{
                var level=server.overworld();var p=server.getPlayerList().getPlayer(uuid);
                level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false,server);
                level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_NATURAL_REGENERATION).set(false,server);
                level.setDayTime(6000);level.setWeatherParameters(6000,0,false,false);
                for(var e:level.getEntities(p,new net.minecraft.world.phys.AABB(10,215,0,40,235,35)))e.discard();
                for(int x=12;x<37;x++)for(int z=2;z<33;z++)for(int y=219;y<227;y++)
                    level.setBlockAndUpdate(new BlockPos(x,y,z),(y==219?Blocks.SMOOTH_QUARTZ:Blocks.AIR).defaultBlockState());
                p.getInventory().clearContent();p.setGameMode(GameType.SURVIVAL);p.teleportTo(20,220,22);
                var cam=EntityType.ARMOR_STAND.create(level);cam.moveTo(20,226,26,180,65);
                cam.setInvisible(true);cam.setNoGravity(true);level.addFreshEntity(cam);cameraId=cam.getId();
            });return;
        }
        var camera=mc.level.getEntity(cameraId);if(camera==null)return;
        int index=scene/200,phase=scene%200;scene++;
        if(index==NAMES.length) {
            done=true;Files.writeString(Path.of(output,"contact-check.json"),RESULTS.toString());
            boolean ok=true;for(int i=0;i<NAMES.length;i++)ok&=SAMPLES[i]>20&&MAX[i]<.18&&AIM_SAMPLES[i]==3&&AIM_ERROR[i]<.2;
            mc.setCameraEntity(mc.player);mc.options.hideGui=false;mc.stop();
            if(!ok)throw new IllegalStateException("Rendered palm contact failed: "+RESULTS);
            return;
        }
        current=index;mc.setCameraEntity(camera);
        boolean above=index==0||index==2||index==5;
        camera.setYRot(above?180:90);camera.yRotO=camera.getYRot();
        camera.setXRot(above?65:8);camera.xRotO=camera.getXRot();
        camera.setYHeadRot(camera.getYRot());
        if(camera instanceof LivingEntity living)living.yHeadRotO=camera.getYRot();
        float yaw=index==5&&phase>=65?180+Math.min(100,(phase-65)*2):180;
        float pitch=phase<45?0:phase<80?70:phase<115?-70:0;
        mc.player.setYRot(yaw);mc.player.setXRot(pitch);
        // Detached QA cameras suppress LocalPlayer's ordinary movement packets.
        // Send the same rotation packet a player-controlled camera sends in game.
        mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot(yaw,pitch,mc.player.onGround()));
        if(mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen)mc.setScreen(null);
        if(phase==0)server.execute(()->{
            var p=server.getPlayerList().getPlayer(uuid);ConsumeSystem.cancel(p);
            var old=server.overworld().getEntity(victimId);if(old!=null)old.discard();
            var cam=server.overworld().getEntity(cameraId);cam.setPos(above?20:26,above?226:220.1,above?26:21);
            p.teleportTo(20,220,22);p.setYRot(180);p.setXRot(0);p.setOnGround(true);p.setHealth(10);
            var state=p.getData(ModAttachments.PLAYER_STATE);state.cure();state.infect();state.setPrototypeMode(true);state.unlock(BLADE?PowerForm.BLADE:PowerForm.CLAWS);state.activate(BLADE?PowerForm.BLADE:PowerForm.CLAWS);
            p.setData(ModAttachments.PLAYER_STATE,state);p.syncData(ModAttachments.PLAYER_STATE);
            p.getData(ModAttachments.CLAW_COMBAT).reset();p.getData(ModAttachments.MOVEMENT_STATE).reset();
            EntityType<? extends Mob> type=index==3?EntityType.PIG:index==4?EntityType.VILLAGER:EntityType.CHICKEN;
            var mob=type.create(p.level());if(index==2)((net.minecraft.world.entity.animal.Chicken)mob).setBaby(true);
            mob.moveTo(20,220,20,0,0);mob.setNoAi(true);mob.setPersistenceRequired();p.serverLevel().addFreshEntity(mob);victimId=mob.getId();
        });
        if(phase==20)ClientClawEvents.onKey(new InputEvent.Key(org.lwjgl.glfw.GLFW.GLFW_KEY_E,0,org.lwjgl.glfw.GLFW.GLFW_PRESS,0));
        if(phase>=45&&phase<140) {
            // Use ordinary view packets; the player's body yaw remains free to lag.
            if(phase==60||phase==95||phase==125) {
                pending=NAMES[index]+"_"+phase+".png";
                server.execute(()->{
                    var p=server.getPlayerList().getPlayer(uuid);var v=server.overworld().getEntity(victimId);
                    var expected=BLADE?GrabPlacement.bladeAnchor(p.position(),yaw,pitch,v.getBbHeight(),0):GrabPlacement.anchor(p.position(),yaw,pitch,v.getBbWidth(),v.getBbHeight());
                    AIM_ERROR[index]=Math.max(AIM_ERROR[index],v.position().distanceTo(expected));
                    AIM_SAMPLES[index]++;
                });
            }
        }
        if(phase==140)net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                new com.codex.blacklight.network.ClawActionPayload(com.codex.blacklight.network.ClawActionPayload.Action.CONSUME));
        if(phase==195)server.execute(()->{
            var p=server.getPlayerList().getPlayer(uuid);
            if(server.overworld().getEntity(victimId)!=null||p.getData(ModAttachments.CONSUMING))
                throw new IllegalStateException("Contact scenario did not finish "+NAMES[index]);
            var result=new com.google.gson.JsonObject();result.addProperty("case",NAMES[index]);result.addProperty("form",BLADE?"blade":"claws");
            result.addProperty("maxPalmToBodyBounds",MAX[index]);result.addProperty("renderSamples",SAMPLES[index]);
            result.addProperty("maxViewAnchorError",AIM_ERROR[index]);result.addProperty("viewSamples",AIM_SAMPLES[index]);RESULTS.add(result);
        });
    }
    @SubscribeEvent public static void frame(RenderFrameEvent.Post event) {
        if(System.getProperty("blacklight.test.contactPreview")==null||done)return;
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;
        var mob=mc.level.getEntity(victimId);var palm=WhipTetherRenderer.capturedGrip(mc.player.getId());
        if(mob!=null&&palm!=null&&scene%200>=50&&scene%200<135) {
            float partial=mc.getTimer().getGameTimeDeltaPartialTick(false);
            var point=palm.subtract(mob.getPosition(partial));
            // Distance to the complete visible body bounds catches floating gaps
            // independently of the desired attachment constants.
            double r=mob.getBbWidth()*.5;
            double dx=Math.max(0,Math.abs(point.x)-r),dz=Math.max(0,Math.abs(point.z)-r);
            double dy=Math.max(0,Math.max(-point.y,point.y-mob.getBbHeight()));
            double distance=Math.sqrt(dx*dx+dy*dy+dz*dz);
            if(!Double.isFinite(distance))throw new IllegalStateException("Non-finite rendered palm");
            if(distance>MAX[current]+.01) {
                System.out.println("CONTACT_DETAIL "+NAMES[current]+" phase="+(scene%200)+" distance="+distance+" palm="+palm+" mob="+mob.getPosition(partial));
                if(distance>.18)Screenshot.grab(new java.io.File(System.getProperty("blacklight.test.contactPreview")),NAMES[current]+"_worst.png",mc.getMainRenderTarget(),m->{});
            }
            MAX[current]=Math.max(MAX[current],distance);SAMPLES[current]++;
        }
        if(pending!=null) {
            Screenshot.grab(new java.io.File(System.getProperty("blacklight.test.contactPreview")),pending,mc.getMainRenderTarget(),m->{});pending=null;
        }
    }
}
