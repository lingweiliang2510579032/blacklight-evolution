package com.codex.blacklight.client;

import com.codex.blacklight.combat.*;
import com.codex.blacklight.entity.GroundSpikeEntity;
import com.codex.blacklight.integration.epicfight.BlacklightEpicFightAnimations;
import com.codex.blacklight.network.ClawActionPayload;
import com.codex.blacklight.power.PowerForm;
import com.codex.blacklight.registry.ModAttachments;
import com.google.gson.*;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/** Opt-in, isolated-world capture of actual server-owned claw actions. Excluded from releases. */
@EventBusSubscriber(modid="blacklight",value=Dist.CLIENT)
public final class ClawReferencePreview {
    private static final String PROPERTY="blacklight.test.clawReferencePreview";
    private static final int ACTION_AT=20,CAPTURE_FROM=10;
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static final Queue<JsonObject> observations=new ConcurrentLinkedQueue<>();
    private static final Queue<String> screenshots=new ConcurrentLinkedQueue<>();
    private static final Queue<String> skippedFrames=new ConcurrentLinkedQueue<>();
    private static final AtomicInteger written=new AtomicInteger();
    private static final EnumSet<ClawCombatState.Action> queued=EnumSet.noneOf(ClawCombatState.Action.class);
    private static volatile int cameraId=-1;
    private static volatile boolean prepared,serverFinished;
    private static int warmup,scene=-1,phase,sceneTicks,captureEnd,saved;
    private static boolean done;
    private static String pending;

    @SubscribeEvent public static void tick(ClientTickEvent.Post event)throws Exception {
        String destination=System.getProperty(PROPERTY);
        if(destination==null||done)return;
        var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();
        if(mc.player==null||mc.level==null||server==null||++warmup<90)return;
        if(System.getProperty("blacklight.test.gameplayPreview")!=null)
            throw new IllegalStateException("ClawReferencePreview requires gameplayPreview to be disabled");
        if(!server.getWorldPath(LevelResource.ROOT).toString().contains("Blacklight Consume QA r10"))
            throw new IllegalStateException("ClawReferencePreview requires isolated Blacklight Consume QA r10 save");
        Path output=Path.of(destination);var uuid=mc.player.getUUID();
        if(scene<0){
            Files.createDirectories(output);
            auditGroundspike(output);
            int groundTicks=GroundSpikeRules.PLANT_TICKS+GroundSpikeEntity.ERUPTION_DURATION_TICKS+GroundSpikeRules.WITHDRAW_TICKS;
            int comboTicks=CombatAnimationSpec.CLAW_AUTO_1.cancelStart()+CombatAnimationSpec.CLAW_AUTO_2.cancelStart()+CombatAnimationSpec.CLAW_AUTO_3.duration();
            captureEnd=ACTION_AT+Math.max(groundTicks,comboTicks)+24;sceneTicks=captureEnd+8;
            mc.options.pauseOnLostFocus=false;mc.options.fov().set(55);mc.options.framerateLimit().set(60);
            mc.options.hideGui=true;mc.options.setCameraType(CameraType.FIRST_PERSON);mc.setScreen(null);
            scene=0;
            server.execute(()->prepare(server,uuid));
            return;
        }
        if(!prepared)return;
        if(scene>=4){
            if(!serverFinished||pending!=null||written.get()<saved)return;
            verifyObservedActions();
            var manifest=new JsonObject();manifest.addProperty("requestedFps",20);manifest.addProperty("captureStartTick",CAPTURE_FROM);
            manifest.addProperty("captureEndTick",captureEnd);manifest.addProperty("sceneTicks",sceneTicks);
            manifest.addProperty("savedFrames",saved);manifest.addProperty("skippedRenderFrames",skippedFrames.size());
            manifest.add("skippedFrameNames",JSON.toJsonTree(skippedFrames));manifest.add("observations",JSON.toJsonTree(observations));
            manifest.add("screenshots",JSON.toJsonTree(screenshots));
            Files.writeString(output.resolve("capture-manifest.json"),JSON.toJson(manifest));
            for(String filename:screenshots)if(!Files.isRegularFile(output.resolve("screenshots").resolve(filename)))
                throw new IllegalStateException("Screenshot write failed: "+filename);
            Files.writeString(output.resolve("capture-complete.txt"),"Actual server groundspike and queued AUTO_1 / AUTO_2 / AUTO_3, side and front-three-quarter. "
                    +saved+" frames; requested 20 fps; skipped render frames: "+skippedFrames.size()+". See capture-manifest.json and groundspike-clearance.json.");
            mc.setCameraEntity(mc.player);mc.options.hideGui=false;done=true;mc.stop();return;
        }
        var camera=mc.level.getEntity(cameraId);if(camera==null)return;
        mc.setCameraEntity(camera);mc.setScreen(null);mc.options.hideGui=true;
        boolean front=scene>=2,ground=(scene%2)==0;
        float yaw=front?39.8F:90F,pitch=7F;
        camera.setYRot(yaw);camera.yRotO=yaw;camera.setXRot(pitch);camera.xRotO=pitch;camera.setYHeadRot(yaw);
        if(camera instanceof LivingEntity living)living.yHeadRotO=yaw;
        mc.player.setYRot(180);mc.player.setXRot(0);mc.player.setSprinting(false);
        mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot(180,0,mc.player.onGround()));
        final int currentScene=scene,currentPhase=phase;
        server.execute(()->advance(server,uuid,currentScene,currentPhase));
        if(phase>=CAPTURE_FROM&&phase<=captureEnd){
            if(pending!=null)skippedFrames.add(pending);
            pending=String.format("%s_%s_%04d.png",ground?"groundspike":"claw_combo",front?"front_three_quarter":"side",phase-CAPTURE_FROM);
        }
        if(++phase>=sceneTicks){phase=0;scene++;}
    }

    private static void prepare(MinecraftServer server,UUID uuid){
        var level=server.overworld();var player=server.getPlayerList().getPlayer(uuid);
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
        level.setDayTime(6000);level.setWeatherParameters(6000,0,false,false);
        for(var entity:level.getEntities(player,new AABB(0,215,0,70,245,65)))entity.discard();
        for(int x=0;x<66;x++)for(int z=0;z<60;z++)for(int y=219;y<236;y++)
            level.setBlock(new BlockPos(x,y,z),(y==219?Blocks.SMOOTH_QUARTZ:Blocks.AIR).defaultBlockState(),2);
        player.setGameMode(GameType.SURVIVAL);player.setInvulnerable(true);player.getInventory().clearContent();
        var state=player.getData(ModAttachments.PLAYER_STATE);state.infect();state.setPrototypeMode(true);state.unlock(PowerForm.CLAWS);state.activate(PowerForm.CLAWS);player.syncData(ModAttachments.PLAYER_STATE);
        var camera=EntityType.ARMOR_STAND.create(level);camera.moveTo(27,220.1,22,90,7);camera.setInvisible(true);camera.setNoGravity(true);level.addFreshEntity(camera);cameraId=camera.getId();
        prepared=true;
    }

    private static void advance(MinecraftServer server,UUID uuid,int sceneIndex,int tick){
        var player=server.getPlayerList().getPlayer(uuid);if(player==null)return;
        boolean ground=sceneIndex%2==0,front=sceneIndex>=2;
        if(tick==0){
            WeaponCombatSystem.cancel(player);player.getData(ModAttachments.CLAW_COMBAT).reset();player.getData(ModAttachments.MOVEMENT_STATE).reset();queued.clear();
            player.teleportTo(20,220,22);player.setYRot(180);player.setXRot(0);player.setSprinting(false);player.setDeltaMovement(0,0,0);player.setOnGround(true);
            var patch=yesman.epicfight.world.capabilities.EpicFightCapabilities.getServerPlayerPatch(player);
            if(patch!=null)patch.playAnimationSynchronized(yesman.epicfight.gameasset.Animations.BIPED_IDLE,0);
            for(var entity:server.overworld().getEntities(player,new AABB(10,219,0,30,229,30)))
                if(entity instanceof Mob||entity instanceof GroundSpikeEntity)entity.discard();
            double centerZ=ground?22:20.35;
            var camera=server.overworld().getEntity(cameraId);
            camera.moveTo(front?25:27,front?220.2:220.1,front?centerZ-6:centerZ,front?39.8F:90F,7F);
        }
        if(tick==ACTION_AT){
            ClawCombatSystem.handle(player,ground?ClawActionPayload.Action.GROUND_SPIKE_CHARGE_START:ClawActionPayload.Action.ATTACK);
            System.out.println("CLAW_REFERENCE_REQUEST scene="+sceneIndex+" action="+(ground?"groundspike":"combo"));
        }
        var combat=player.getData(ModAttachments.CLAW_COMBAT);
        if(!ground&&tick>ACTION_AT&&(combat.action()==ClawCombatState.Action.AUTO_1||combat.action()==ClawCombatState.Action.AUTO_2)
                &&combat.elapsedTicks()>=2&&queued.add(combat.action()))
            ClawCombatSystem.handle(player,ClawActionPayload.Action.ATTACK);
        var sample=new JsonObject();sample.addProperty("scene",sceneIndex);sample.addProperty("tick",tick);
        sample.addProperty("action",combat.action().name());sample.addProperty("elapsed",combat.elapsedTicks());sample.addProperty("queued",combat.queuedAttack());
        sample.addProperty("serverTick",server.getTickCount());sample.addProperty("x",player.getX());sample.addProperty("y",player.getY());sample.addProperty("z",player.getZ());
        observations.add(sample);
        if(sceneIndex==3&&tick==sceneTicks-1)serverFinished=true;
    }

    @SubscribeEvent public static void frame(RenderFrameEvent.Post event){
        if(pending==null||done)return;
        String filename=pending;pending=null;saved++;screenshots.add(filename);
        var mc=Minecraft.getInstance();
        Screenshot.grab(new java.io.File(System.getProperty(PROPERTY)),filename,mc.getMainRenderTarget(),message->written.incrementAndGet());
    }

    private static void verifyObservedActions(){
        for(int index=0;index<4;index++){
            var observed=EnumSet.noneOf(ClawCombatState.Action.class);boolean recovered=false;
            for(var sample:observations){
                if(sample.get("scene").getAsInt()!=index)continue;
                int tick=sample.get("tick").getAsInt();
                var action=ClawCombatState.Action.valueOf(sample.get("action").getAsString());observed.add(action);
                if(tick>ACTION_AT+10&&action==ClawCombatState.Action.IDLE)recovered=true;
                if(index%2==0&&(action==ClawCombatState.Action.GROUND_SPIKE_CHARGING||action==ClawCombatState.Action.GROUND_SPIKE)
                        &&(Math.abs(sample.get("x").getAsDouble()-20)>.05||Math.abs(sample.get("z").getAsDouble()-22)>.05))
                    throw new IllegalStateException("Groundspike moved from its planted position in scene "+index);
            }
            var required=index%2==0?EnumSet.of(ClawCombatState.Action.GROUND_SPIKE_CHARGING,ClawCombatState.Action.GROUND_SPIKE)
                    :EnumSet.of(ClawCombatState.Action.AUTO_1,ClawCombatState.Action.AUTO_2,ClawCombatState.Action.AUTO_3);
            if(!observed.containsAll(required)||!recovered)
                throw new IllegalStateException("Incomplete real claw action in scene "+index+": "+observed+", recovered="+recovered);
        }
    }

    /** Samples the loaded runtime clip, all unique forearm/claw vertices, and the posed head cube. */
    private static void auditGroundspike(Path output)throws Exception {
        var mc=Minecraft.getInstance();
        var model=ResourceLocation.fromNamespaceAndPath("blacklight","models/claw_blender.json");
        var resource=mc.getResourceManager().getResource(model).orElseThrow(()->new IllegalStateException("Missing claw model: "+model));
        JsonObject joints;
        try(var reader=resource.openAsReader()){joints=JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("joints");}
        var points=new TreeMap<String,List<Vec3f>>();
        for(String jointName:List.of("Hand_R","Hand_L")){
            if(!joints.has(jointName))throw new IllegalStateException("Missing claw joint: "+jointName);
            var unique=new HashSet<String>();var vertices=new ArrayList<Vec3f>();
            for(var face:joints.getAsJsonArray(jointName))for(var point:face.getAsJsonObject().getAsJsonArray("p")){
                if(!unique.add(point.toString()))continue;var coordinates=point.getAsJsonArray();
                vertices.add(new Vec3f(coordinates.get(0).getAsFloat(),coordinates.get(1).getAsFloat(),coordinates.get(2).getAsFloat()));
            }
            if(vertices.isEmpty())throw new IllegalStateException("Empty claw joint: "+jointName);
            points.put(jointName,vertices);
        }
        var animation=BlacklightEpicFightAnimations.CLAW_GROUNDSPIKE.get();var clip=animation.getAnimationClip();
        var times=new TreeSet<Float>();
        for(int frame=0;frame<=Math.ceil(clip.getClipTime()*60);frame++)times.add(Math.min(clip.getClipTime(),frame/60F));
        for(var track:clip.getJointTransforms().values())for(var frame:track.getKeyframes())times.add(frame.time());
        var rig=yesman.epicfight.gameasset.Armatures.BIPED.get();
        var frames=new JsonArray();var jointSummary=new JsonObject();double minimum=Double.POSITIVE_INFINITY;float closestTime=0;String closestJoint="";
        for(var entry:points.entrySet()){
            double handMinimum=Double.POSITIVE_INFINITY;float handClosest=0;int penetratingFrames=0;
            for(float time:times){
                var pose=animation.getRawPose(time);
                var head=rig.getBoundTransformFor(pose,rig.searchJointByName("Head"));
                var inverse=OpenMatrix4f.invert(head,null);
                var hand=rig.getBoundTransformFor(pose,rig.searchJointByName(entry.getKey()));
                var relative=OpenMatrix4f.mul(inverse,hand,null);
                double frameMinimum=Double.POSITIVE_INFINITY;int closestVertex=-1;
                for(int index=0;index<entry.getValue().size();index++){
                    var v=OpenMatrix4f.transform3v(relative,entry.getValue().get(index),null);
                    double x=Math.max(0,Math.abs(v.x)-.25),y=Math.max(0,Math.abs(v.y-.25)-.25),z=Math.max(0,Math.abs(v.z)-.25);
                    double distance=Math.sqrt(x*x+y*y+z*z);
                    if(distance<frameMinimum){frameMinimum=distance;closestVertex=index;}
                }
                if(frameMinimum==0)penetratingFrames++;
                if(frameMinimum<handMinimum){handMinimum=frameMinimum;handClosest=time;}
                if(frameMinimum<minimum){minimum=frameMinimum;closestTime=time;closestJoint=entry.getKey();}
                var sample=new JsonObject();sample.addProperty("joint",entry.getKey());sample.addProperty("time",time);
                sample.addProperty("clearance",frameMinimum);sample.addProperty("closestVertex",closestVertex);frames.add(sample);
            }
            var summary=new JsonObject();summary.addProperty("vertexCount",entry.getValue().size());summary.addProperty("minimumClearance",handMinimum);
            summary.addProperty("time",handClosest);summary.addProperty("penetratingSampleCount",penetratingFrames);jointSummary.add(entry.getKey(),summary);
        }
        var report=new JsonObject();report.addProperty("clip","blacklight:biped/combat/claw_groundspike");report.addProperty("model",model.toString());
        report.addProperty("clipSeconds",clip.getClipTime());report.addProperty("sampling","60 Hz plus every loaded keyframe; raw runtime clip");
        report.addProperty("headBounds","Head-local x/z [-0.25,0.25], y [0,0.5]; distances in model units");
        report.addProperty("minimumHandHeadClearance",minimum);report.addProperty("closestTime",closestTime);report.addProperty("closestJoint",closestJoint);
        report.addProperty("limitation","Vertex-to-head AABB clearance only; does not prove triangle clearance, contact quality or Prototype fidelity.");
        report.add("hands",jointSummary);report.add("samples",frames);
        Files.writeString(output.resolve("groundspike-clearance.json"),JSON.toJson(report));
    }
}
