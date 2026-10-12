package com.codex.blacklight.combat;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LocalVisualMotionTest {
    static JsonObject visual() {
        var json = LocalWeaponMotionTest.motion();
        json.addProperty("format", "MATRIX");
        json.getAsJsonObject("blacklight").remove("contactStartTick");
        json.getAsJsonObject("blacklight").remove("contactEndTick");
        return json;
    }

    @Test void visualOnlyClipsCannotSmuggleCombatWindowsOrRecursiveCharges() {
        var json = visual();
        assertEquals(21, LocalVisualMotion.parse(json).durationTicks());
        for (String field : new String[]{"contactStartTick", "contactEndTick", "contactWindows", "contactBounds", "charge"}) {
            var invalid = json.deepCopy();
            invalid.getAsJsonObject("blacklight").addProperty(field, 4);
            assertThrows(IllegalArgumentException.class, () -> LocalVisualMotion.parse(invalid), field);
        }
        json.remove("format");
        assertThrows(IllegalArgumentException.class, () -> LocalVisualMotion.parse(json));
    }

    @Test void visualLoaderRequiresSynchronizedKnownAffineJointTracks() {
        var unknown = visual();
        unknown.getAsJsonArray("animation").get(1).getAsJsonObject().addProperty("name", "UnknownJoint");
        assertThrows(IllegalArgumentException.class, () -> LocalVisualMotion.parse(unknown));
        var projective = visual();
        projective.getAsJsonArray("animation").get(0).getAsJsonObject().getAsJsonArray("transform")
                .get(0).getAsJsonArray().set(15, new com.google.gson.JsonPrimitive(0));
        assertThrows(IllegalArgumentException.class, () -> LocalVisualMotion.parse(projective));
        var overflow = visual();
        overflow.getAsJsonArray("animation").get(0).getAsJsonObject().getAsJsonArray("transform")
                .get(0).getAsJsonArray().set(0, new com.google.gson.JsonPrimitive(1e80));
        assertThrows(IllegalArgumentException.class, () -> LocalVisualMotion.parse(overflow));
        var original = visual(); var snapshot = LocalVisualMotion.parse(original);
        original.getAsJsonObject("blacklight").addProperty("durationTicks", 100);
        assertEquals(21, snapshot.animation().getAsJsonObject("blacklight").get("durationTicks").getAsInt());
    }
}
