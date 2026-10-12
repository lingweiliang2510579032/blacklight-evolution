package com.codex.blacklight.animation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NativeRootMotionTest {
    private static MatrixMotionClip motion() {
        var json = new JsonObject();
        json.addProperty("format", "MATRIX");
        var track = new JsonObject(); track.addProperty("name", "Coord");
        var clock = new JsonArray(); clock.add(0); clock.add(1);
        var frames = new JsonArray();
        for (var matrix : new Matrix4f[]{new Matrix4f(), new Matrix4f().translation(1, 2, 9)}) {
            var frame = new JsonArray();
            for (float value : new Matrix4f(matrix).transpose().get(new float[16])) frame.add(value);
            frames.add(frame);
        }
        track.add("time", clock); track.add("transform", frames);
        var tracks = new JsonArray(); tracks.add(track); json.add("animation", tracks);
        return MatrixMotionClip.parse(json);
    }

    @Test void coordinatesUseTheContactBoxBasisAndDoNotApplyTheAuthoredVerticalReference() {
        var north = NativeRootMotion.clipStep(motion(), 0, 1, 0);
        assertEquals(-1, north.x, .00001); assertEquals(0, north.y); assertEquals(2, north.z, .00001);
        var west = NativeRootMotion.clipStep(motion(), 0, 1, 90);
        assertEquals(-2, west.x, .00001); assertEquals(0, west.y); assertEquals(-1, west.z, .00001);
    }

    @Test void splitTicksAndReleaseSeekingApplyEachCentimetreOnlyOnce() {
        var clip = motion(); var total = Vec3.ZERO;
        for (int tick = 1; tick <= 20; tick++)
            total = total.add(NativeRootMotion.clipStep(clip, (tick - 1) / 20F, tick / 20F, 35));
        assertEquals(0, total.distanceTo(NativeRootMotion.clipStep(clip, 0, 1, 35)), .00001);
        var release = NativeRootMotion.clipStep(clip, .4F, 1, 0);
        assertEquals(.6, -release.x, .00001); assertEquals(1.2, release.z, .00001);
        assertEquals(Vec3.ZERO, NativeRootMotion.clipStep(clip, 1, .4F, 0));
    }

    @Test void standOffClampsOnlyTheAdvanceAndKeepsTheLateralStep() {
        var clamped = NativeRootMotion.standOff(new Vec3(.4, 0, 1), new Vec3(0, 0, 4), .25);
        assertEquals(.4, clamped.x, .00001); assertEquals(.25, clamped.z, .00001);
        var touching = NativeRootMotion.standOff(new Vec3(.4, 0, 1), new Vec3(0, 0, 4), -1);
        assertEquals(.4, touching.x, .00001); assertEquals(0, touching.z, .00001);
        var retreat = new Vec3(.4, 0, -1);
        assertEquals(retreat, NativeRootMotion.standOff(retreat, new Vec3(0, 0, 4), 0));
    }

    @Test void fractionalReleaseSeeksTheSameSubTickClockAsTheWeaponContactSweep() {
        float release=1F/6;
        assertEquals(release,NativeRootMotion.weaponClock(3,release,.05F),.00001);
        assertEquals(release+.05F,NativeRootMotion.weaponClock(5,release,.05F),.00001);
        assertEquals(release+.1F,NativeRootMotion.weaponClock(6,release,.05F),.00001);
    }

    @Test void installedAttackForwardTrackUsesBlenderYAndRetainsTheAuthoredLunge() {
        var installed=com.codex.blacklight.combat.MotionResources.read("blade_cut.json",8*1024*1024);
        org.junit.jupiter.api.Assumptions.assumeTrue(installed.isPresent(),"requires bundled local motion profile");
        var clip=MatrixMotionClip.parse(installed.orElseThrow());
        var from=clip.sample("Coord",0,false);var to=clip.sample("Coord",clip.duration(),false);
        double authoredAdvance=to.m31()-from.m31();
        assertTrue(authoredAdvance>.5,"fixture must contain a real forward lunge");
        var moved=NativeRootMotion.clipStep(clip,0,clip.duration(),0);
        assertEquals(authoredAdvance,moved.z,.00001);
        assertEquals(0,moved.y);
    }
}
