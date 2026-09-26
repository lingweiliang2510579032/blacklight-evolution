package com.codex.blacklight.client.animation;

import com.codex.blacklight.combat.MotionResources;
import com.google.gson.JsonArray;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import java.util.HashMap;
import java.util.Map;

/** Native shield attachment motion, independent of the animated wrist joint. */
public final class NativeShieldGrip {
    private record Frame(Vector3f position, Quaternionf rotation, Vector3f scale) { }
    private record Track(float fps, Frame[] frames) { }
    private static final Map<String, Track> tracks = new HashMap<>();
    private static Vector3f vector(JsonArray a) {
        return new Vector3f(a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat());
    }
    private static Track load(String key) {
        var root = MotionResources.read("utility_" + key + ".json", 8_000_000)
                .orElseThrow(() -> new IllegalStateException("Missing shield motion: " + key));
        if (!root.has("shieldGrip")) throw new IllegalStateException("Motion profile predates shield grip tracks: " + key);
        var grip = root.getAsJsonObject("shieldGrip");
        float fps = grip.get("fps").getAsFloat();
        var data = grip.getAsJsonArray("frames");
        if (!(fps > 0 && Float.isFinite(fps)) || data.isEmpty()) throw new IllegalStateException("Invalid shield grip: " + key);
        Frame[] frames = new Frame[data.size()];
        for (int i = 0; i < frames.length; i++) {
            var f = data.get(i).getAsJsonObject(); var q = f.getAsJsonArray("rot");
            frames[i] = new Frame(vector(f.getAsJsonArray("loc")),
                    new Quaternionf(q.get(1).getAsFloat(), q.get(2).getAsFloat(), q.get(3).getAsFloat(), q.get(0).getAsFloat()).normalize(),
                    vector(f.getAsJsonArray("sca")));
        }
        return new Track(fps, frames);
    }
    public static void apply(LivingEntityPatch<?> patch, PoseStack stack, float partial) {
        var selected = NativeUtilityPose.select(patch, partial);
        if (selected == null || selected.plated()) return;
        var track = tracks.computeIfAbsent(selected.key(), NativeShieldGrip::load);
        float frame = Math.clamp(selected.time() * track.fps, 0, track.frames.length - 1);
        int index = (int)frame;
        var a = track.frames[index]; var b = track.frames[Math.min(index + 1, track.frames.length - 1)];
        float u = frame - index, weight = selected.weight();
        var position = new Vector3f(a.position).lerp(b.position, u).mul(weight);
        var rotation = new Quaternionf().slerp(new Quaternionf(a.rotation).slerp(b.rotation, u), weight);
        var scale = new Vector3f(1).lerp(new Vector3f(a.scale).lerp(b.scale, u), weight);
        stack.translate(position.x, position.y, position.z);
        stack.mulPose(rotation);
        stack.scale(scale.x, scale.y, scale.z);
    }
}
