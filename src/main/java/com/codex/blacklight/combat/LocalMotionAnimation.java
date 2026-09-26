package com.codex.blacklight.combat;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashSet;
import java.util.Set;

/** Shared structural validation for local combat and visual-only clips. */
final class LocalMotionAnimation {
    private static final Set<String> JOINTS = Set.of("Root", "Torso", "Chest", "Head", "Shoulder_R",
            "Shoulder_L", "Arm_R", "Arm_L", "Hand_R", "Hand_L", "Tool_R", "Tool_L", "Elbow_R",
            "Elbow_L", "Thigh_R", "Thigh_L", "Leg_R", "Leg_L", "Knee_R", "Knee_L", "Coord");

    private LocalMotionAnimation() {}

    static int integer(JsonObject object, String field) {
        JsonElement element = object.get(field);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber())
            throw new IllegalArgumentException("Expected integer: " + field);
        double value = element.getAsDouble();
        if (!Double.isFinite(value) || value != Math.rint(value) || value < Integer.MIN_VALUE || value > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Expected finite integer: " + field);
        return (int) value;
    }

    static String id(JsonObject object, String field) {
        JsonElement element = object.get(field);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())
            throw new IllegalArgumentException("Expected motion id: " + field);
        String value = element.getAsString();
        if (!validId(value)) throw new IllegalArgumentException("Invalid motion id: " + value);
        return value;
    }

    static boolean validId(String value) { return value != null && value.matches("[a-z0-9_]{1,80}"); }

    static float validate(JsonObject json, int duration, float transition, boolean requireFormat) {
        if (duration < 2 || duration > 200 || !Float.isFinite(transition) || transition < 0 || transition > .15F)
            throw new IllegalArgumentException("Invalid local animation duration or transition");
        if ((requireFormat && !json.has("format"))
                || (json.has("format") && !json.get("format").getAsString().equalsIgnoreCase("MATRIX")))
            throw new IllegalArgumentException("Local motion must use MATRIX transforms");
        if (!json.has("animation") || !json.get("animation").isJsonArray())
            throw new IllegalArgumentException("Missing local animation tracks");
        var tracks = json.getAsJsonArray("animation");
        var names = new HashSet<String>();
        float clipEnd = 0;
        for (int index = 0; index < tracks.size(); index++) {
            var track = tracks.get(index).getAsJsonObject();
            String name = track.get("name").getAsString();
            if (!JOINTS.contains(name) || !names.add(name)) throw new IllegalArgumentException("Unknown or duplicate joint: " + name);
            if (index == 0 && !name.equals("Root")) throw new IllegalArgumentException("Root must be first");
            if (name.equals("Coord") && index != tracks.size() - 1) throw new IllegalArgumentException("Coord must be last");
            var times = track.getAsJsonArray("time");
            var transforms = track.getAsJsonArray("transform");
            if (times.size() < 2 || times.size() > 2401 || times.size() != transforms.size())
                throw new IllegalArgumentException("Invalid keyframe count for " + name);
            float previous = -1;
            for (int frame = 0; frame < times.size(); frame++) {
                float time = times.get(frame).getAsFloat();
                if (!Float.isFinite(time) || time < 0 || time <= previous || time > 10)
                    throw new IllegalArgumentException("Invalid keyframe time for " + name);
                if (frame == 0 && time != 0) throw new IllegalArgumentException("Track must begin at zero: " + name);
                previous = time;
                var matrix = transforms.get(frame).getAsJsonArray();
                if (matrix.size() != 16) throw new IllegalArgumentException("Expected 4x4 matrix: " + name);
                for (var value : matrix) if (!Float.isFinite(value.getAsFloat()))
                    throw new IllegalArgumentException("Non-finite transform: " + name);
                if (Math.abs(matrix.get(12).getAsDouble()) > .0001 || Math.abs(matrix.get(13).getAsDouble()) > .0001
                        || Math.abs(matrix.get(14).getAsDouble()) > .0001 || Math.abs(matrix.get(15).getAsDouble() - 1) > .0001)
                    throw new IllegalArgumentException("Expected affine transform: " + name);
            }
            if (clipEnd != 0 && Math.abs(previous - clipEnd) > .001F)
                throw new IllegalArgumentException("Joint and movement tracks must end together");
            clipEnd = previous;
        }
        if (!names.containsAll(Set.of("Root", "Torso", "Chest", "Head", "Arm_R", "Arm_L", "Hand_R", "Hand_L",
                "Thigh_R", "Thigh_L", "Leg_R", "Leg_L", "Coord")))
            throw new IllegalArgumentException("Incomplete full-body motion or missing Coord");
        float finish = clipEnd + transition;
        if (duration / 20F + .001F < finish || duration / 20F - finish > .101F)
            throw new IllegalArgumentException("Server duration does not match the motion and link");
        return clipEnd;
    }
}
