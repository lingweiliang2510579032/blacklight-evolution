package com.codex.blacklight.client;

import com.codex.blacklight.combat.*;
import com.codex.blacklight.integration.epicfight.BlacklightEpicFightAnimations;
import com.codex.blacklight.network.ClawActionPayload;
import com.codex.blacklight.network.MovementInputPayload;
import com.codex.blacklight.power.PowerForm;
import com.codex.blacklight.registry.ModAttachments;
import com.codex.blacklight.registry.ModEffects;
import com.google.gson.*;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** R24 opt-in only: actual Special routing, entity displacement, one contact, recovery and a solid wall. */
@EventBusSubscriber(modid="blacklight",value=Dist.CLIENT)
public final class BladeSpecialPreview {
    private static final String PROPERTY="blacklight.test.bladeSpecialPreview",WORLD="Blacklight Consume QA r10";
    private static final String[] SCENES={"standing_special","sprinting_special","blocked_special"};
    private static final int ACTION_AT=80,CAPTURE_FROM=60,IDLE_TICKS=35;
    private static final int MAX_SCREENSHOT_WRITES=4;
    private static final double WALL_FRONT=21.0;
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static final Queue<JsonObject> serverFrames=new ConcurrentLinkedQueue<>(),renderedFrames=new ConcurrentLinkedQueue<>();
    private static final Queue<JsonObject> requests=new ConcurrentLinkedQueue<>(),damageEvents=new ConcurrentLinkedQueue<>(),healingEvents=new ConcurrentLinkedQueue<>();
    private static final Queue<JsonObject> interferenceInputs=new ConcurrentLinkedQueue<>();
    private static final Queue<String> skipped=new ConcurrentLinkedQueue<>();
    private static final Queue<JsonObject> skippedDetails=new ConcurrentLinkedQueue<>();
    private static final Map<Integer,JsonObject> targets=new ConcurrentHashMap<>();
    private static final Map<Integer,Float> healthBefore=new ConcurrentHashMap<>();
    private static final AtomicInteger written=new AtomicInteger();
    private static final AtomicInteger screenshotWrites=new AtomicInteger(),peakScreenshotWrites=new AtomicInteger();
    private static final AtomicReference<Throwable> serverFailure=new AtomicReference<>();
    private static final List<String> failures=new ArrayList<>();
    private static final Map<BlockPos,net.minecraft.world.level.block.state.BlockState> replacedWallBlocks=new HashMap<>();
    private static volatile int cameraId=-1,targetId=-1,qaPlayerId=-1,currentServerScene=-1,requestPlayerTick=-1;
    private static volatile boolean prepared,serverFinished;
    private static int scene=-1,phase,warmup,waiting,captureEnd,sceneTicks,saved;
    private static long runStarted;
    private static boolean done;
    private static JsonObject pending,verification;

    private static Path output(){
        String value=System.getProperty(PROPERTY);
        return value==null||value.isBlank()||value.equalsIgnoreCase("true")?Path.of("../tmp/blade-special-r24-runtime").toAbsolutePath().normalize():Path.of(value);
    }

    private static double targetDistance(int index){
        String common=System.getProperty(PROPERTY+".targetDistance","3.0");
        double distance=Double.parseDouble(index==1?System.getProperty(PROPERTY+".sprintTargetDistance",common):common);
        if(!Double.isFinite(distance)||distance<1||distance>12)throw new IllegalArgumentException("R24 target distance must be finite and within 1..12 blocks");
        return distance;
    }

    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        if(System.getProperty(PROPERTY)==null||done)return;
        try{runTick();}catch(Exception error){fail(error);}
    }

    private static void runTick()throws Exception{
        var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();
        if(mc.player==null||mc.level==null||server==null||++warmup<90)return;
        for(String other:List.of("blacklight.test.gameplayPreview","blacklight.test.clawReferencePreview","blacklight.test.bladeReferencePreview"))
            if(System.getProperty(other)!=null)throw new IllegalStateException("Only BladeSpecialPreview may be enabled");
        if(!server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().getFileName().toString().equals(WORLD))
            throw new IllegalStateException("R24 capture requires isolated "+WORLD+" save");
        if(serverFailure.get()!=null)throw new IllegalStateException("R24 server capture failed",serverFailure.get());
        var uuid=mc.player.getUUID();
        if(scene<0){
            Files.createDirectories(output());Files.deleteIfExists(output().resolve("capture-complete.txt"));Files.deleteIfExists(output().resolve("capture-failed.txt"));
            runStarted=System.currentTimeMillis();captureEnd=ACTION_AT+WeaponMove.BLADE_RUSH.duration+IDLE_TICKS;sceneTicks=captureEnd+8;
            mc.options.pauseOnLostFocus=false;mc.options.fov().set(55);mc.options.framerateLimit().set(60);
            mc.options.hideGui=true;mc.options.setCameraType(CameraType.FIRST_PERSON);mc.setScreen(null);
            scene=0;server.execute(()->serverTask(()->prepare(server,uuid)));return;
        }
        if(!prepared){if(++waiting>400)throw new IllegalStateException("R24 scene setup timed out");return;}
        if(scene>=SCENES.length){
            if(!serverFinished||pending!=null||screenshotWrites.get()!=0||written.get()<saved){if(++waiting>400)throw new IllegalStateException("R24 final capture writes timed out");return;}
            verify();writeManifest();finish();return;
        }
        var camera=mc.level.getEntity(cameraId);if(camera==null){if(++waiting>400)throw new IllegalStateException("R24 camera missing");return;}
        waiting=0;mc.setCameraEntity(camera);mc.options.hideGui=true;mc.setScreen(null);
        float cameraYaw=scene==1?45:90;
        camera.setYRot(cameraYaw);camera.yRotO=cameraYaw;camera.setXRot(7);camera.xRotO=7;camera.setYHeadRot(cameraYaw);
        if(camera instanceof LivingEntity living)living.yHeadRotO=cameraYaw;
        // Simulate vanilla KeyMapping input from the four-tick lead-in through most of recovery.
        // Release three ticks before recovery ends so its network packet arrives before movement resumes.
        // This is not physical keyboard input. Displacement excludes the pre-request lead-in.
        boolean heldSprintInput=scene==1&&phase>=ACTION_AT-4&&phase<ACTION_AT+WeaponMove.BLADE_RUSH.duration-3;
        mc.options.keyUp.setDown(heldSprintInput);mc.options.keySprint.setDown(heldSprintInput);
        mc.options.keyDown.setDown(false);mc.options.keyLeft.setDown(false);mc.options.keyRight.setDown(false);
        mc.options.keyJump.setDown(false);mc.options.keyShift.setDown(false);mc.options.keyAttack.setDown(false);mc.options.keyUse.setDown(false);
        mc.player.setYRot(180);mc.player.setXRot(0);mc.player.setSprinting(heldSprintInput);
        // Share real RMB's immediate takeover; action dispatch remains server-side below.
        if(phase==ACTION_AT)ClientWeaponCombat.prepareSpecialMovement();
        if(scene==0&&(phase==ACTION_AT+3||phase==ACTION_AT+5)){
            boolean pressed=phase==ACTION_AT+3;
            var input=new MovementInputPayload(pressed,pressed,pressed?1:0,pressed?1:0,pressed,false);
            PacketDistributor.sendToServer(input);
            var sample=new JsonObject();sample.addProperty("scene",scene);sample.addProperty("phaseTick",phase);
            sample.addProperty("relativeTick",phase-ACTION_AT);sample.addProperty("clientGameTime",mc.level.getGameTime());
            sample.addProperty("jumpHeld",input.jumpHeld());sample.addProperty("dashPressed",input.dashPressed());
            sample.addProperty("forward",input.forward());sample.addProperty("sideways",input.sideways());
            sample.addProperty("sprinting",input.sprinting());sample.addProperty("detach",input.detach());interferenceInputs.add(sample);
        }
        // External camera mode disables LocalPlayer's normal position packets; forward the actual
        // simulated position, never teleport/lunge the actor from the harness while an action runs.
        mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot(
                mc.player.getX(),mc.player.getY(),mc.player.getZ(),180,0,mc.player.onGround()));
        final int currentScene=scene,currentPhase=phase;
        var clientInputPosition=mc.player.position();
        server.execute(()->serverTask(()->advance(server,uuid,currentScene,currentPhase,clientInputPosition)));
        if(phase>=CAPTURE_FROM&&phase<=captureEnd){
            if(pending!=null)skipFrame(pending,"next_tick_before_render");
            pending=new JsonObject();pending.addProperty("scene",scene);pending.addProperty("phaseTick",phase);
            pending.addProperty("requestedRelativeTick",phase-ACTION_AT);pending.addProperty("stage",stage(phase-ACTION_AT));
            pending.addProperty("wKeyMappingDown",mc.options.keyUp.isDown());pending.addProperty("sprintKeyMappingDown",mc.options.keySprint.isDown());
            pending.addProperty("file",String.format("%s_%04d_%s.png",SCENES[scene],phase-CAPTURE_FROM,stage(phase-ACTION_AT)));
        }
        if(++phase>=sceneTicks){phase=0;scene++;}
    }

    private static void serverTask(Runnable work){try{work.run();}catch(Throwable error){serverFailure.compareAndSet(null,error);}}

    private static void prepare(MinecraftServer server,UUID uuid){
        var level=server.overworld();var player=Objects.requireNonNull(server.getPlayerList().getPlayer(uuid));qaPlayerId=player.getId();
        if(level.getBlockState(new BlockPos(20,219,22)).isAir())throw new IllegalStateException("Existing QA platform is missing");
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);level.setDayTime(6000);level.setWeatherParameters(6000,0,false,false);
        for(var entity:level.getEntities(player,new AABB(12,219,10,30,232,28)))if(entity instanceof Mob)entity.discard();
        player.setGameMode(GameType.SURVIVAL);player.setInvulnerable(true);player.getInventory().clearContent();player.getFoodData().setFoodLevel(20);
        var state=player.getData(ModAttachments.PLAYER_STATE);state.infect();state.setPrototypeMode(true);state.unlock(PowerForm.BLADE);state.activate(PowerForm.BLADE);player.syncData(ModAttachments.PLAYER_STATE);
        var camera=level.getEntities(player,new AABB(23,219,14,29,226,26)).stream()
                .filter(entity->entity.getType()==EntityType.ARMOR_STAND&&entity.isInvisible()&&entity.isNoGravity()).findFirst().orElseGet(()->{
                    var created=Objects.requireNonNull(EntityType.ARMOR_STAND.create(level));created.setInvisible(true);created.setNoGravity(true);level.addFreshEntity(created);return created;
                });
        camera.moveTo(29,220.15,18.5,90,7);cameraId=camera.getId();prepared=true;
    }

    private static void advance(MinecraftServer server,UUID uuid,int index,int tick,net.minecraft.world.phys.Vec3 clientInputPosition){
        var player=Objects.requireNonNull(server.getPlayerList().getPlayer(uuid));var level=server.overworld();
        if(tick==0){
            currentServerScene=index;requestPlayerTick=-1;
            WeaponCombatSystem.cancel(player);player.getData(ModAttachments.CLAW_COMBAT).reset();player.getData(ModAttachments.MOVEMENT_STATE).reset();
            player.setData(ModAttachments.WEAPON_VISUAL,WeaponVisual.EMPTY);player.syncData(ModAttachments.WEAPON_VISUAL);
            player.teleportTo(20,220,22);player.setYRot(180);player.setXRot(0);player.setSprinting(false);player.setDeltaMovement(0,0,0);player.setOnGround(true);
            var patch=Objects.requireNonNull(yesman.epicfight.world.capabilities.EpicFightCapabilities.getServerPlayerPatch(player));
            patch.playAnimationSynchronized(yesman.epicfight.gameasset.Animations.BIPED_IDLE,0);
            for(var entity:level.getEntities(player,new AABB(12,219,8,30,232,29)))if(entity instanceof Mob)entity.discard();
            if(index==2)for(int x=17;x<=23;x++)for(int y=220;y<=224;y++){
                var pos=new BlockPos(x,y,20);replacedWallBlocks.putIfAbsent(pos,level.getBlockState(pos));level.setBlock(pos,Blocks.STONE.defaultBlockState(),3);
            }
            var target=Objects.requireNonNull(EntityType.VILLAGER.create(level));target.moveTo(20,220,index==2?19:22-targetDistance(index),0,0);
            target.setNoAi(true);target.setPersistenceRequired();level.addFreshEntity(target);targetId=target.getId();
            var data=new JsonObject();data.addProperty("scene",index);data.addProperty("id",targetId);data.addProperty("uuid",target.getUUID().toString());
            data.addProperty("initialHealth",target.getHealth());data.addProperty("type","minecraft:villager");data.addProperty("x",target.getX());data.addProperty("y",target.getY());data.addProperty("z",target.getZ());
            data.addProperty("noAi",true);data.addProperty("distanceFromInitialPlayer",22-target.getZ());data.addProperty("repositionedAfterSpawn",false);targets.put(targetId,data);
            Objects.requireNonNull(level.getEntity(cameraId)).moveTo(index==1?28:29,220.15,index==1?10:18.5,index==1?45:90,7);
        }
        if(tick==ACTION_AT){
            player.setSprinting(index==1);requestPlayerTick=player.tickCount;
            var request=new JsonObject();request.addProperty("scene",index);request.addProperty("intent","WEAPON_SPECIAL");
            request.addProperty("requestPlayerTick",requestPlayerTick);request.addProperty("serverTick",server.getTickCount());request.addProperty("gameTime",level.getGameTime());
            request.addProperty("clientRequestX",clientInputPosition.x);request.addProperty("clientRequestY",clientInputPosition.y);request.addProperty("clientRequestZ",clientInputPosition.z);
            addActor(request,player);ClawCombatSystem.handle(player,ClawActionPayload.Action.WEAPON_SPECIAL);
            addVisual(request,player.getData(ModAttachments.WEAPON_VISUAL),level.getGameTime());requests.add(request);
            System.out.println("BLADE_SPECIAL_REQUEST scene="+SCENES[index]+" selected="+player.getData(ModAttachments.WEAPON_VISUAL).spec());
        }
        var sample=new JsonObject();sample.addProperty("scene",index);sample.addProperty("phaseTick",tick);sample.addProperty("serverTick",server.getTickCount());sample.addProperty("gameTime",level.getGameTime());
        sample.addProperty("busy",WeaponCombatSystem.busy(player));sample.addProperty("clawAction",player.getData(ModAttachments.CLAW_COMBAT).action().name());
        var movement=player.getData(ModAttachments.MOVEMENT_STATE);sample.addProperty("movementChargeTicks",movement.chargeTicks());sample.addProperty("movementDashTicks",movement.dashTicks());sample.addProperty("movementMode",movement.mode().name());
        var animationPatch=yesman.epicfight.world.capabilities.EpicFightCapabilities.getServerPlayerPatch(player);
        if(animationPatch!=null)sample.addProperty("playingChargedJump",animationPatch.getAnimator().isPlaying(BlacklightEpicFightAnimations.CHARGED_JUMP));
        addActor(sample,player);addVisual(sample,player.getData(ModAttachments.WEAPON_VISUAL),level.getGameTime());addTarget(sample,level.getEntity(targetId));serverFrames.add(sample);
        if(index==SCENES.length-1&&tick==sceneTicks-1){
            // Restore only blocks replaced by this harness, after the entire wall scene was captured.
            for(var entry:replacedWallBlocks.entrySet())level.setBlock(entry.getKey(),entry.getValue(),3);replacedWallBlocks.clear();serverFinished=true;
        }
    }

    @SubscribeEvent public static void frame(RenderFrameEvent.Post event){
        if(pending==null||done)return;
        try{
            var mc=Minecraft.getInstance();var sample=pending;pending=null;
            // Screenshot.grab allocates a native image before queuing PNG compression on the IO pool.
            // Do not let slow writes retain an unbounded number of those images during capture.
            if(screenshotWrites.get()>=MAX_SCREENSHOT_WRITES){skipFrame(sample,"screenshot_write_backpressure");return;}
            sample.addProperty("clientGameTime",mc.level.getGameTime());sample.addProperty("nanoTime",System.nanoTime());
            addActor(sample,mc.player);addVisual(sample,mc.player.getData(ModAttachments.WEAPON_VISUAL),mc.level.getGameTime());addTarget(sample,mc.level.getEntity(targetId));
            var animation=BlacklightEpicFightAnimations.weapon(WeaponMove.BLADE_RUSH);
            var patch=yesman.epicfight.world.capabilities.EpicFightCapabilities.getEntityPatch(mc.player,yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch.class);
            if(patch!=null){sample.addProperty("playingSpecial",patch.getAnimator().isPlaying(animation));sample.addProperty("animationTime",patch.getAnimator().getPlayerFor(animation).getElapsedTime());sample.addProperty("playingChargedJump",patch.getAnimator().isPlaying(BlacklightEpicFightAnimations.CHARGED_JUMP));}
            int outstanding=screenshotWrites.incrementAndGet();peakScreenshotWrites.accumulateAndGet(outstanding,Math::max);
            var released=new AtomicBoolean();
            try{
                saved++;renderedFrames.add(sample);
                Screenshot.grab(output().toFile(),sample.get("file").getAsString(),mc.getMainRenderTarget(),message->{
                    if(released.compareAndSet(false,true)){written.incrementAndGet();screenshotWrites.decrementAndGet();}
                });
            }catch(Exception error){if(released.compareAndSet(false,true))screenshotWrites.decrementAndGet();throw error;}
        }catch(Exception error){fail(error);}
    }

    private static void skipFrame(JsonObject sample,String reason){
        skipped.add(sample.get("file").getAsString());var detail=sample.deepCopy();
        detail.addProperty("reason",reason);detail.addProperty("outstandingScreenshotWrites",screenshotWrites.get());skippedDetails.add(detail);
    }

    private static void addActor(JsonObject sample,LivingEntity entity){
        sample.addProperty("x",entity.getX());sample.addProperty("y",entity.getY());sample.addProperty("z",entity.getZ());sample.addProperty("playerTick",entity.tickCount);
        sample.addProperty("vx",entity.getDeltaMovement().x);sample.addProperty("vy",entity.getDeltaMovement().y);sample.addProperty("vz",entity.getDeltaMovement().z);
        sample.addProperty("sprinting",entity.isSprinting());sample.addProperty("onGround",entity.onGround());sample.addProperty("yaw",entity.getYRot());
        sample.addProperty("bodyMinZ",entity.getBoundingBox().minZ);sample.addProperty("bodyMinX",entity.getBoundingBox().minX);sample.addProperty("bodyMaxX",entity.getBoundingBox().maxX);
    }

    private static void addVisual(JsonObject sample,WeaponVisual visual,long gameTime){
        var data=new JsonObject();data.addProperty("move",visual.move());data.addProperty("name",visual.spec()==null?"EMPTY":visual.spec().name());
        data.addProperty("started",visual.started());data.addProperty("elapsed",visual.spec()==null?-1:gameTime-visual.started());sample.add("weaponVisual",data);
    }

    private static void addTarget(JsonObject sample,Entity entity){
        var data=new JsonObject();data.addProperty("present",entity instanceof LivingEntity);data.addProperty("expectedId",targetId);
        if(entity instanceof LivingEntity target){
            data.addProperty("id",target.getId());data.addProperty("uuid",target.getUUID().toString());data.addProperty("health",target.getHealth());data.addProperty("maxHealth",target.getMaxHealth());
            data.addProperty("x",target.getX());data.addProperty("y",target.getY());data.addProperty("z",target.getZ());data.addProperty("hurtTime",target.hurtTime);
            data.addProperty("blacklightInfection",target.hasEffect(ModEffects.BLACKLIGHT_INFECTION));
        }sample.add("target",data);
    }

    @SubscribeEvent public static void damageBefore(LivingDamageEvent.Pre event){
        if(System.getProperty(PROPERTY)==null||done||event.getEntity().level().isClientSide()||!targets.containsKey(event.getEntity().getId()))return;
        healthBefore.put(event.getEntity().getId(),event.getEntity().getHealth());
    }

    @SubscribeEvent public static void damageAfter(LivingDamageEvent.Post event){
        var target=event.getEntity();if(System.getProperty(PROPERTY)==null||done||target.level().isClientSide()||!targets.containsKey(target.getId()))return;
        var source=event.getSource().getEntity();var sample=new JsonObject();var before=healthBefore.remove(target.getId());
        sample.addProperty("scene",targets.get(target.getId()).get("scene").getAsInt());sample.addProperty("targetId",target.getId());sample.addProperty("targetUuid",target.getUUID().toString());
        sample.addProperty("sourceEntityId",source==null?-1:source.getId());sample.addProperty("sourceUuid",source==null?"":source.getUUID().toString());
        sample.addProperty("gameTime",target.level().getGameTime());sample.addProperty("serverTick",target.getServer().getTickCount());sample.addProperty("damageType",event.getSource().getMsgId());
        sample.addProperty("healthBeforeObserved",before!=null);sample.addProperty("healthBefore",before==null?target.getHealth()+event.getNewDamage():before);
        sample.addProperty("healthAfter",target.getHealth());sample.addProperty("originalDamage",event.getOriginalDamage());sample.addProperty("appliedDamage",event.getNewDamage());
        sample.addProperty("elapsedPlayerTicks",source==null||requestPlayerTick<0?-1:source.tickCount-requestPlayerTick);
        if(source instanceof ServerPlayer player)addVisual(sample,player.getData(ModAttachments.WEAPON_VISUAL),player.level().getGameTime());damageEvents.add(sample);
    }

    @SubscribeEvent(priority=EventPriority.LOWEST,receiveCanceled=true) public static void healing(LivingHealEvent event){
        var target=event.getEntity();if(System.getProperty(PROPERTY)==null||done||target.level().isClientSide()||!targets.containsKey(target.getId()))return;
        var sample=new JsonObject();sample.addProperty("scene",targets.get(target.getId()).get("scene").getAsInt());sample.addProperty("targetId",target.getId());
        sample.addProperty("gameTime",target.level().getGameTime());sample.addProperty("healthBefore",target.getHealth());sample.addProperty("maxHealth",target.getMaxHealth());
        sample.addProperty("amount",event.getAmount());sample.addProperty("canceled",event.isCanceled());sample.addProperty("blacklightInfection",target.hasEffect(ModEffects.BLACKLIGHT_INFECTION));healingEvents.add(sample);
    }

    private static String stage(int elapsed){
        var move=WeaponMove.BLADE_RUSH;return elapsed<0?"idle_before":elapsed<move.start?"windup":move.contactIndex(elapsed)>=0?"contact":elapsed<move.end?"between_contacts":elapsed<move.duration?"follow_through":"idle_after";
    }

    private static void verify()throws Exception{
        var move=WeaponMove.BLADE_RUSH;verification=new JsonObject();var cases=new JsonArray();verification.add("scenes",cases);
        long pressedInputs=interferenceInputs.stream().filter(row->row.get("scene").getAsInt()==0&&row.get("relativeTick").getAsInt()==3&&row.get("jumpHeld").getAsBoolean()&&row.get("dashPressed").getAsBoolean()).count();
        long releasedInputs=interferenceInputs.stream().filter(row->row.get("scene").getAsInt()==0&&row.get("relativeTick").getAsInt()==5&&!row.get("jumpHeld").getAsBoolean()&&!row.get("dashPressed").getAsBoolean()).count();
        verification.addProperty("interferencePressPackets",pressedInputs);verification.addProperty("interferenceReleasePackets",releasedInputs);
        if(interferenceInputs.size()!=2||pressedInputs!=1||releasedInputs!=1)failures.add("Expected one jump/dash interference packet at +3 and one release at +5");
        for(int index=0;index<SCENES.length;index++){
            final int current=index;var matching=requests.stream().filter(row->row.get("scene").getAsInt()==current).toList();
            if(matching.size()!=1){failures.add("Expected one Special request in scene "+index+": "+matching.size());continue;}
            var request=matching.getFirst();
            if(request.getAsJsonObject("weaponVisual").get("move").getAsInt()!=move.ordinal())failures.add("Special selected wrong move in scene "+index+": "+request);
            if(request.get("sprinting").getAsBoolean()!=(index==1))failures.add("Wrong stationary/sprinting precondition in scene "+index);
            // Measure from the actor position at the actual client input. The direct server task
            // can run before that frame's PosRot; counting that already-completed pre-input travel
            // as action movement would add a false extra sprint step. Keep both positions in the log.
            double originX=request.get("clientRequestX").getAsDouble(),originZ=request.get("clientRequestZ").getAsDouble();
            int lastActive=-1,recovered=0,damageCount=0;double damage=0,minimumBodyZ=Double.POSITIVE_INFINITY,lastRenderedAnimationTime=-1;
            boolean windup=false,contact=false,recovery=false,clientIdle=false,played=false;JsonObject serverLast=null,clientLast=null;
            for(var row:serverFrames){
                if(row.get("scene").getAsInt()!=index)continue;var visual=row.getAsJsonObject("weaponVisual");int id=visual.get("move").getAsInt();
                int relativeTick=row.get("phaseTick").getAsInt()-ACTION_AT;
                if(relativeTick>=0&&relativeTick<move.duration){
                    if(row.has("playingChargedJump")&&row.get("playingChargedJump").getAsBoolean())failures.add("Server CHARGED_JUMP overrode Special in scene "+index+" at "+relativeTick);
                    if(row.get("movementChargeTicks").getAsInt()>0||row.get("movementDashTicks").getAsInt()>0)failures.add("Server jump/air-dash movement state remained active during Special in scene "+index+" at "+relativeTick);
                }
                if(id==move.ordinal())lastActive=Math.max(lastActive,visual.get("elapsed").getAsInt());
                else if(id!=-1)failures.add("Unexpected active weapon in scene "+index+": "+id);
                if(row.get("phaseTick").getAsInt()>ACTION_AT+move.duration&&id==-1&&!row.get("busy").getAsBoolean()&&row.get("clawAction").getAsString().equals("IDLE")){recovered++;serverLast=row;}
                minimumBodyZ=Math.min(minimumBodyZ,row.get("bodyMinZ").getAsDouble());
            }
            for(var row:renderedFrames){
                if(row.get("scene").getAsInt()!=index)continue;var visual=row.getAsJsonObject("weaponVisual");int id=visual.get("move").getAsInt(),t=visual.get("elapsed").getAsInt();
                int relativeTick=row.get("phaseTick").getAsInt()-ACTION_AT;
                if(relativeTick>=0&&relativeTick<move.duration&&row.has("playingChargedJump")&&row.get("playingChargedJump").getAsBoolean())
                    failures.add("Rendered CHARGED_JUMP overrode Special in scene "+index+" at "+relativeTick);
                if(id==move.ordinal()){windup|=t>=0&&t<move.start;contact|=move.contactIndex(t)>=0;recovery|=t>=move.end;}
                if(row.has("playingSpecial")&&row.get("playingSpecial").getAsBoolean()){
                    played=true;if(row.has("animationTime"))lastRenderedAnimationTime=Math.max(lastRenderedAnimationTime,row.get("animationTime").getAsDouble());
                }
                if(row.get("phaseTick").getAsInt()>ACTION_AT+move.duration+10&&id==-1){clientIdle=true;clientLast=row;}
                if(index==2&&row.get("bodyMinZ").getAsDouble()<WALL_FRONT-.01)failures.add("Client actor crossed wall in frame "+row.get("file").getAsString());
            }
            for(var event:damageEvents){
                if(event.get("scene").getAsInt()!=index||event.get("appliedDamage").getAsDouble()<=.0001)continue;
                damageCount++;damage+=event.get("appliedDamage").getAsDouble();int t=event.get("elapsedPlayerTicks").getAsInt();
                if(event.get("sourceEntityId").getAsInt()!=qaPlayerId||move.contactIndex(t)<0||!event.has("weaponVisual")
                        ||event.getAsJsonObject("weaponVisual").get("move").getAsInt()!=move.ordinal())failures.add("Unexpected source/contact time for damage: "+event);
                if(!event.get("healthBeforeObserved").getAsBoolean()||Math.abs(event.get("healthBefore").getAsDouble()-event.get("healthAfter").getAsDouble()-move.damage)>.0001)
                    failures.add("Actual health reduction differs from move.damage: "+event);
            }
            int expectedHits=index==2?0:1;
            if(damageCount!=expectedHits||Math.abs(damage-expectedHits*move.damage)>.0001)failures.add("Scene "+index+" expected "+expectedHits+" damage event(s), observed="+damageCount+", damage="+damage);
            if(lastActive<move.duration-2||recovered<10||!windup||!contact||!recovery||!clientIdle||!played)
                failures.add("Incomplete Special playback/recovery in scene "+index+": activeEnd="+lastActive+", recovered="+recovered+", rendered="+windup+"/"+contact+"/"+recovery+"/"+clientIdle+", playing="+played);
            double clipEnd=BlacklightEpicFightAnimations.weapon(move).get().getAnimationClip().getClipTime();
            if(lastRenderedAnimationTime<clipEnd-.12)failures.add("Rendered Special ended before clip recovery in scene "+index+": last="+lastRenderedAnimationTime+", clipEnd="+clipEnd);
            var result=new JsonObject();result.addProperty("scene",index);result.addProperty("name",SCENES[index]);result.addProperty("damageEvents",damageCount);result.addProperty("damage",damage);
            result.addProperty("lastRenderedAnimationTime",lastRenderedAnimationTime);result.addProperty("clipEnd",clipEnd);
            result.addProperty("minimumServerBodyZ",minimumBodyZ);result.addProperty("sprintLeadInForward",22-originZ);
            result.addProperty("serverPositionLagAtRequest",Math.hypot(originX-request.get("x").getAsDouble(),originZ-request.get("z").getAsDouble()));
            if(serverLast==null||clientLast==null)failures.add("Missing final entity positions in scene "+index);
            else{
                double sx=serverLast.get("x").getAsDouble()-originX,sz=serverLast.get("z").getAsDouble()-originZ;
                double cx=clientLast.get("x").getAsDouble()-originX,cz=clientLast.get("z").getAsDouble()-originZ;
                double disagreement=Math.hypot(sx-cx,sz-cz);result.addProperty("serverDx",sx);result.addProperty("serverDz",sz);result.addProperty("clientDx",cx);result.addProperty("clientDz",cz);result.addProperty("clientServerError",disagreement);
                if(disagreement>.15)failures.add("Client/server Special displacement differs in scene "+index+": "+disagreement);
                if(index<2&&(-sz<.4||-cz<.4))failures.add("Ground Special did not move both actors forward at least 0.4 blocks in scene "+index);
                if(index==2&&(minimumBodyZ<WALL_FRONT-.01||-sz>.72||-cz>.72))failures.add("Special crossed/overran wall: "+result);
            }
            if(index==1&&22-originZ<.03)failures.add("Sprint scene lacked a real movement lead-in before the Special request");
            cases.add(result);
        }
        JsonObject standing=null,sprinting=null;
        for(var element:cases){var result=element.getAsJsonObject();if(result.get("scene").getAsInt()==0)standing=result;else if(result.get("scene").getAsInt()==1)sprinting=result;}
        if(standing!=null&&sprinting!=null&&standing.has("serverDx")&&sprinting.has("serverDx")){
            double serverDifference=Math.hypot(standing.get("serverDx").getAsDouble()-sprinting.get("serverDx").getAsDouble(),standing.get("serverDz").getAsDouble()-sprinting.get("serverDz").getAsDouble());
            double clientDifference=Math.hypot(standing.get("clientDx").getAsDouble()-sprinting.get("clientDx").getAsDouble(),standing.get("clientDz").getAsDouble()-sprinting.get("clientDz").getAsDouble());
            verification.addProperty("standingSprintServerDisplacementDifference",serverDifference);verification.addProperty("standingSprintClientDisplacementDifference",clientDifference);
            if(serverDifference>=.15||clientDifference>=.15)failures.add("Held W/Sprint changed post-request Special displacement by >=0.15 blocks: server="+serverDifference+", client="+clientDifference);
        }else failures.add("Missing standing/sprinting displacement comparison");
        for(var row:renderedFrames){var file=output().resolve("screenshots").resolve(row.get("file").getAsString());
            if(!Files.isRegularFile(file)||Files.size(file)==0||Files.getLastModifiedTime(file).toMillis()<runStarted-2000)failures.add("Missing/stale screenshot: "+file.getFileName());}
        if(saved==0)failures.add("No screenshots captured");
    }

    private static void writeManifest()throws Exception{
        Files.createDirectories(output());var move=WeaponMove.BLADE_RUSH;var animation=BlacklightEpicFightAnimations.weapon(move).get();var manifest=new JsonObject();
        manifest.addProperty("status",failures.isEmpty()?"passed":"failed");manifest.addProperty("save",WORLD);manifest.addProperty("runStartedEpochMillis",runStarted);
        manifest.addProperty("intent","ClientWeaponCombat.prepareSpecialMovement() then ClawCombatSystem.handle(player, WEAPON_SPECIAL), one request per scene; shared real-RMB movement prediction, direct server action dispatch");manifest.addProperty("expectedMove",move.name());
        manifest.addProperty("durationTicks",move.duration);manifest.addProperty("contactStartTick",move.start);manifest.addProperty("contactEndTickExclusive",move.end);manifest.addProperty("damagePerContact",move.damage);
        var contactTicks=new JsonArray();for(int tick=0;tick<move.duration;tick++)if(move.contactIndex(tick)>=0)contactTicks.add(tick);manifest.add("actualContactTicks",contactTicks);
        manifest.addProperty("localMotionLoaded",LocalWeaponMotion.find(move.id).isPresent());manifest.addProperty("animationClass",animation.getClass().getName());manifest.addProperty("clipSeconds",animation.getAnimationClip().getClipTime());
        manifest.addProperty("requestedFps",20);manifest.addProperty("captureStartTick",CAPTURE_FROM);manifest.addProperty("requestPhaseTick",ACTION_AT);manifest.addProperty("captureEndTick",captureEnd);manifest.addProperty("sceneTicks",sceneTicks);
        manifest.addProperty("savedFrames",saved);manifest.addProperty("writtenCallbacks",written.get());manifest.add("skippedFrames",JSON.toJsonTree(skipped));
        manifest.addProperty("maxOutstandingScreenshotWrites",MAX_SCREENSHOT_WRITES);manifest.addProperty("peakOutstandingScreenshotWrites",peakScreenshotWrites.get());
        manifest.addProperty("remainingScreenshotWrites",screenshotWrites.get());manifest.add("skippedFrameDetails",JSON.toJsonTree(skippedDetails));
        manifest.addProperty("camera","Standing/wall side view: (29,220.15,18.5), yaw 90; sprint front three-quarter: (28,220.15,10), yaw 45; pitch 7, fov 55. PosRot sends actual client entity position while the external camera is active.");
        manifest.addProperty("wall","Scene 2: stone x=17..23, y=220..224, z=20, front face z=21. Actor starts (20,220,22). Target behind wall at (20,220,19). Original block states restored after capture.");
        manifest.addProperty("damageAudit","All target damage events retained, not just attacks from the player. Open scenes require one actual move.damage health reduction where move.contactIndex(elapsed)>=0; wall scene zero. One hit per target is the current Mod policy, not a claim about original-game deduplication across native dash windows. Infection healing is separately recorded and never subtracted from damage totals.");
        manifest.addProperty("targetDistanceProperty",PROPERTY+".targetDistance (default 3 blocks from initial player position); optional "+PROPERTY+".sprintTargetDistance overrides scene 1 only. Wall target remains behind the wall.");
        manifest.addProperty("displacementBaseline","Actual client position when the shared RMB prediction is called, preserved as clientRequestX/Y/Z; authoritative request x/y/z also retained. The server-direct task can precede that frame's PosRot. Already-completed pre-input travel is not counted as Special displacement. No test teleport or velocity is added during the action.");
        manifest.addProperty("inputGuardAudit","Scene 1 simulates vanilla W/Sprint KeyMapping input from request -4 through duration -4, releasing three ticks before recovery ends so the release packet arrives before normal movement resumes; this is not physical keyboard input. Standing/sprinting post-request displacement must differ by less than 0.15 blocks on both client and server. Scene 0 sends MovementInputPayload(true,true,1,1,true,false) at request +3 and the released payload at +5. Server movement state and client/server CHARGED_JUMP playback are checked during the full action.");
        manifest.addProperty("limitation","Tests routing, displacement, damage, wall collision and complete rendered recovery. No duplicated full blade-vertex collision audit; this report alone does not certify blade/head clearance or original-game visual fidelity. Input simulation covers the vanilla KeyMapping path and explicit movement network packets; displacement starts at the actual request position, excluding the four-tick sprint lead-in.");
        manifest.add("interferenceInputs",JSON.toJsonTree(interferenceInputs));
        manifest.add("requests",JSON.toJsonTree(requests));manifest.add("targets",JSON.toJsonTree(targets.values()));manifest.add("damageEvents",JSON.toJsonTree(damageEvents));manifest.add("healingEvents",JSON.toJsonTree(healingEvents));
        manifest.add("serverObservations",JSON.toJsonTree(serverFrames));manifest.add("renderedFrames",JSON.toJsonTree(renderedFrames));manifest.add("failures",JSON.toJsonTree(failures));if(verification!=null)manifest.add("verification",verification);
        Files.writeString(output().resolve("capture-manifest.json"),JSON.toJson(manifest));
    }

    private static void finish()throws Exception{
        Files.writeString(output().resolve(failures.isEmpty()?"capture-complete.txt":"capture-failed.txt"),failures.isEmpty()
                ?"PASS: actual standing/sprinting WEAPON_SPECIAL, BLADE_RUSH displacement, one contact each, blocked wall and full recovery. "+saved+" screenshots; inspect manifest and images for visual quality."
                :String.join(System.lineSeparator(),failures));
        done=true;var mc=Minecraft.getInstance();mc.options.keyUp.setDown(false);mc.options.keySprint.setDown(false);mc.setCameraEntity(mc.player);mc.options.hideGui=false;mc.stop();
    }

    private static void fail(Exception error){
        if(done)return;failures.add(error.toString());error.printStackTrace();
        var server=Minecraft.getInstance().getSingleplayerServer();
        if(server!=null)server.execute(()->{for(var entry:replacedWallBlocks.entrySet())server.overworld().setBlock(entry.getKey(),entry.getValue(),3);replacedWallBlocks.clear();});
        try{writeManifest();finish();}catch(Exception writeError){writeError.printStackTrace();done=true;Minecraft.getInstance().stop();}
    }
}
