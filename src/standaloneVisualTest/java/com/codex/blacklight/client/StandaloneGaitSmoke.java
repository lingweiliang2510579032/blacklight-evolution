package com.codex.blacklight.client;

import com.codex.blacklight.BlacklightNeoMod;
import com.codex.blacklight.animation.*;
import com.codex.blacklight.client.animation.BlacklightRenderPose;
import com.codex.blacklight.movement.*;
import com.codex.blacklight.power.*;
import com.codex.blacklight.registry.ModAttachments;
import com.codex.blacklight.state.BlacklightState;
import com.google.gson.*;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.*;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Opt-in continuous native-input QA; refuses every save outside the disposable QA copy. */
@EventBusSubscriber(modid=BlacklightNeoMod.MOD_ID,value=Dist.CLIENT)
public final class StandaloneGaitSmoke {
    private static final String PROPERTY="blacklight.test.standaloneGait";
    private enum Stage { SETTLE,SPRINT,STOP,ARMOR_SETTLE,ARMOR_SPRINT,ARMOR_STOP,JUMP_SETTLE,JUMP_SPRINT,JUMP_STOP,DONE }
    private static Stage stage=Stage.SETTLE;
    private static boolean initialized,disabled,completed,capturePending;
    private static volatile boolean serverReady;
    private static int stageTicks;
    private static long firstJumpAirTick=-1,jumpContactTick=-1;
    private static int jumpAirTicks,jumpHoldTicks=2;
    private static boolean movingLandingObserved;
    private static boolean jumpOnly(){return Boolean.getBoolean(PROPERTY+".jumpOnly");}
    private static long latestTick=Long.MIN_VALUE;
    private static Path output;
    private static CameraType previousCamera;
    private static int previousFov;
    private static boolean previousGui,previousPause,previousMouseGrabbed;
    private static float previousYaw,previousPitch,yawBeforeInputLock;
    private static int suppressedMouseEvents,suppressedPerspectiveClicks;
    private static final Map<KeyMapping,Boolean> savedKeys=new IdentityHashMap<>();
    private static final JsonArray ticks=new JsonArray(),frames=new JsonArray(),violations=new JsonArray();
    private static Field statesField,gaitField,positionField;
    private static JsonObject previousTick;
    private static final Set<String> captureNames=new HashSet<>();
    private StandaloneGaitSmoke() {}
    private static boolean active(){return initialized&&!disabled&&!completed;}
    private static boolean sprinting(){return stage==Stage.SPRINT||stage==Stage.ARMOR_SPRINT||stage==Stage.JUMP_SPRINT;}
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void input(ClientTickEvent.Pre event) {
        if(!active())return;
        var mc=Minecraft.getInstance();
        // This fixture owns native W/sprint but must not consume the human's pointer.
        // Lock before LocalPlayer physics; the observer camera is never a control yaw.
        mc.mouseHandler.releaseMouse();
        // This isolated measurement fixture owns its observer. Human F5 must not
        // switch to the source chase camera and feed that camera yaw into W.
        while(mc.options.keyTogglePerspective.consumeClick())suppressedPerspectiveClicks++;
        mc.options.keyTogglePerspective.setDown(false);
        mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        if(stageTicks%20==0)mc.getWindow().setTitle("Blacklight · 自动跑跳 QA（请勿操作测试窗口）");
        if(mc.player!=null){
            yawBeforeInputLock=mc.player.getYRot();
            mc.player.setYRot(0);mc.player.yRotO=0;mc.player.setXRot(0);mc.player.xRotO=0;
        }
        boolean run=serverReady&&sprinting();
        mc.options.keyUp.setDown(run);mc.options.keySprint.setDown(run);
        mc.options.keyDown.setDown(false);mc.options.keyLeft.setDown(false);mc.options.keyRight.setDown(false);
        mc.options.keyJump.setDown(run&&stage==Stage.JUMP_SPRINT&&stageTicks>=20&&stageTicks<20+jumpHoldTicks);mc.options.keyShift.setDown(false);
        mc.options.keyAttack.setDown(false);mc.options.keyUse.setDown(false);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void humanPerspective(InputEvent.Key event) {
        if(active()&&Minecraft.getInstance().options.keyTogglePerspective.matches(event.getKey(),event.getScanCode())) {
            var mc=Minecraft.getInstance();
            while(mc.options.keyTogglePerspective.consumeClick())suppressedPerspectiveClicks++;
            mc.options.keyTogglePerspective.setDown(false);mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        }
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void humanMouse(InputEvent.MouseButton.Pre event) {
        if(active()) {
            suppressedMouseEvents++;
            // Prevent vanilla's onPress from reacquiring capture or attacking the lane.
            event.setCanceled(true);Minecraft.getInstance().mouseHandler.releaseMouse();
        }
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void tick(ClientTickEvent.Post event) {
        if(disabled||completed||System.getProperty(PROPERTY,"").isBlank())return;
        var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();
        if(server==null||mc.player==null||mc.level==null)return;
        var save=server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toAbsolutePath().normalize();
        if(!save.getFileName().toString().equals("Blacklight Standalone QA v4")
                ||!mc.gameDirectory.toPath().toAbsolutePath().normalize().getFileName().toString().equals("run-native-qa")) {
            disabled=true;BlacklightNeoMod.LOGGER.error("Continuous gait QA refused non-QA save {}",save);return;
        }
        if(!initialized) {
            try {
                output=Path.of(System.getProperty(PROPERTY)).toAbsolutePath().normalize();Files.createDirectories(output);
                if(ModList.get().isLoaded("epicfight")||ModList.get().isLoaded("wom"))throw new IllegalStateException("Combat framework present on QA classpath");
                previousCamera=mc.options.getCameraType();previousFov=mc.options.fov().get();previousGui=mc.options.hideGui;previousPause=mc.options.pauseOnLostFocus;
                previousMouseGrabbed=mc.mouseHandler.isMouseGrabbed();previousYaw=mc.player.getYRot();previousPitch=mc.player.getXRot();
                for(var key:List.of(mc.options.keyUp,mc.options.keySprint,mc.options.keyDown,mc.options.keyLeft,mc.options.keyRight,
                        mc.options.keyJump,mc.options.keyShift,mc.options.keyAttack,mc.options.keyUse,mc.options.keyTogglePerspective))savedKeys.put(key,key.isDown());
                mc.options.pauseOnLostFocus=false;mc.options.hideGui=false;mc.options.fov().set(60);mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);mc.setScreen(null);mc.mouseHandler.releaseMouse();mc.getWindow().setTitle("Blacklight · 自动跑跳 QA（请勿操作测试窗口）");
                statesField=PlayerMotionController.class.getDeclaredField("STATES");statesField.setAccessible(true);
                initialized=true;var uuid=mc.player.getUUID();
                // Exercise a real light moving impact, rather than a tiny jump with
                // no landing action. Derive hold from the configured native physics.
                int maxCharge=MovementTuningSettings.jumpChargeTicks();jumpHoldTicks=maxCharge;
                for(int held=1;held<=maxCharge;held++)
                    if(MovementTuningSettings.estimatedJumpHeight(held/(double)maxCharge,0)>=5.5){jumpHoldTicks=held;break;}
                server.execute(()->{
                    try {
                        var level=server.overworld();level.setDayTime(6000);level.setWeatherParameters(6000,0,false,false);
                        // A 384-block lane covers the complete 120-tick default sprint.
                        for(int x=-4;x<=4;x++)for(int z=-8;z<=384;z++) {
                            level.setBlockAndUpdate(new BlockPos(x,250,z),(z%8==0?Blocks.WHITE_CONCRETE:Blocks.GRAY_CONCRETE).defaultBlockState());
                            for(int y=251;y<=255;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
                        }
                        var p=server.getPlayerList().getPlayer(uuid);if(p==null)throw new IllegalStateException("QA actor missing");
                        var state=new BlacklightState();state.infect();state.setPrototypeMode(true);state.unlock(PowerForm.NONE);state.unlock(PowerForm.ARMOR);state.activate(PowerForm.NONE);state.addBiomass(1000);
                        p.setData(ModAttachments.PLAYER_STATE,state);p.syncData(ModAttachments.PLAYER_STATE);
                        p.setData(ModAttachments.UTILITY_POWERS,0);p.syncData(ModAttachments.UTILITY_POWERS);
                        p.setData(ModAttachments.UTILITY_STATE,new UtilityState(UtilityState.SHIELD_CAPACITY,0,level.getGameTime()-200,0,-1000));p.syncData(ModAttachments.UTILITY_STATE);
                        p.setData(ModAttachments.POWER_MORPH,PowerMorph.NONE);p.syncData(ModAttachments.POWER_MORPH);
                        p.setData(ModAttachments.WEAPON_VISUAL,com.codex.blacklight.combat.WeaponVisual.EMPTY);p.syncData(ModAttachments.WEAPON_VISUAL);
                        NativePlayerActionBackend.clear(p);com.codex.blacklight.combat.CombatTargeting.requestLock(p,-1);
                        p.getInventory().clearContent();
                        p.setGameMode(GameType.CREATIVE);p.getAbilities().flying=false;p.onUpdateAbilities();
                        p.connection.teleport(.5,251,.5,0,0);p.setDeltaMovement(0,0,0);p.setSprinting(false);
                        serverReady=true;
                    }catch(Exception e){fail(e);}
                });
            }catch(Exception e){fail(e);}return;
        }
        try {recordTick(mc);}catch(Exception e){fail(e);return;}
        if(!serverReady)return;
        stageTicks++;
        if(stage==Stage.SETTLE&&stageTicks>=100)advance(jumpOnly()?Stage.JUMP_SPRINT:Stage.SPRINT);
        else if(stage==Stage.SPRINT&&stageTicks>=120)advance(Stage.STOP);
        else if(stage==Stage.STOP&&stageTicks>=30) {
            advance(Stage.ARMOR_SETTLE);serverReady=false;var uuid=mc.player.getUUID();
            server.execute(()->{
                try {
                    var p=server.getPlayerList().getPlayer(uuid);if(p==null)throw new IllegalStateException("QA actor missing");
                    p.connection.teleport(.5,251,.5,0,0);p.setDeltaMovement(0,0,0);p.setSprinting(false);
                    UtilityPowerSystem.toggle(p,PowerForm.ARMOR);serverReady=true;
                }catch(Exception e){fail(e);}
            });
        } else if(stage==Stage.ARMOR_SETTLE&&stageTicks>=20)advance(Stage.ARMOR_SPRINT);
        else if(stage==Stage.ARMOR_SPRINT&&stageTicks>=120)advance(Stage.ARMOR_STOP);
        else if(stage==Stage.ARMOR_STOP&&stageTicks>=30) {
            advance(Stage.JUMP_SETTLE);serverReady=false;var uuid=mc.player.getUUID();
            server.execute(()->{
                try {
                    var p=server.getPlayerList().getPlayer(uuid);if(p==null)throw new IllegalStateException("QA actor missing");
                    p.connection.teleport(.5,251,.5,0,0);p.setDeltaMovement(0,0,0);p.setSprinting(false);
                    if(UtilityPowerSystem.active(p,PowerForm.ARMOR))UtilityPowerSystem.toggle(p,PowerForm.ARMOR);
                    NativePlayerActionBackend.clear(p);serverReady=true;
                }catch(Exception e){fail(e);}
            });
        } else if(stage==Stage.JUMP_SETTLE&&stageTicks>=20)advance(Stage.JUMP_SPRINT);
        else if(stage==Stage.JUMP_SPRINT&&stageTicks>=140)advance(Stage.JUMP_STOP);
        else if(stage==Stage.JUMP_STOP&&stageTicks>=30){stage=Stage.DONE;capturePending=true;}
    }
    private static void advance(Stage next){stage=next;stageTicks=0;}
    private static Object runtime(Minecraft mc)throws Exception {
        var states=(Map<?,?>)statesField.get(null);var runtime=states.get(mc.player);
        if(runtime!=null&&gaitField==null) {
            gaitField=runtime.getClass().getDeclaredField("gait");gaitField.setAccessible(true);
            positionField=runtime.getClass().getDeclaredField("committedPosition");positionField.setAccessible(true);
        }
        return runtime;
    }
    private static void recordTick(Minecraft mc)throws Exception {
        var runtime=runtime(mc);if(runtime==null)return;
        var clock=(LocomotionClock)gaitField.get(runtime);if(clock.key()==null||clock.tick()==latestTick)return;
        var position=(Vec3)positionField.get(runtime);if(position==null)return;
        var row=new JsonObject();row.addProperty("tick",clock.tick());row.addProperty("stage",stage.name());row.addProperty("stageTick",stageTicks);
        row.addProperty("key",clock.key());row.addProperty("phase",PrototypeAnimationController.phaseFor(mc.player).name());
        row.addProperty("loop",clock.looping());row.addProperty("duration",clock.duration());row.addProperty("previousSeconds",clock.seconds(0));row.addProperty("currentSeconds",clock.current());
        row.addProperty("x",position.x);row.addProperty("y",position.y);row.addProperty("z",position.z);
        row.addProperty("playerYaw",mc.player.getYRot());row.addProperty("playerPitch",mc.player.getXRot());
        row.addProperty("movementYaw",mc.player.getData(ModAttachments.MOVEMENT_STATE).cameraYaw(mc.player.getYRot()));
        row.addProperty("observerYaw",mc.gameRenderer.getMainCamera().getYRot());row.addProperty("yawBeforeInputLock",yawBeforeInputLock);
        row.addProperty("chaseOwnsView",com.codex.blacklight.client.camera.PrototypeChaseCamera.ownsView());
        row.addProperty("cameraType",mc.options.getCameraType().name());
        row.addProperty("mouseGrabbed",mc.mouseHandler.isMouseGrabbed());row.addProperty("windowFocused",mc.isWindowActive());
        if(sprinting()&&com.codex.blacklight.client.camera.PrototypeChaseCamera.ownsView())violation("source chase entered isolated gait fixture at "+clock.tick());
        if(sprinting()&&(Math.abs(mc.player.getYRot())>.01F||Math.abs(row.get("movementYaw").getAsFloat())>.01F))
            violation("QA steering yaw changed at "+clock.tick());
        var nativeAction=mc.player.getData(ModAttachments.PLAYER_ACTION);
        row.addProperty("nativeAction",nativeAction.clipId());row.addProperty("nativeActionSeconds",nativeAction.seconds(mc.level.getGameTime()));
        var sampled=PlayerMotionController.pose(mc.player,1);var root=sampled.get("Root");
        if(root!=null){row.addProperty("bodyRootX",root.translation().x);row.addProperty("bodyRootY",root.translation().y);row.addProperty("bodyRootZ",root.translation().z);}
        if(stage==Stage.JUMP_SPRINT&&stageTicks>=20+jumpHoldTicks) {
            if(nativeAction.clipId().equals("basic_land_moving")||nativeAction.clipId().equals("basic_land_light"))movingLandingObserved=true;
            if(!mc.player.onGround()){if(firstJumpAirTick<0)firstJumpAirTick=clock.tick();jumpAirTicks++;}
            else if(firstJumpAirTick>=0&&jumpContactTick<0){jumpContactTick=clock.tick();capturePending=true;}
            row.addProperty("ticksSinceJumpContact",jumpContactTick<0?-1:clock.tick()-jumpContactTick);
            if(jumpContactTick>=0&&mc.player.onGround()&&clock.tick()-jumpContactTick<=30
                    &&(!LandingMotionPolicy.isLanding(nativeAction.clipId())||LandingMotionPolicy.permitsStride(nativeAction.clipId(),nativeAction.seconds(mc.level.getGameTime())))
                    &&!(clock.key().equals("basic_walk")||clock.key().equals("basic_run")||clock.key().startsWith("basic_sprint")))
                violation("landing stole ground stride at "+clock.tick()+": "+clock.key());
        }
        row.addProperty("armor",UtilityPowerSystem.active(mc.player,PowerForm.ARMOR));row.addProperty("onGround",mc.player.onGround());
        double nominal=MotionLibrary.nominalSpeed(clock.key())*mc.player.getScale();row.addProperty("nominalBlocksPerTick",nominal);
        if(previousTick!=null) {
            double dx=position.x-previousTick.get("x").getAsDouble(),dy=position.y-previousTick.get("y").getAsDouble(),dz=position.z-previousTick.get("z").getAsDouble();
            double distance=Math.hypot(dx,dz);row.addProperty("distance",distance);row.addProperty("verticalDistance",dy);
            long delta=clock.tick()-previousTick.get("tick").getAsLong();
            if(previousTick.get("key").getAsString().equals(clock.key())&&clock.looping()&&nominal>0&&distance<4&&delta>0) {
                double expected=Math.min(distance/(nominal*20),delta/20.0*8),actual=clock.current()-previousTick.get("currentSeconds").getAsDouble();
                row.addProperty("expectedPhaseAdvance",expected);row.addProperty("actualPhaseAdvance",actual);
                if(Math.abs(expected-actual)>.00001)violation("distance-clock mismatch at "+clock.tick()+": "+actual+" != "+expected);
                if(actual<-.000001)violation("clock rewound at "+clock.tick());
            }
        }
        // Repeated body/layer/camera samples must leave the committed gait unchanged.
        double before=clock.current();
        PlayerMotionController.pose(mc.player,.8F);PlayerMotionController.pose(mc.player,.2F);PlayerMotionController.pose(mc.player,.8F);
        if(before!=clock.current())violation("render sampling advanced gait at "+clock.tick());
        ticks.add(row);latestTick=clock.tick();previousTick=row;
    }
    private static void violation(String message){violations.add(message);BlacklightNeoMod.LOGGER.error("Continuous gait QA: {}",message);}
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void fov(ViewportEvent.ComputeFov event){if(active())event.setFOV(60);}
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void camera(ViewportEvent.ComputeCameraAngles event) {
        if(!active())return;var mc=Minecraft.getInstance();if(mc.player==null)return;
        var actor=mc.player.getPosition((float)event.getPartialTick());
        var eye=actor.add(5,2.2,4);var target=actor.add(0,.9,0);var look=target.subtract(eye);
        ((com.codex.blacklight.mixin.client.PrototypeCameraAccess)event.getCamera()).blacklight$setPosition(eye);
        event.setYaw((float)Math.toDegrees(Math.atan2(-look.x,look.z)));
        event.setPitch((float)-Math.toDegrees(Math.atan2(look.y,look.horizontalDistance())));event.setRoll(0);
    }
    @SubscribeEvent public static void beforeFrame(RenderFrameEvent.Pre event) {
        if(!active()||!serverReady)return;var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;
        mc.mouseHandler.releaseMouse();
        try {
            recordTick(mc);var runtime=runtime(mc);if(runtime==null)return;var clock=(LocomotionClock)gaitField.get(runtime);
            var partial=mc.getTimer().getGameTimeDeltaPartialTick(false);
            var pose=BlacklightRenderPose.sample(mc.player,partial);var skeleton=PlayerMotionController.skeleton();
            var model=new Matrix4f().rotationY((float)Math.toRadians(180-BlacklightRenderPose.facing(mc.player,partial))).scale(.9375F*mc.player.getScale());
            var row=new JsonObject();row.addProperty("stage",stage.name());row.addProperty("stageTick",stageTicks);row.addProperty("tick",clock.tick());row.addProperty("partial",partial);
            row.addProperty("key",clock.key());row.addProperty("sampleSeconds",clock.sampleTime(partial));
            row.addProperty("playerYaw",mc.player.getYRot());row.addProperty("playerPitch",mc.player.getXRot());
            var anchor=mc.player.getPosition(partial);row.add("actor",vector(anchor));
            for(String side:new String[]{"R","L"}) {
                var local=skeleton.getBoundTransformFor(pose,skeleton.searchJointByName("Leg_"+side)).transformPosition(new Vector3f(0,.37472F,0));
                model.transformPosition(local);row.add("foot_"+side,vector(anchor.add(local.x,local.y,local.z)));
            }
            frames.add(row);
            if(sprinting()&&(stageTicks==30||stageTicks==75||stageTicks==115))capturePending=true;
            if(stage==Stage.JUMP_SPRINT&&jumpContactTick>=0) {
                long age=clock.tick()-jumpContactTick;if(age==4||age==10||age==25)capturePending=true;
            }
            if(stage==Stage.DONE)capturePending=true;
        }catch(Exception e){fail(e);}
    }
    private static JsonArray vector(Vec3 v){var a=new JsonArray();a.add(v.x);a.add(v.y);a.add(v.z);return a;}
    @SubscribeEvent public static void frame(RenderFrameEvent.Post event) {
        if(!active()||!capturePending)return;capturePending=false;var mc=Minecraft.getInstance();
        String name=stage.name().toLowerCase(Locale.ROOT)+"_"+stageTicks+".png";
        try {
            if(captureNames.add(name))try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(output.resolve(name));}
            write(false,null);if(stage==Stage.DONE)finish(mc);
        }catch(Exception e){fail(e);}
    }
    private static void finish(Minecraft mc) {
        for(Stage phase:jumpOnly()?new Stage[]{}:new Stage[]{Stage.SPRINT,Stage.ARMOR_SPRINT}) {
            int count=0,running=0,grounded=0;double distance=0;
            for(var element:ticks) {
                var row=element.getAsJsonObject();if(!row.get("stage").getAsString().equals(phase.name()))continue;
                count++;if(row.get("phase").getAsString().equals("RUN"))running++;
                if(row.get("onGround").getAsBoolean())grounded++;
                if(row.has("distance"))distance+=row.get("distance").getAsDouble();
            }
            if(count<100)violation(phase+" captured only "+count+" committed ticks");
            if(distance<30)violation(phase+" native input did not produce continuous travel: "+distance+" blocks");
            if(running<60)violation(phase+" never established running gait: "+running+" ticks");
            if(grounded<count*.8)violation(phase+" left the flat lane: "+grounded+"/"+count+" grounded");
        }
        if(!movingLandingObserved)violation("native jump did not exercise the moving/light landing action");
        if(jumpAirTicks<5)violation("native Space input did not produce an observed jump: "+jumpAirTicks+" airborne ticks");
        if(jumpContactTick<0)violation("native jump never returned to real ground contact");
        else if(latestTick-jumpContactTick<30)violation("jump QA ended before 30 post-contact ticks");
        completed=true;restore(mc);write(true,null);
        BlacklightNeoMod.LOGGER.info("Continuous native gait QA completed {} committed ticks, {} real frames, {} violations",ticks.size(),frames.size(),violations.size());
        mc.stop();
    }
    private static void restore(Minecraft mc) {
        savedKeys.forEach(KeyMapping::setDown);
        if(mc.player!=null){mc.player.setYRot(previousYaw);mc.player.yRotO=previousYaw;mc.player.setXRot(previousPitch);mc.player.xRotO=previousPitch;}
        if(previousMouseGrabbed)mc.mouseHandler.grabMouse();else mc.mouseHandler.releaseMouse();
        if(previousCamera!=null){mc.options.setCameraType(previousCamera);mc.options.fov().set(previousFov);mc.options.hideGui=previousGui;mc.options.pauseOnLostFocus=previousPause;}
        mc.updateTitle();
    }
    private static void fail(Exception e) {
        if(disabled)return;disabled=true;BlacklightNeoMod.LOGGER.error("Continuous gait QA failed",e);
        var mc=Minecraft.getInstance();mc.execute(()->{restore(mc);write(false,e.toString());mc.stop();});
    }
    private static synchronized void write(boolean complete,String error) {
        if(output==null)return;
        try {
            var report=new JsonObject();report.addProperty("complete",complete);report.addProperty("source","real native KeyMapping input, packet/physics and Minecraft framebuffer");
            report.addProperty("passed",complete&&violations.isEmpty());report.addProperty("epicfightLoaded",ModList.get().isLoaded("epicfight"));report.addProperty("womLoaded",ModList.get().isLoaded("wom"));
            report.addProperty("fixedFov",60);report.addProperty("camera","constant actor-relative observer; own chase disabled by front perspective");
            report.addProperty("controls","native W/sprint; QA player yaw/pitch locked to zero before physics; pointer released and click recapture suppressed");
            report.addProperty("suppressedMouseEvents",suppressedMouseEvents);report.addProperty("suppressedPerspectiveClicks",suppressedPerspectiveClicks);
            report.addProperty("jumpOnly",jumpOnly());report.addProperty("firstJumpAirTick",firstJumpAirTick);report.addProperty("jumpContactTick",jumpContactTick);report.addProperty("jumpAirTicks",jumpAirTicks);
            report.addProperty("jumpHoldTicks",jumpHoldTicks);report.addProperty("movingLandingObserved",movingLandingObserved);
            report.addProperty("jumpInput","W+sprint140 ticks; Space held from tick20 for configured charge yielding a light impact, then released; actual airborne and real ground contact observed");
            report.add("committedTicks",ticks);report.add("renderFrames",frames);report.add("violations",violations);if(error!=null)report.addProperty("error",error);
            var images=new JsonArray();captureNames.stream().sorted().forEach(images::add);report.add("images",images);
            Path pending=output.resolve("continuous-gait-report.pending.json");Files.writeString(pending,new GsonBuilder().setPrettyPrinting().create().toJson(report));
            Files.move(pending,output.resolve("continuous-gait-report.json"),StandardCopyOption.REPLACE_EXISTING);
            String[] columns={"tick","stage","stageTick","phase","key","x","y","z","distance","currentSeconds","expectedPhaseAdvance","actualPhaseAdvance","playerYaw","playerPitch","movementYaw","observerYaw","yawBeforeInputLock","mouseGrabbed","windowFocused","onGround","armor","cameraType","chaseOwnsView","nativeAction","nativeActionSeconds","ticksSinceJumpContact","bodyRootX","bodyRootY","bodyRootZ"};
            var csv=new StringBuilder(String.join(",",columns)).append('\n');
            for(var element:ticks){var row=element.getAsJsonObject();for(int i=0;i<columns.length;i++){if(i>0)csv.append(',');var value=row.get(columns[i]);if(value!=null)csv.append(value.isJsonPrimitive()&&value.getAsJsonPrimitive().isString()?value.getAsString():value.toString());}csv.append('\n');}
            Files.writeString(output.resolve("continuous-gait-ticks.csv"),csv);
        }catch(Exception e){BlacklightNeoMod.LOGGER.error("Cannot write continuous gait evidence",e);}
    }
}
