package com.codex.blacklight.client;

import com.codex.blacklight.BlacklightNeoMod;
import com.codex.blacklight.client.tuning.ClawTransformSettings;
import com.codex.blacklight.integration.epicfight.BlacklightEpicFightAnimations;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.client.animation.ClientAnimator;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import java.io.File;
import java.util.List;

/** Opt-in, development-only pose capture. Runs solely on a copied QA world. */
@EventBusSubscriber(modid = BlacklightNeoMod.MOD_ID, value = Dist.CLIENT)
public final class MotionPreviewCapture {
    private record Shot(String name, AnimationAccessor<? extends StaticAnimation> clip, float time) { }
    private static int tick, index = -1;
    private static boolean ready, captured, done;
    private static List<Shot> shots;
    private static CameraType previousCamera;
    private static int previousFov;
    private static boolean previousGui;
    private static boolean previousPause;
    private static final boolean SEQUENCE = Boolean.getBoolean("blacklight.test.motionSequence");
    private static final int INTERVAL = SEQUENCE ? 2 : 10;

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (done || System.getProperty("blacklight.test.motionCapture") == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) return;
        if (!mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toString().contains("Blacklight Motion QA")) {
            done = true;
            BlacklightNeoMod.LOGGER.error("Motion capture refused: requires a copied Blacklight Motion QA world");
            return;
        }
        var patch = EpicFightCapabilities.getEntityPatch(mc.player, LocalPlayerPatch.class);
        if (patch == null || ++tick < 80) return;
        if (shots == null) {
            new File(System.getProperty("blacklight.test.motionCapture"), "screenshots").mkdirs();
            previousPause = mc.options.pauseOnLostFocus;
            mc.options.pauseOnLostFocus = false;
            previousCamera = mc.options.getCameraType(); previousFov = mc.options.fov().get(); previousGui = mc.options.hideGui;
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT); mc.options.fov().set(55); mc.options.hideGui = true;
            ClawTransformSettings.setPreview(true);
            mc.level.setDayTime(6000);
            mc.getSingleplayerServer().execute(() -> mc.getSingleplayerServer().overworld().setDayTime(6000));
            shots = List.of(
                new Shot("01_idle", BlacklightEpicFightAnimations.CLAW_IDLE, .4F),
                new Shot("02_walk", BlacklightEpicFightAnimations.CLAW_WALK, .2F),
                new Shot("03_sprint", BlacklightEpicFightAnimations.CLAW_SPRINT, .13F),
                new Shot("04_combo1_wind", BlacklightEpicFightAnimations.CLAW_AUTO_1, .14F),
                new Shot("05_combo1_hit", BlacklightEpicFightAnimations.CLAW_AUTO_1, .20F),
                new Shot("06_combo1_follow", BlacklightEpicFightAnimations.CLAW_AUTO_1, .32F),
                new Shot("07_combo2_hit", BlacklightEpicFightAnimations.CLAW_AUTO_2, .25F),
                new Shot("08_combo3_wind", BlacklightEpicFightAnimations.CLAW_AUTO_3, .26F),
                new Shot("09_combo3_hit", BlacklightEpicFightAnimations.CLAW_AUTO_3, .35F),
                new Shot("10_dash_slice", BlacklightEpicFightAnimations.CLAW_DASHING_SLICE, .15F),
                new Shot("11_sprint_attack", BlacklightEpicFightAnimations.CLAW_SPRINT_ATTACK, .1F),
                new Shot("12_plant", BlacklightEpicFightAnimations.CLAW_GROUNDSPIKE, .55F),
                new Shot("13_charge", BlacklightEpicFightAnimations.CHARGED_JUMP, 2.0F),
                new Shot("14_rise", BlacklightEpicFightAnimations.JUMP_RISE, .12F),
                new Shot("15_fall", BlacklightEpicFightAnimations.JUMP_FALL, .16F),
                new Shot("16_air_dash", BlacklightEpicFightAnimations.AIR_DASH, .22F),
                new Shot("17_glide", BlacklightEpicFightAnimations.GLIDE, .35F),
                new Shot("18_wall", BlacklightEpicFightAnimations.WALL_RUN, .12F),
                new Shot("19_morph", BlacklightEpicFightAnimations.TRANSFORM_CLAWS, .45F),
                new Shot("20_consume", BlacklightEpicFightAnimations.CONSUME, 1.25F),
                new Shot("21_land_light", BlacklightEpicFightAnimations.LAND_LIGHT, .07F),
                new Shot("22_land_heavy", BlacklightEpicFightAnimations.LAND_HEAVY, .12F),
                new Shot("23_land_devastating", BlacklightEpicFightAnimations.LAND_DEVASTATING, .18F));
            var audit = new com.google.gson.JsonObject();
            var armature = yesman.epicfight.gameasset.Armatures.BIPED.get();
            for (Shot shot : shots) {
                var joints = new com.google.gson.JsonObject();
                var raw = shot.clip().get().getRawPose(shot.time());
                for (String joint : List.of("Arm_R", "Hand_R", "Tool_R", "Arm_L", "Hand_L", "Tool_L", "Root")) {
                    var matrix = armature.getBoundTransformFor(raw, armature.searchJointByName(joint));
                    var values = new com.google.gson.JsonArray();
                    values.add(matrix.m30); values.add(matrix.m31); values.add(matrix.m32);
                    joints.add(joint, values);
                }
                audit.add(shot.name(), joints);
            }
            try {
                java.nio.file.Files.writeString(java.nio.file.Path.of(System.getProperty("blacklight.test.motionCapture"), "joint-audit.json"), audit.toString());
            } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
            if (SEQUENCE) {
                var sequence = new java.util.ArrayList<Shot>();
                var seen = new java.util.HashSet<AnimationAccessor<? extends StaticAnimation>>();
                for (Shot base : shots) {
                    if (!seen.add(base.clip())) continue;
                    float duration = base.clip().get().getTotalTime();
                    int count = (int)Math.ceil(duration * 25);
                    for (int f = 0; f <= count; f++) sequence.add(new Shot(
                            base.name() + "_" + String.format("%04d", f), base.clip(), Math.min(f / 25.0F, duration)));
                }
                shots = sequence;
            }
        }
        if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) mc.setScreen(null);
        if ((tick - 80) % INTERVAL == 0 && (index < 0 || captured)) {
            index++;
            if (index >= shots.size()) {
                patch.getAnimator().setHardPause(false);
                ClawTransformSettings.setPreview(false);
                mc.options.setCameraType(previousCamera); mc.options.fov().set(previousFov); mc.options.hideGui = previousGui;
                mc.options.pauseOnLostFocus = previousPause;
                done = true;
                BlacklightNeoMod.LOGGER.info("Motion capture finished: {} shots", shots.size());
                mc.stop();
                return;
            }
            patch.getAnimator().setHardPause(false);
            // Frozen composite layers otherwise contaminate subsequent base-layer shots.
            ((ClientAnimator) patch.getAnimator()).getAllLayers().forEach(layer -> layer.disableLayer());
            patch.getAnimator().playAnimationInstantly(BlacklightEpicFightAnimations.CLAW_IDLE);
            patch.getAnimator().playAnimationInstantly(shots.get(index).clip());
            captured = false;
        }
        mc.player.setXRot(0); mc.player.setYRot(0); mc.player.yBodyRot = 0; mc.player.yHeadRot = 0;
        var shot = shots.get(index);
        patch.getAnimator().getPlayerFor(shot.clip()).setElapsedTime(shot.time(), shot.time());
        patch.getAnimator().setHardPause(true);
        ready = (tick - 80) % INTERVAL >= (SEQUENCE ? 1 : 5);
    }

    @SubscribeEvent
    public static void frame(RenderFrameEvent.Post event) {
        if (done || !ready || captured || shots == null) return;
        captured = true;
        var mc = Minecraft.getInstance();
        String name = shots.get(index).name() + ".png";
        Screenshot.grab(new File(System.getProperty("blacklight.test.motionCapture")), name,
                mc.getMainRenderTarget(), message -> BlacklightNeoMod.LOGGER.info("Motion capture {}: {}", name, message.getString()));
    }
}
