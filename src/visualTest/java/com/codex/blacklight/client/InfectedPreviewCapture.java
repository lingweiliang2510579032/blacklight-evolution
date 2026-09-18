package com.codex.blacklight.client;
import com.codex.blacklight.BlacklightNeoMod;
import com.codex.blacklight.infection.BlacklightInfectionSystem;
import com.codex.blacklight.registry.ModAttachments;
import com.codex.blacklight.power.PowerForm;
import com.codex.blacklight.combat.ClawCombatSystem;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import java.io.File;

/** A reproducible scene in a dedicated copy of the QA save, never the user's original world. */
@EventBusSubscriber(modid=BlacklightNeoMod.MOD_ID,value=Dist.CLIENT)
public final class InfectedPreviewCapture {
    private static int ticks,sceneTick;
    private static volatile int zombieId=-1;
    private static boolean initialized,done,oldTargetGuide;
    private static String pending;
    private static int oldFov; private static boolean oldGui,oldPause;private static CameraType oldCamera;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        String output=System.getProperty("blacklight.test.infectedCapture");if(output==null||done)return;
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null)return;
        var server=mc.getSingleplayerServer();
        if(!server.getWorldPath(LevelResource.ROOT).toString().contains("Blacklight Motion QA r8"))throw new IllegalStateException("Capture requires isolated r8 QA save");
        if(++ticks<80)return;
        if(!initialized) {
            initialized=true;new File(output,"screenshots").mkdirs();
            oldTargetGuide=yesman.epicfight.config.ClientConfig.enableTargetEntityGuide;
            yesman.epicfight.config.ClientConfig.enableTargetEntityGuide=false;
            oldFov=mc.options.fov().get();oldGui=mc.options.hideGui;oldCamera=mc.options.getCameraType();oldPause=mc.options.pauseOnLostFocus;
            mc.options.fov().set(40);mc.options.hideGui=true;mc.options.pauseOnLostFocus=false;mc.options.setCameraType(CameraType.FIRST_PERSON);
            var playerId=mc.player.getUUID();
            server.execute(()->{
                var level=server.overworld();var player=server.getPlayerList().getPlayer(playerId);if(player==null)return;
                var state=player.getData(ModAttachments.PLAYER_STATE);state.setPrototypeMode(false);state.activate(PowerForm.NONE);player.setData(ModAttachments.PLAYER_STATE,state);player.syncData(ModAttachments.PLAYER_STATE);
                player.setGameMode(GameType.CREATIVE);level.setDayTime(6000);
                level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false,server);
                for(int x=14;x<=27;x++)for(int z=14;z<=29;z++) {
                    level.setBlockAndUpdate(new BlockPos(x,219,z),Blocks.SMOOTH_QUARTZ.defaultBlockState());
                    level.setBlockAndUpdate(new BlockPos(x,225,z),(x%3==0&&z%3==0?Blocks.SEA_LANTERN:Blocks.SMOOTH_QUARTZ).defaultBlockState());
                }
                player.teleportTo(22.5,220,25.3);player.setYRot(155);player.setXRot(6);
                for(var old:level.getEntitiesOfClass(Zombie.class,new net.minecraft.world.phys.AABB(14,219,14,28,226,30)))if(old.getTags().contains("blacklight_r8_preview") || (old.isNoAi() && old.isInvulnerable()))old.discard();
                var zombie=EntityType.ZOMBIE.create(level);zombie.addTag("blacklight_r8_preview");zombie.moveTo(20,220,20,0,0);zombie.setNoAi(true);zombie.setInvulnerable(true);zombie.setPersistenceRequired();level.addFreshEntity(zombie);zombieId=zombie.getId();
            });return;
        }
        if(!(mc.level.getEntity(zombieId) instanceof Zombie zombie))return;
        if(mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen)mc.setScreen(null);
        sceneTick++;
        if(sceneTick<180){mc.player.setYRot(155);mc.player.setXRot(6);}
        if(sceneTick==25)pending="zombie_before.png";
        if(sceneTick==40)server.execute(()->{
            var z=server.overworld().getEntity(zombieId);if(z instanceof Zombie target)BlacklightInfectionSystem.infect(target,null);
        });
        if(sceneTick>=40&&sceneTick<=106&&sceneTick%2==0)pending=String.format("infection_%03d.png",sceneTick-40);
        if(sceneTick==130)pending="zombie_after.png";
        if(sceneTick==150) {
            var report=new com.google.gson.JsonObject();
            report.addProperty("infected",BlacklightInfectionSystem.isInfected(zombie));report.addProperty("infectionStart",zombie.getData(ModAttachments.INFECTION_START));
            report.addProperty("epicparcool",net.neoforged.fml.ModList.get().isLoaded("epicparcool"));
            report.addProperty("wallClip",com.codex.blacklight.integration.epicfight.BlacklightEpicFightAnimations.WALL_RUN.get().getLocation().toString());
            try {java.nio.file.Files.writeString(java.nio.file.Path.of(output,"runtime-check.json"),report.toString());}catch(Exception ex){throw new IllegalStateException(ex);}
        }
        if(sceneTick==180) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);mc.options.fov().set(55);
            server.execute(()->{var p=server.getPlayerList().getPlayer(mc.player.getUUID());p.teleportTo(23,220,23);});
        }
        if(sceneTick>=180) {mc.player.setYRot(0);mc.player.yBodyRot=0;mc.player.yHeadRot=0;mc.player.setXRot(0);}
        if(sceneTick==200)server.execute(()->{
            var p=server.getPlayerList().getPlayer(mc.player.getUUID());var state=p.getData(ModAttachments.PLAYER_STATE);state.infect();state.setPrototypeMode(true);state.activate(PowerForm.CLAWS);p.setData(ModAttachments.PLAYER_STATE,state);p.syncData(ModAttachments.PLAYER_STATE);ClawCombatSystem.playPrototypeTransition(p,true);
        });
        if(sceneTick>=196&&sceneTick<=242)pending=String.format("morph_%03d.png",sceneTick-196);
        if(sceneTick==256)pending="player_after.png";
        if(sceneTick==275)server.execute(()->{
            var p=server.getPlayerList().getPlayer(mc.player.getUUID());var state=p.getData(ModAttachments.PLAYER_STATE);state.activate(PowerForm.NONE);p.setData(ModAttachments.PLAYER_STATE,state);p.syncData(ModAttachments.PLAYER_STATE);ClawCombatSystem.playPrototypeTransition(p,false);
        });
        if(sceneTick>=275&&sceneTick<=309)pending=String.format("retract_%03d.png",sceneTick-275);
        if(sceneTick==330)server.execute(()->{
            var p=server.getPlayerList().getPlayer(mc.player.getUUID());var state=p.getData(ModAttachments.PLAYER_STATE);state.activate(PowerForm.CLAWS);p.setData(ModAttachments.PLAYER_STATE,state);p.syncData(ModAttachments.PLAYER_STATE);
        });
        if(sceneTick>=365 && sceneTick<=385) {
            var patch=yesman.epicfight.world.capabilities.EpicFightCapabilities.getEntityPatch(mc.player,yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch.class);
            if(patch!=null) {
                var animation=com.codex.blacklight.integration.epicfight.BlacklightEpicFightAnimations.WALL_RUN;
                patch.getAnimator().setHardPause(false);
                ((yesman.epicfight.api.client.animation.ClientAnimator)patch.getAnimator()).getAllLayers().forEach(layer->layer.disableLayer());
                patch.getAnimator().playAnimationInstantly(animation);
                float time=(sceneTick-365)*.025F;
                patch.getAnimator().getPlayerFor(animation).setElapsedTime(time,time);patch.getAnimator().setHardPause(true);
                pending=String.format("wall_pose_%03d.png",sceneTick-365);
            }
        }
        if(sceneTick>395) {
            yesman.epicfight.config.ClientConfig.enableTargetEntityGuide=oldTargetGuide;
            mc.options.fov().set(oldFov);mc.options.hideGui=oldGui;mc.options.setCameraType(oldCamera);mc.options.pauseOnLostFocus=oldPause;
            done=true;
            var patch=yesman.epicfight.world.capabilities.EpicFightCapabilities.getEntityPatch(mc.player,yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch.class);
            if(patch!=null) {patch.getAnimator().setHardPause(false);patch.getAnimator().resetLivingAnimations();patch.updateMotion(true);}
            BlacklightNeoMod.LOGGER.info("Infected preview capture completed");
            if(Boolean.getBoolean("blacklight.test.keepInfectedPreviewOpen")) {
                var uuid=mc.player.getUUID();
                server.execute(()->{
                    var level=server.overworld();var player=server.getPlayerList().getPlayer(uuid);
                    for(int y=220;y<=235;y++)for(int z=15;z<=27;z++)level.setBlockAndUpdate(new BlockPos(27,y,z),Blocks.STONE_BRICKS.defaultBlockState());
                    player.getInventory().add(new net.minecraft.world.item.ItemStack(com.codex.blacklight.registry.ModItems.BLACKLIGHT_SAMPLE.get(),16));
                    player.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ZOMBIE_SPAWN_EGG,8));
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal("R8 试玩：V 展开/收回利爪；背包内有病毒瓶和僵尸刷怪蛋。旁边石砖墙可测试贴墙跑。"),false);
                });
            } else mc.stop();
        }
    }
    @SubscribeEvent public static void frame(RenderFrameEvent.Post event) {
        if(pending==null||done)return;String name=pending;pending=null;var mc=Minecraft.getInstance();
        Screenshot.grab(new File(System.getProperty("blacklight.test.infectedCapture")),name,mc.getMainRenderTarget(),m->{});
    }
}
