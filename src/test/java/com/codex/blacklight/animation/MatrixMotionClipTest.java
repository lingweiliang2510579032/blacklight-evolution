package com.codex.blacklight.animation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MatrixMotionClipTest {
    private static JsonObject clip(Matrix4f start, Matrix4f end) {
        var json = new JsonObject();
        json.addProperty("format", "MATRIX");
        var tracks = new JsonArray();
        var track = new JsonObject();
        track.addProperty("name", "Root");
        var times = new JsonArray(); times.add(0); times.add(2);
        var transforms = new JsonArray();
        for (var matrix : new Matrix4f[]{start, end}) {
            var values = new JsonArray();
            for (float f : new Matrix4f(matrix).transpose().get(new float[16])) values.add(f);
            transforms.add(values);
        }
        track.add("time", times); track.add("transform", transforms);
        tracks.add(track); json.add("animation", tracks);
        return json;
    }

    @Test void rowMajorTranslationAndQuaternionInterpolationPreserveLimbLength() {
        var motion = MatrixMotionClip.parse(clip(new Matrix4f(),
                new Matrix4f().translation(4, 2, -6).rotateZ((float) Math.PI / 2)));
        var mid = motion.sample("Root", 1, false);
        assertEquals(2, mid.m30(), .0001);
        assertEquals(1, mid.m31(), .0001);
        assertEquals(-3, mid.m32(), .0001);
        var direction = mid.transformDirection(new Vector3f(1, 0, 0));
        assertEquals(1, direction.length(), .0001);
        assertEquals(Math.sqrt(.5), direction.x, .0001);
        assertEquals(Math.sqrt(.5), direction.y, .0001);
    }

    @Test void sampleIsSeekableLoopsAndCannotMutateTheSourceClip() {
        var json = clip(new Matrix4f(), new Matrix4f().translation(4, 0, 0));
        var motion = MatrixMotionClip.parse(json);
        json.getAsJsonArray("animation").get(0).getAsJsonObject().getAsJsonArray("time").set(1, new com.google.gson.JsonPrimitive(10));
        assertEquals(4, motion.sample("Root", 200, false).m30(), .0001);
        assertEquals(2, motion.sample("Root", 3, true).m30(), .0001);
        assertEquals(0, motion.sample("Root", -1, true).m30(), .0001);
        motion.sample("Root", 1, false).translation(999, 999, 999);
        assertEquals(2, motion.sample("Root", 1, false).m30(), .0001);
        assertEquals(0, motion.sample("Root", 2, true).m30(), .0001);
    }

    @Test void scaledAndMirroredBonesRetainTheirTransforms() {
        var matrix = new Matrix4f().translation(3, 2, 1).rotateY(.7F).scale(-2, 3, 4);
        var motion = MatrixMotionClip.parse(clip(matrix, matrix));
        assertTrue(matrix.equals(motion.sample("Root", 1, false), .0001F));
    }

    @Test void rejectsInvalidClocksTransformsAndAmbiguousDuplicateBones() {
        var duplicate = clip(new Matrix4f(), new Matrix4f());
        duplicate.getAsJsonArray("animation").add(duplicate.getAsJsonArray("animation").get(0).deepCopy());
        assertThrows(IllegalArgumentException.class, () -> MatrixMotionClip.parse(duplicate));
        var invalidTime = clip(new Matrix4f(), new Matrix4f());
        invalidTime.getAsJsonArray("animation").get(0).getAsJsonObject().getAsJsonArray("time").set(1, new com.google.gson.JsonPrimitive(0));
        assertThrows(IllegalArgumentException.class, () -> MatrixMotionClip.parse(invalidTime));
        assertThrows(IllegalArgumentException.class, () -> MatrixMotionClip.parse(clip(new Matrix4f().scale(0), new Matrix4f())));
        assertThrows(IllegalArgumentException.class, () -> MatrixMotionClip.parse(clip(new Matrix4f().m01(.4F), new Matrix4f())));
        assertThrows(IllegalArgumentException.class, () -> MatrixMotionClip.parse(clip(new Matrix4f().m00(Float.NaN), new Matrix4f())));
        var motion = MatrixMotionClip.parse(clip(new Matrix4f(), new Matrix4f()));
        assertThrows(IllegalArgumentException.class, () -> motion.sample("Root", Float.NaN, false));
        assertThrows(IllegalArgumentException.class, () -> motion.sample("missing", 0, false));
    }
}
