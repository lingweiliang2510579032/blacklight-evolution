package com.codex.blacklight.client;

import com.codex.blacklight.combat.*;
import com.codex.blacklight.integration.epicfight.BlacklightEpicFightAnimations;
import com.codex.blacklight.power.PowerForm;
import com.codex.blacklight.registry.ModAttachments;
import com.codex.blacklight.registry.ModEffects;
import com.google.gson.*;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Real single-press blade capture, explicitly enabled in the existing isolated QA save only. */
@EventBusSubscriber(modid="blacklight",value=Dist.CLIENT)
public final class BladeReferencePreview {
    private static final String PROPERTY="blacklight.test.bladeReferencePreview",WORLD="Blacklight Consume QA r10";
    private static final int ACTION_AT=80,CAPTURE_FROM=60,IDLE_TICKS=30;
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static final Queue<JsonObject> observations=new ConcurrentLinkedQueue<>(),rendered=new ConcurrentLinkedQueue<>();
    private static final Queue<JsonObject> damageEvents=new ConcurrentLinkedQueue<>();
    private static final Queue<JsonObject> healingEvents=new ConcurrentLinkedQueue<>(),healthChanges=new ConcurrentLinkedQueue<>();
    private static final Queue<String> skipped=new ConcurrentLinkedQueue<>();
    private static final AtomicInteger written=new AtomicInteger();
    private static final AtomicReference<Throwable> serverFailure=new AtomicReference<>();
    private static final List<String> failures=new ArrayList<>();
    private static volatile int cameraId=-1,targetId=-1,qaPlayerId=-1;
    private static volatile JsonObject targetMetadata;
    // Written/read by the integrated server thread; damage events use the combat system's player tick clock.
    private static int requestPlayerTick=-1;
    private static float targetHealthBeforeDamage=Float.NaN;
    private static volatile boolean prepared,serverFinished;
    private static int warmup,scene=-1,phase,captureEnd,sceneTicks,saved,waiting;
    private static long startedMillis;
    private static boolean done;
    private static JsonObject pending,audit,healthSummary,movementSummary;

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        String destination=System.getProperty(PROPERTY);
        if(destination==null||done)return;
        try { tickCapture(Path.of(destination)); }
        catch(Exception error) { fail(Path.of(destination),error); }
    }

    private static void tickCapture(Path output)throws Exception {
        var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();
        if(mc.player==null||mc.level==null||server==null||++warmup<90)return;
        if(System.getProperty("blacklight.test.gameplayPreview")!=null||System.getProperty("blacklight.test.clawReferencePreview")!=null)
            throw new IllegalStateException("Only one visual capture harness may be enabled");
        if(!server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().getFileName().toString().equals(WORLD))
            throw new IllegalStateException("BladeReferencePreview requires the isolated "+WORLD+" save");
        if(serverFailure.get()!=null)throw new IllegalStateException("Server capture failed",serverFailure.get());
        var uuid=mc.player.getUUID();var move=WeaponMove.BLADE_CUT;
        if(scene<0){
            startedMillis=System.currentTimeMillis();
            Files.createDirectories(output);
            // Each run owns these two markers; screenshots/manifest include their exact current-run inventory.
            Files.deleteIfExists(output.resolve("capture-complete.txt"));Files.deleteIfExists(output.resolve("capture-failed.txt"));
            audit=auditBlade(output);
            if(audit.get("unexpectedPenetratingSampleCount").getAsInt()>0)
                failures.add("Loaded blade clip intersects the head or enters chest/waist beyond the documented mount/surface tolerance; inspect blade-clearance.json");
            captureEnd=ACTION_AT+move.duration+IDLE_TICKS;sceneTicks=captureEnd+8;
            mc.options.pauseOnLostFocus=false;mc.options.fov().set(55);mc.options.framerateLimit().set(60);
            mc.options.hideGui=true;mc.options.setCameraType(CameraType.FIRST_PERSON);mc.setScreen(null);
            scene=0;server.execute(()->serverTask(()->prepare(server,uuid)));return;
        }
        if(!prepared){if(++waiting>400)throw new IllegalStateException("Timed out preparing QA scene");return;}
        if(scene>=2){
            if(!serverFinished||pending!=null||written.get()<saved){
                if(++waiting>400)throw new IllegalStateException("Timed out awaiting final server tick or screenshot writes");return;
            }
            verifyCapture(output);writeManifest(output);finish(output);return;
        }
        var camera=mc.level.getEntity(cameraId);
        if(camera==null){if(++waiting>400)throw new IllegalStateException("QA camera did not reach client");return;}
        waiting=0;mc.setCameraEntity(camera);mc.setScreen(null);mc.options.hideGui=true;
        float yaw=scene==0?90F:39.8F;
        camera.setYRot(yaw);camera.yRotO=yaw;camera.setXRot(7);camera.xRotO=7;camera.setYHeadRot(yaw);
        if(camera instanceof LivingEntity living)living.yHeadRotO=yaw;
        // Avoid stale keys from the menu affecting the single server-owned action or its displacement.
        mc.options.keyUp.setDown(false);mc.options.keyDown.setDown(false);mc.options.keyLeft.setDown(false);mc.options.keyRight.setDown(false);
        mc.options.keyJump.setDown(false);mc.options.keySprint.setDown(false);mc.options.keyShift.setDown(false);
        mc.options.keyAttack.setDown(false);mc.options.keyUse.setDown(false);
        mc.player.setYRot(180);mc.player.setXRot(0);mc.player.setSprinting(false);
        // An external camera suppresses LocalPlayer's normal position packets. Reproduce normal
        // vanilla movement synchronization for this QA camera without moving the actor artificially.
        mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot(
                mc.player.getX(),mc.player.getY(),mc.player.getZ(),180,0,mc.player.onGround()));
        final int currentScene=scene,currentPhase=phase;
        server.execute(()->serverTask(()->advance(server,uuid,currentScene,currentPhase)));
        if(phase>=CAPTURE_FROM&&phase<=captureEnd){
            if(pending!=null)skipped.add(pending.get("file").getAsString());
            pending=new JsonObject();pending.addProperty("scene",scene);pending.addProperty("phaseTick",phase);
            pending.addProperty("requestedRelativeTick",phase-ACTION_AT);pending.addProperty("stage",stage(phase-ACTION_AT));
            pending.addProperty("file",String.format("blade_cut_%s_%04d_%s.png",scene==0?"side":"front_three_quarter",phase-CAPTURE_FROM,stage(phase-ACTION_AT)));
        }
        if(++phase>=sceneTicks){phase=0;scene++;}
    }

    private static void serverTask(Runnable operation){
        try{operation.run();}catch(Throwable error){serverFailure.compareAndSet(null,error);}
    }

    private static void prepare(MinecraftServer server,UUID uuid){
        var level=server.overworld();var player=Objects.requireNonNull(server.getPlayerList().getPlayer(uuid));
        qaPlayerId=player.getId();
        // Reuse the quartz platform created by GameplayPreview/ClawReferencePreview; never rebuild a world here.
        if(level.getBlockState(new BlockPos(20,219,22)).isAir())
            throw new IllegalStateException("Existing QA platform is missing at 20,219,22");
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
        level.setDayTime(6000);level.setWeatherParameters(6000,0,false,false);
        for(var entity:level.getEntities(player,new AABB(10,219,10,32,233,30)))
            if(entity instanceof Mob)entity.discard();
        player.setGameMode(GameType.SURVIVAL);player.setInvulnerable(true);player.getInventory().clearContent();
        var state=player.getData(ModAttachments.PLAYER_STATE);state.infect();state.setPrototypeMode(true);state.unlock(PowerForm.BLADE);state.activate(PowerForm.BLADE);player.syncData(ModAttachments.PLAYER_STATE);
        // Reuse an invisible QA camera when available; it has no hit box contact with the actor.
        var existing=level.getEntities(player,new AABB(23,219,14,29,226,26)).stream()
                .filter(entity->entity.getType()==EntityType.ARMOR_STAND&&entity.isInvisible()&&entity.isNoGravity()).findFirst();
        var camera=existing.orElseGet(()->{
            var created=Objects.requireNonNull(EntityType.ARMOR_STAND.create(level));
            created.setInvisible(true);created.setNoGravity(true);level.addFreshEntity(created);return created;
        });
        camera.moveTo(27,220.1,22,90,7);cameraId=camera.getId();prepared=true;
    }

    private static void advance(MinecraftServer server,UUID uuid,int sceneIndex,int tick){
        var player=Objects.requireNonNull(server.getPlayerList().getPlayer(uuid));
        if(tick==0){
            WeaponCombatSystem.cancel(player);player.getData(ModAttachments.CLAW_COMBAT).reset();player.getData(ModAttachments.MOVEMENT_STATE).reset();
            player.setData(ModAttachments.WEAPON_VISUAL,WeaponVisual.EMPTY);player.syncData(ModAttachments.WEAPON_VISUAL);
            player.teleportTo(20,220,22);player.setYRot(180);player.setXRot(0);player.setSprinting(false);player.setDeltaMovement(0,0,0);player.setOnGround(true);
            var patch=yesman.epicfight.world.capabilities.EpicFightCapabilities.getServerPlayerPatch(player);
            if(patch==null)throw new IllegalStateException("Epic Fight server player patch is missing");
            patch.playAnimationSynchronized(yesman.epicfight.gameasset.Animations.BIPED_IDLE,0);
            var camera=Objects.requireNonNull(server.overworld().getEntity(cameraId));
            camera.moveTo(sceneIndex==0?27:25,sceneIndex==0?220.1:220.2,sceneIndex==0?22:16,sceneIndex==0?90F:39.8F,7F);
            requestPlayerTick=-1;
            if(sceneIndex==1){
                var target=Objects.requireNonNull(EntityType.VILLAGER.create(server.overworld()));
                target.moveTo(20,220,20,0,0);target.setNoAi(true);target.setPersistenceRequired();
                server.overworld().addFreshEntity(target);targetId=target.getId();targetHealthBeforeDamage=Float.NaN;
                var metadata=new JsonObject();metadata.addProperty("scene",sceneIndex);metadata.addProperty("id",targetId);
                metadata.addProperty("uuid",target.getUUID().toString());metadata.addProperty("type","minecraft:villager");
                metadata.addProperty("initialHealth",target.getHealth());metadata.addProperty("maxHealth",target.getMaxHealth());
                metadata.addProperty("x",target.getX());metadata.addProperty("y",target.getY());metadata.addProperty("z",target.getZ());
                metadata.addProperty("noAi",true);metadata.addProperty("repositionedAfterSpawn",false);targetMetadata=metadata;
            }
        }
        if(tick==ACTION_AT){
            requestPlayerTick=player.tickCount;
            WeaponCombatSystem.request(player,false);
            if(player.getData(ModAttachments.WEAPON_VISUAL).spec()!=WeaponMove.BLADE_CUT)
                throw new IllegalStateException("Single request did not start BLADE_CUT in scene "+sceneIndex);
            System.out.println("BLADE_REFERENCE_REQUEST scene="+sceneIndex+" serverTick="+server.getTickCount()+" duration="+WeaponMove.BLADE_CUT.duration);
        }
        var sample=new JsonObject();sample.addProperty("scene",sceneIndex);sample.addProperty("phaseTick",tick);
        sample.addProperty("request",tick==ACTION_AT);sample.addProperty("serverTick",server.getTickCount());sample.addProperty("playerTick",player.tickCount);
        sample.addProperty("gameTime",player.level().getGameTime());sample.addProperty("busy",WeaponCombatSystem.busy(player));
        sample.addProperty("clawAction",player.getData(ModAttachments.CLAW_COMBAT).action().name());
        sample.addProperty("queuedAttack",player.getData(ModAttachments.CLAW_COMBAT).queuedAttack());
        addPlayer(sample,player);addVisual(sample,player.getData(ModAttachments.WEAPON_VISUAL),player.level().getGameTime());
        addTarget(sample,targetId<0?null:server.overworld().getEntity(targetId));observations.add(sample);
        if(sceneIndex==1&&tick==sceneTicks-1)serverFinished=true;
    }

    @SubscribeEvent public static void frame(RenderFrameEvent.Post event){
        if(pending==null||done)return;
        try{
            var mc=Minecraft.getInstance();var sample=pending;pending=null;
            sample.addProperty("clientGameTime",mc.level.getGameTime());sample.addProperty("nanoTime",System.nanoTime());
            addPlayer(sample,mc.player);addVisual(sample,mc.player.getData(ModAttachments.WEAPON_VISUAL),mc.level.getGameTime());
            addTarget(sample,targetId<0?null:mc.level.getEntity(targetId));
            var animation=BlacklightEpicFightAnimations.weapon(WeaponMove.BLADE_CUT);
            var patch=yesman.epicfight.world.capabilities.EpicFightCapabilities.getEntityPatch(mc.player,
                    yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch.class);
            if(patch!=null){
                sample.addProperty("playingBlade",patch.getAnimator().isPlaying(animation));
                sample.addProperty("animationTime",patch.getAnimator().getPlayerFor(animation).getElapsedTime());
            }
            saved++;rendered.add(sample);
            Screenshot.grab(new java.io.File(System.getProperty(PROPERTY)),sample.get("file").getAsString(),mc.getMainRenderTarget(),message->written.incrementAndGet());
        }catch(Exception error){fail(Path.of(System.getProperty(PROPERTY)),error);}
    }

    private static void addPlayer(JsonObject sample,LivingEntity player){
        sample.addProperty("x",player.getX());sample.addProperty("y",player.getY());sample.addProperty("z",player.getZ());
        sample.addProperty("dxFromStart",player.getX()-20);sample.addProperty("dyFromStart",player.getY()-220);sample.addProperty("dzFromStart",player.getZ()-22);
        sample.addProperty("velocityX",player.getDeltaMovement().x);sample.addProperty("velocityY",player.getDeltaMovement().y);sample.addProperty("velocityZ",player.getDeltaMovement().z);
        sample.addProperty("yaw",player.getYRot());sample.addProperty("bodyYaw",player.yBodyRot);sample.addProperty("onGround",player.onGround());
    }

    private static void addVisual(JsonObject sample,WeaponVisual visual,long gameTime){
        var value=new JsonObject();value.addProperty("move",visual.move());value.addProperty("name",visual.spec()==null?"EMPTY":visual.spec().name());
        value.addProperty("started",visual.started());value.addProperty("yaw",visual.yaw());value.addProperty("elapsed",visual.spec()==null?-1:gameTime-visual.started());
        sample.add("weaponVisual",value);
    }

    private static void addTarget(JsonObject sample,Entity entity){
        var value=new JsonObject();value.addProperty("expectedId",targetId);value.addProperty("present",entity instanceof LivingEntity);
        if(entity instanceof LivingEntity target){
            value.addProperty("id",target.getId());value.addProperty("uuid",target.getUUID().toString());
            value.addProperty("health",target.getHealth());value.addProperty("maxHealth",target.getMaxHealth());value.addProperty("alive",target.isAlive());
            value.addProperty("x",target.getX());value.addProperty("y",target.getY());value.addProperty("z",target.getZ());
            value.addProperty("hurtTime",target.hurtTime);value.addProperty("invulnerableTime",target.invulnerableTime);
            var infection=target.getEffect(ModEffects.BLACKLIGHT_INFECTION);value.addProperty("blacklightInfection",infection!=null);
            if(infection!=null){value.addProperty("infectionAmplifier",infection.getAmplifier());value.addProperty("infectionDuration",infection.getDuration());}
        }
        sample.add("target",value);
    }

    @SubscribeEvent public static void beforeTargetDamage(LivingDamageEvent.Pre event){
        if(System.getProperty(PROPERTY)==null||done||event.getEntity().level().isClientSide()||event.getEntity().getId()!=targetId)return;
        targetHealthBeforeDamage=event.getEntity().getHealth();
    }

    @SubscribeEvent public static void afterTargetDamage(LivingDamageEvent.Post event){
        if(System.getProperty(PROPERTY)==null||done||event.getEntity().level().isClientSide()||event.getEntity().getId()!=targetId)return;
        var target=event.getEntity();var source=event.getSource().getEntity();var direct=event.getSource().getDirectEntity();
        var sample=new JsonObject();sample.addProperty("scene",1);sample.addProperty("targetId",target.getId());sample.addProperty("targetUuid",target.getUUID().toString());
        sample.addProperty("gameTime",target.level().getGameTime());sample.addProperty("serverTick",target.getServer().getTickCount());
        sample.addProperty("sourceEntityId",source==null?-1:source.getId());sample.addProperty("sourceUuid",source==null?"":source.getUUID().toString());
        sample.addProperty("directEntityId",direct==null?-1:direct.getId());sample.addProperty("damageType",event.getSource().getMsgId());
        sample.addProperty("healthBefore",Float.isFinite(targetHealthBeforeDamage)?targetHealthBeforeDamage:target.getHealth()+event.getNewDamage());
        sample.addProperty("healthBeforeObserved",Float.isFinite(targetHealthBeforeDamage));sample.addProperty("healthAfter",target.getHealth());
        sample.addProperty("originalDamage",event.getOriginalDamage());sample.addProperty("appliedDamage",event.getNewDamage());
        sample.addProperty("requestPlayerTick",requestPlayerTick);sample.addProperty("sourcePlayerTick",source==null?-1:source.tickCount);
        sample.addProperty("elapsedPlayerTicks",source==null||requestPlayerTick<0?-1:source.tickCount-requestPlayerTick);
        if(source instanceof ServerPlayer player)addVisual(sample,player.getData(ModAttachments.WEAPON_VISUAL),player.level().getGameTime());
        damageEvents.add(sample);targetHealthBeforeDamage=Float.NaN;
    }

    @SubscribeEvent(priority=EventPriority.LOWEST,receiveCanceled=true)
    public static void targetHealing(LivingHealEvent event){
        if(System.getProperty(PROPERTY)==null||done||event.getEntity().level().isClientSide()||event.getEntity().getId()!=targetId)return;
        var target=event.getEntity();var sample=new JsonObject();sample.addProperty("targetId",targetId);sample.addProperty("targetUuid",target.getUUID().toString());
        sample.addProperty("gameTime",target.level().getGameTime());sample.addProperty("serverTick",target.getServer().getTickCount());
        sample.addProperty("healthBefore",target.getHealth());sample.addProperty("maxHealth",target.getMaxHealth());
        sample.addProperty("requestedHeal",event.getAmount());sample.addProperty("canceledAtObserver",event.isCanceled());
        sample.addProperty("predictedClampedGain",event.isCanceled()?0:Math.min(Math.max(0,event.getAmount()),Math.max(0,target.getMaxHealth()-target.getHealth())));
        // LivingHealEvent has no source field and fires before health changes. Preserve the actual
        // call path and later observed health deltas instead of assuming the request was applied.
        var callers=StackWalker.getInstance().walk(frames->frames
                .filter(frame->frame.getClassName().startsWith("com.codex.blacklight.")&&!frame.getClassName().equals(BladeReferencePreview.class.getName()))
                .limit(12).map(frame->frame.getClassName()+"#"+frame.getMethodName()+":"+frame.getLineNumber()).toList());
        sample.add("blacklightCallers",JSON.toJsonTree(callers));
        sample.addProperty("infectionEffectCaller",callers.stream().anyMatch(name->name.startsWith("com.codex.blacklight.effect.BlacklightInfectionEffect#")));
        addTarget(sample,target);healingEvents.add(sample);
    }

    private static String stage(int tick){
        var move=WeaponMove.BLADE_CUT;
        return tick<0?"idle_before":tick<move.start?"windup":tick<move.end?"contact":tick<move.duration?"follow_through":"idle_after";
    }

    private static void verifyCapture(Path output)throws Exception{
        var move=WeaponMove.BLADE_CUT;
        for(int index=0;index<2;index++){
            int requests=0,active=0,lastElapsed=-1,idleAfter=0,clientLastElapsed=-1;
            boolean contact=false,clientWindup=false,clientContact=false,clientFollowThrough=false,clientIdle=false;
            for(var sample:observations){
                if(sample.get("scene").getAsInt()!=index)continue;
                if(sample.get("request").getAsBoolean())requests++;
                var visual=sample.getAsJsonObject("weaponVisual");int id=visual.get("move").getAsInt(),elapsed=visual.get("elapsed").getAsInt();
                if(id==move.ordinal()){active++;lastElapsed=Math.max(lastElapsed,elapsed);contact|=elapsed>=move.start&&elapsed<move.end;}
                else if(id!=-1)failures.add("Unexpected combo/weapon action in scene "+index+": "+id);
                if(sample.get("phaseTick").getAsInt()>ACTION_AT+move.duration&&id==-1&&!sample.get("busy").getAsBoolean()
                        &&sample.get("clawAction").getAsString().equals("IDLE"))idleAfter++;
                for(String axis:List.of("x","y","z"))if(!Double.isFinite(sample.get(axis).getAsDouble()))failures.add("Non-finite player displacement");
            }
            for(var sample:rendered){
                if(sample.get("scene").getAsInt()!=index)continue;
                var visual=sample.getAsJsonObject("weaponVisual");int id=visual.get("move").getAsInt(),elapsed=visual.get("elapsed").getAsInt();
                if(id==move.ordinal()){
                    clientWindup|=elapsed>=0&&elapsed<move.start;clientContact|=elapsed>=move.start&&elapsed<move.end;
                    clientFollowThrough|=elapsed>=move.end;clientLastElapsed=Math.max(clientLastElapsed,elapsed);
                }
                clientIdle|=sample.get("phaseTick").getAsInt()>ACTION_AT+move.duration+5&&id==-1;
            }
            if(requests!=1||active==0||lastElapsed<move.duration-2||!contact||idleAfter<10||!clientWindup||!clientContact||!clientFollowThrough||clientLastElapsed<move.duration-2||!clientIdle)
                failures.add("Incomplete single blade action in scene "+index+": requests="+requests+", active="+active+", lastElapsed="+lastElapsed+", contact="+contact+", recoveredTicks="+idleAfter
                        +", renderedWindup="+clientWindup+", renderedContact="+clientContact+", renderedFollowThrough="+clientFollowThrough+", renderedLastElapsed="+clientLastElapsed+", clientRecovered="+clientIdle);
        }
        for(var sample:rendered){
            Path file=output.resolve("screenshots").resolve(sample.get("file").getAsString());
            if(!Files.isRegularFile(file)||Files.size(file)==0||Files.getLastModifiedTime(file).toMillis()<startedMillis-2000)
                failures.add("Screenshot write failed or file is stale: "+file.getFileName());
        }
        if(saved==0)failures.add("No rendered screenshots");
        verifyTargetDamage();
        verifyMovement();
    }

    private static void verifyTargetDamage(){
        var move=WeaponMove.BLADE_CUT;int healthLossEvents=0,healthDrops=0;
        double totalDamage=0,previousHealth=Double.NaN,finalHealth=Double.NaN,observedDamage=0,observedHealing=0;
        if(targetMetadata==null){failures.add("Front three-quarter damage target was never spawned");return;}
        for(var sample:damageEvents){
            double damage=sample.get("appliedDamage").getAsDouble();if(damage<=.0001)continue;
            healthLossEvents++;totalDamage+=damage;int elapsed=sample.get("elapsedPlayerTicks").getAsInt();
            double before=sample.get("healthBefore").getAsDouble(),after=sample.get("healthAfter").getAsDouble();
            if(sample.get("sourceEntityId").getAsInt()!=qaPlayerId||elapsed<move.start||elapsed>=move.end
                    ||!sample.has("weaponVisual")||sample.getAsJsonObject("weaponVisual").get("move").getAsInt()!=move.ordinal())
                failures.add("Target damage outside the real blade contact window or from another entity: "+sample);
            if(!sample.get("healthBeforeObserved").getAsBoolean()||Math.abs((before-after)-move.damage)>.0001||Math.abs(damage-move.damage)>.0001)
                failures.add("Target event did not lose exactly move.damage="+move.damage+": "+sample);
        }
        for(var sample:observations){
            if(sample.get("scene").getAsInt()!=1)continue;var target=sample.getAsJsonObject("target");
            if(!target.get("present").getAsBoolean()){failures.add("Damage target disappeared during scene 1");continue;}
            double health=target.get("health").getAsDouble();
            if(Double.isFinite(previousHealth)&&Math.abs(health-previousHealth)>.0001){
                double delta=health-previousHealth;var change=new JsonObject();change.addProperty("scene",1);change.addProperty("targetId",targetId);
                change.addProperty("phaseTick",sample.get("phaseTick").getAsInt());change.addProperty("serverTick",sample.get("serverTick").getAsInt());
                change.addProperty("gameTime",sample.get("gameTime").getAsLong());change.addProperty("healthBefore",previousHealth);
                change.addProperty("healthAfter",health);change.addProperty("delta",delta);change.addProperty("kind",delta<0?"health_loss":"health_gain");
                change.add("target",target.deepCopy());healthChanges.add(change);
                if(delta<0){healthDrops++;observedDamage-=delta;}else observedHealing+=delta;
            }
            previousHealth=health;finalHealth=health;
        }
        double healthLoss=targetMetadata.get("initialHealth").getAsDouble()-finalHealth;
        double predictedHealing=0;for(var event:healingEvents)predictedHealing+=event.get("predictedClampedGain").getAsDouble();
        healthSummary=new JsonObject();healthSummary.addProperty("damageEventCount",healthLossEvents);healthSummary.addProperty("cumulativeAppliedDamage",totalDamage);
        healthSummary.addProperty("observedNegativeChangeCount",healthDrops);healthSummary.addProperty("cumulativeObservedHealthLoss",observedDamage);
        healthSummary.addProperty("cumulativeObservedHealing",observedHealing);healthSummary.addProperty("healingRequestCount",healingEvents.size());
        healthSummary.addProperty("predictedHealingFromRequests",predictedHealing);healthSummary.addProperty("initialHealth",targetMetadata.get("initialHealth").getAsDouble());
        if(Double.isFinite(finalHealth))healthSummary.addProperty("finalHealth",finalHealth);else healthSummary.add("finalHealth",JsonNull.INSTANCE);
        if(Double.isFinite(healthLoss))healthSummary.addProperty("netHealthLoss",healthLoss);else healthSummary.add("netHealthLoss",JsonNull.INSTANCE);
        healthSummary.addProperty("interpretation","Damage events are authoritative and each positive event is validated. Later healing is separate from damage. Polling may combine damage and healing within one interval; heal requests include call-path evidence and are checked against net balance.");
        if(healthLossEvents!=1||healthDrops>1||observedDamage>move.damage+.0001||Math.abs(totalDamage-move.damage)>.0001||!Double.isFinite(healthLoss))
            failures.add("Expected exactly one blade damage event without additional health losses: damageEvents="+healthLossEvents+", observedHealthDrops="+healthDrops+", cumulativeObservedLoss="+observedDamage+", eventDamage="+totalDamage+", expected="+move.damage);
        if(Double.isFinite(healthLoss)&&Math.abs(healthLoss-(totalDamage-predictedHealing))>.0001)
            failures.add("Unexplained target health balance: netLoss="+healthLoss+", recordedDamage="+totalDamage+", predictedHealing="+predictedHealing+"; inspect all damage/healing events and healthChanges");
    }

    private static void verifyMovement(){
        var animation=BlacklightEpicFightAnimations.weapon(WeaponMove.BLADE_CUT).get();
        var coord=animation.getProperty(yesman.epicfight.api.animation.property.AnimationProperty.ActionAnimationProperty.COORD);
        movementSummary=new JsonObject();var scenes=new JsonArray();movementSummary.add("scenes",scenes);
        if(coord.isEmpty()){failures.add("Loaded blade animation has no COORD movement track");return;}
        var keys=coord.get().getKeyframes();if(keys.length<2){failures.add("Blade COORD has fewer than two keys");return;}
        var first=keys[0].transform().translation();var last=keys[keys.length-1].transform().translation();
        double expectedX=last.x-first.x,expectedZ=last.z-first.z,expectedDistance=Math.hypot(expectedX,expectedZ);
        movementSummary.addProperty("coordDeltaX",expectedX);movementSummary.addProperty("coordDeltaZ",expectedZ);movementSummary.addProperty("coordHorizontalDistance",expectedDistance);
        movementSummary.addProperty("method","External camera uses normal PosRot synchronization of actual client entity coordinates. Compare final recovered client/server positions; empty swing follows loaded COORD within 0.12 blocks. Target scene permits collision-shortened travel but requires forward movement and side agreement.");
        for(int index=0;index<2;index++){
            JsonObject serverLast=null,clientLast=null;
            for(var sample:observations)if(sample.get("scene").getAsInt()==index&&sample.get("phaseTick").getAsInt()>ACTION_AT+WeaponMove.BLADE_CUT.duration+10)serverLast=sample;
            for(var sample:rendered)if(sample.get("scene").getAsInt()==index&&sample.get("phaseTick").getAsInt()>ACTION_AT+WeaponMove.BLADE_CUT.duration+10)clientLast=sample;
            if(serverLast==null||clientLast==null){failures.add("Missing recovered displacement samples for scene "+index);continue;}
            double sx=serverLast.get("dxFromStart").getAsDouble(),sz=serverLast.get("dzFromStart").getAsDouble();
            double cx=clientLast.get("dxFromStart").getAsDouble(),cz=clientLast.get("dzFromStart").getAsDouble();
            double disagreement=Math.hypot(sx-cx,sz-cz),coordError=Math.hypot(sx-expectedX,sz-expectedZ);
            var sample=new JsonObject();sample.addProperty("scene",index);sample.addProperty("serverDx",sx);sample.addProperty("serverDz",sz);
            sample.addProperty("clientDx",cx);sample.addProperty("clientDz",cz);sample.addProperty("clientServerHorizontalError",disagreement);
            sample.addProperty("serverCoordEndpointError",coordError);sample.addProperty("collisionTargetPresent",index==1);scenes.add(sample);
            if(disagreement>.12)failures.add("Server/client displacement disagrees in scene "+index+": "+disagreement);
            if(index==0&&coordError>.12)failures.add("Empty swing entity displacement does not match loaded COORD: "+coordError);
            if(expectedDistance>.15&&(Math.hypot(sx,sz)<.1||sx*expectedX+sz*expectedZ<=0))
                failures.add("Scene "+index+" lacks forward server entity movement despite moving COORD");
        }
    }

    private static void writeManifest(Path output)throws Exception{
        Files.createDirectories(output);var move=WeaponMove.BLADE_CUT;var manifest=new JsonObject();
        manifest.addProperty("status",failures.isEmpty()?"passed":"failed");manifest.addProperty("save",WORLD);manifest.addProperty("trigger","WeaponCombatSystem.request(player, false), exactly once per scene");
        manifest.addProperty("runStartedEpochMillis",startedMillis);
        manifest.addProperty("move",move.name());manifest.addProperty("durationTicks",move.duration);manifest.addProperty("contactStartTick",move.start);manifest.addProperty("contactEndTickExclusive",move.end);
        manifest.addProperty("animationClass",BlacklightEpicFightAnimations.weapon(move).get().getClass().getName());
        manifest.addProperty("localMotionLoaded",LocalWeaponMotion.find(move.id).isPresent());
        manifest.addProperty("requestedFps",20);manifest.addProperty("geometryAuditHz",60);manifest.addProperty("captureStartTick",CAPTURE_FROM);
        manifest.addProperty("requestPhaseTick",ACTION_AT);manifest.addProperty("captureEndTick",captureEnd);manifest.addProperty("sceneTicks",sceneTicks);
        manifest.addProperty("savedFrames",saved);manifest.addProperty("completedWriteCallbacks",written.get());manifest.add("skippedRenderFrames",JSON.toJsonTree(skipped));
        manifest.addProperty("stageLabels","Filename stage is the requested server phase; each rendered frame includes actual client WEAPON_VISUAL elapsed time. Idle-after labels alone do not prove recovery.");
        manifest.addProperty("angles","0: side (27,220.1,22), yaw 90 pitch 7; 1: front three-quarter (25,220.2,16), yaw 39.8 pitch 7; player starts (20,220,22), yaw 180");
        manifest.addProperty("scope","Full visible single action and return to idle: side scene is an empty swing; front three-quarter scene hits one NoAI villager initially two blocks ahead. Target is never repositioned after spawning; no combo or manually substituted static clip. Original-game fidelity still requires visual comparison.");
        manifest.addProperty("expectedDamage",move.damage);manifest.addProperty("expectedHealthLossEvents",1);manifest.addProperty("qaPlayerEntityId",qaPlayerId);
        manifest.addProperty("damageClock","LivingDamageEvent.Post captures actual server damage, checked against sourcePlayer.tickCount minus the request player tick, with start inclusive/end exclusive. Pre records health before damage; observed negative health changes and later healing are recorded separately, never treated as a requirement for net loss to equal move.damage.");
        if(targetMetadata!=null)manifest.add("damageTarget",targetMetadata);manifest.add("damageEvents",JSON.toJsonTree(damageEvents));
        manifest.add("healingEvents",JSON.toJsonTree(healingEvents));manifest.add("healthChanges",JSON.toJsonTree(healthChanges));
        if(healthSummary!=null)manifest.add("healthSummary",healthSummary);if(movementSummary!=null)manifest.add("movementSummary",movementSummary);
        manifest.addProperty("knownHealingPath","BlacklightInfectionSystem.onContactDamage may infect on a player hit (35%); ModEffects adds 6 max health; BlacklightInfectionEffect.applyEffectTick calls heal(0.75 + amplifier*0.25) on effect ticks divisible by 80. Actual healing requests retain caller evidence; infection is not disabled by this QA.");
        manifest.add("failures",JSON.toJsonTree(failures));manifest.add("serverObservations",JSON.toJsonTree(observations));manifest.add("renderedFrames",JSON.toJsonTree(rendered));
        if(audit!=null)manifest.addProperty("clearanceReport","blade-clearance.json");
        Files.writeString(output.resolve("capture-manifest.json"),JSON.toJson(manifest));
    }

    private static void finish(Path output)throws Exception{
        String text=failures.isEmpty()?"PASS: two angles, one real BLADE_CUT each, one villager damage event inside the contact window, separately audited healing, synchronized entity displacement, complete recovery and clearance audit. "+saved+" PNG frames. See capture-manifest.json and blade-clearance.json.":String.join(System.lineSeparator(),failures);
        Files.writeString(output.resolve(failures.isEmpty()?"capture-complete.txt":"capture-failed.txt"),text);
        done=true;var mc=Minecraft.getInstance();mc.setCameraEntity(mc.player);mc.options.hideGui=false;mc.stop();
    }

    private static void fail(Path output,Exception error){
        if(done)return;failures.add(error.toString());error.printStackTrace();
        try{writeManifest(output);Files.deleteIfExists(output.resolve("capture-complete.txt"));finish(output);}
        catch(Exception writeError){writeError.printStackTrace();done=true;Minecraft.getInstance().stop();}
    }

    private record Vertex(Vec3f point,boolean shoulderMount){}
    private record BodyBox(String name,String joint,double minX,double minY,double minZ,double maxX,double maxY,double maxZ){
        double signedDistance(Vec3f v){
            double dx=Math.max(minX-v.x,Math.max(0,v.x-maxX)),dy=Math.max(minY-v.y,Math.max(0,v.y-maxY)),dz=Math.max(minZ-v.z,Math.max(0,v.z-maxZ));
            if(dx+dy+dz>0)return Math.sqrt(dx*dx+dy*dy+dz*dz);
            return -Math.min(Math.min(v.x-minX,maxX-v.x),Math.min(Math.min(v.y-minY,maxY-v.y),Math.min(v.z-minZ,maxZ-v.z)));
        }
    }

    /** All loaded blade vertices; posed head/chest/waist boxes are a diagnostic proxy, not triangle collision. */
    private static JsonObject auditBlade(Path output)throws Exception{
        var model=ResourceLocation.fromNamespaceAndPath("blacklight","models/form_blade.json");
        var resource=Minecraft.getInstance().getResourceManager().getResource(model).orElseThrow();
        JsonObject json;try(var reader=resource.openAsReader()){json=JsonParser.parseReader(reader).getAsJsonObject();}
        var joints=json.getAsJsonObject("joints");var points=new TreeMap<String,List<Vertex>>();
        for(String name:List.of("Arm_R","Hand_R")){
            var unique=new HashSet<String>();var vertices=new ArrayList<Vertex>();
            for(var face:joints.getAsJsonArray(name))for(var point:face.getAsJsonObject().getAsJsonArray("p")){
                if(!unique.add(point.toString()))continue;var p=point.getAsJsonArray();
                var v=new Vec3f(p.get(0).getAsFloat(),p.get(1).getAsFloat(),p.get(2).getAsFloat());
                // The proximal 0.12 units of the upper-arm sleeve are intended to meet the shoulder.
                vertices.add(new Vertex(v,name.equals("Arm_R")&&v.y>=-.06F&&v.y<=.12F));
            }
            if(vertices.isEmpty())throw new IllegalStateException("Empty blade model joint "+name);points.put(name,vertices);
        }
        var animation=BlacklightEpicFightAnimations.weapon(WeaponMove.BLADE_CUT).get();var clip=animation.getAnimationClip();
        if(!Float.isFinite(clip.getClipTime())||clip.getClipTime()<=0)throw new IllegalStateException("Blade clip has no finite duration");
        var times=new TreeSet<Float>();for(int frame=0;frame<=Math.ceil(clip.getClipTime()*60);frame++)times.add(Math.min(clip.getClipTime(),frame/60F));
        for(var track:clip.getJointTransforms().values())for(var frame:track.getKeyframes())times.add(frame.time());
        var boxes=List.of(new BodyBox("head","Head",-.25,0,-.25,.25,.5,.25),
                new BodyBox("chest","Chest",-.25,0,-.125,.25,.4,.125),
                new BodyBox("waist","Torso",-.25,-.064,-.125,.25,.3,.125));
        var rig=yesman.epicfight.gameasset.Armatures.BIPED.get();var samples=new JsonArray();var summary=new JsonObject();
        int unexpected=0;double minimum=Double.POSITIVE_INFINITY;
        for(var body:boxes)for(var entry:points.entrySet()){
            double closest=Double.POSITIVE_INFINITY;float closestTime=0;int penetrations=0,tolerated=0;
            for(float time:times){
                var pose=animation.getRawPose(time);var bodyMatrix=rig.getBoundTransformFor(pose,rig.searchJointByName(body.joint()));
                var source=rig.getBoundTransformFor(pose,rig.searchJointByName(entry.getKey()));
                var relative=OpenMatrix4f.mul(OpenMatrix4f.invert(bodyMatrix,null),source,null);
                double distance=Double.POSITIVE_INFINITY;int closestVertex=-1,bad=0,allowed=0;
                for(int index=0;index<entry.getValue().size();index++){
                    var vertex=entry.getValue().get(index);var transformed=OpenMatrix4f.transform3v(relative,vertex.point(),null);
                    double d=body.signedDistance(transformed);if(!Double.isFinite(d))throw new IllegalStateException("Non-finite loaded blade pose at "+time);
                    if(d<distance){distance=d;closestVertex=index;}
                    // No head exemption. Body surface overlap <= 0.025 is tolerated; only the proximal
                    // upper-arm mount gets 0.065. Deep chest/waist crossings remain failures for both joints.
                    double tolerance=body.name().equals("head")?0:(vertex.shoulderMount()?.065:.025);
                    if(d<-.00001){if(-d>tolerance+.00001)bad++;else allowed++;}
                }
                if(bad>0){penetrations++;unexpected++;}if(allowed>0)tolerated++;
                if(distance<closest){closest=distance;closestTime=time;}minimum=Math.min(minimum,distance);
                var sample=new JsonObject();sample.addProperty("body",body.name());sample.addProperty("joint",entry.getKey());sample.addProperty("time",time);
                sample.addProperty("signedClearance",distance);sample.addProperty("closestVertex",closestVertex);sample.addProperty("unexpectedVertices",bad);sample.addProperty("toleratedSurfaceOrMountVertices",allowed);samples.add(sample);
            }
            var item=new JsonObject();item.addProperty("vertexCount",entry.getValue().size());item.addProperty("minimumSignedClearance",closest);item.addProperty("closestTime",closestTime);
            item.addProperty("unexpectedPenetratingSamples",penetrations);item.addProperty("samplesWithAllowedSurfaceOrMountContact",tolerated);summary.add(body.name()+"/"+entry.getKey(),item);
        }
        var report=new JsonObject();report.addProperty("clip","blacklight:biped/combat/blade_cut");report.addProperty("runtimeAnimationClass",animation.getClass().getName());
        report.addProperty("clipSeconds",clip.getClipTime());report.addProperty("model",model.toString());report.addProperty("sampling","60 Hz + every loaded keyframe, inclusive clip endpoint; actual loaded raw runtime poses");
        report.addProperty("samplesPerJointAndBody",times.size());report.addProperty("minimumSignedClearance",minimum);report.addProperty("unexpectedPenetratingSampleCount",unexpected);
        report.addProperty("headBounds","Head local x/z [-0.25,0.25], y [0,0.5]; no overlap tolerance");
        report.addProperty("bodyBounds","Chest local x [-0.25,0.25], y [0,0.4], z [-0.125,0.125]; Torso local x [-0.25,0.25], y [-0.064,0.3], z [-0.125,0.125]");
        report.addProperty("expectedConnection","Body surface overlap <=0.025 units allowed. Proximal Arm_R sleeve only (local y [-0.06,0.12]) allows <=0.065 units at chest/waist. Never exempt Hand_R or head.");
        report.addProperty("limitation","All unique Arm_R/Hand_R model vertices are checked. These posed vanilla body boxes are conservative proxies; triangle interiors, armor, skin outer layer, procedural tendrils and animation link/idle blending are not tested. Review PNGs for those cases and attack fidelity.");
        report.add("summary",summary);report.add("samples",samples);Files.writeString(output.resolve("blade-clearance.json"),JSON.toJson(report));return report;
    }
}
