package com.codex.blacklight.combat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LocalWeaponMotionTest {
    static JsonObject motion() {
        var json = new JsonObject();
        var meta = new JsonObject();
        meta.addProperty("durationTicks", 21);
        meta.addProperty("contactStartTick", 4);
        meta.addProperty("contactEndTick", 8);
        meta.addProperty("transitionSeconds", .05);
        json.add("blacklight", meta);
        var tracks = new JsonArray();
        for (String name : new String[]{"Root", "Torso", "Chest", "Head", "Arm_R", "Arm_L", "Hand_R",
                "Hand_L", "Thigh_R", "Thigh_L", "Leg_R", "Leg_L", "Coord"}) {
            var track = new JsonObject();
            track.addProperty("name", name);
            var times = new JsonArray(); times.add(0); times.add(1);
            var transforms = new JsonArray();
            for (int frame = 0; frame < 2; frame++) {
                var matrix = new JsonArray();
                for (int index = 0; index < 16; index++) matrix.add(index % 5 == 0 ? 1 : 0);
                transforms.add(matrix);
            }
            track.add("time", times); track.add("transform", transforms); tracks.add(track);
        }
        json.add("animation", tracks);
        return json;
    }

    @Test void timingAndAnimationUseOneSnapshot() {
        var json = motion();
        var definition = LocalWeaponMotion.parse(json);
        json.getAsJsonObject("blacklight").addProperty("durationTicks", 90);
        assertEquals(21, definition.durationTicks());
        assertEquals(21, definition.animation().getAsJsonObject("blacklight").get("durationTicks").getAsInt());
        assertEquals(4, definition.contactStartTick());
    }

    @Test void rejectsTimingThatCutsOffTheMotion() {
        var json = motion(); json.getAsJsonObject("blacklight").addProperty("durationTicks", 16);
        assertThrows(IllegalArgumentException.class, () -> LocalWeaponMotion.parse(json));
    }

    @Test void separateContactWindowsKeepTheirGapsAndCannotOverlapOrEscapeBounds() {
        var json = motion();
        var meta = json.getAsJsonObject("blacklight");
        meta.add("contactWindows", com.google.gson.JsonParser.parseString(
                "[{\"startTick\":4,\"endTick\":5},{\"startTick\":7,\"endTick\":8}]"));
        var windows = LocalWeaponMotion.parse(json).contactWindows();
        assertTrue(windows.stream().anyMatch(window -> window.contains(4)));
        assertFalse(windows.stream().anyMatch(window -> window.contains(6)));
        assertFalse(windows.stream().anyMatch(window -> window.contains(8)));
        assertThrows(UnsupportedOperationException.class, () -> windows.clear());
        meta.getAsJsonArray("contactWindows").get(1).getAsJsonObject().addProperty("startTick", 4);
        assertThrows(IllegalArgumentException.class, () -> LocalWeaponMotion.parse(json));
        meta.add("contactWindows", new JsonArray());
        assertThrows(IllegalArgumentException.class, () -> LocalWeaponMotion.parse(json));
    }

    @Test void rejectsAbsentOrMisorderedCoordInsteadOfDoubleApplyingRootMotion() {
        var json = motion();
        var tracks = json.getAsJsonArray("animation");
        var coord = tracks.remove(tracks.size() - 1);
        assertThrows(IllegalArgumentException.class, () -> LocalWeaponMotion.parse(json));
        var reordered = new JsonArray(); reordered.add(coord); tracks.forEach(reordered::add);
        json.add("animation", reordered);
        assertThrows(IllegalArgumentException.class, () -> LocalWeaponMotion.parse(json));
    }

    @Test void bladeBoundsRejectDistantTargetsAndRequireEveryActiveTick() {
        var bounds = new LocalWeaponMotion.ContactBounds(-.5,0,0,.5,2,1);
        assertTrue(bounds.intersects(0,1,1.2,.3,.95,.3));
        assertFalse(bounds.intersects(0,1,2,.3,.95,.3), "a long reach cone alone must not deal an early hit");
        assertFalse(bounds.intersects(2,1,1,.3,.95,.3));
        assertFalse(bounds.intersects(0,4,1,.3,.95,.3));
        var json=motion();var meta=json.getAsJsonObject("blacklight");
        meta.add("contactBounds",com.google.gson.JsonParser.parseString(
                "[{\"tick\":4,\"min\":[-1,0,0],\"max\":[1,2,1]}]"));
        assertThrows(IllegalArgumentException.class,()->LocalWeaponMotion.parse(json));
        var entries=meta.getAsJsonArray("contactBounds");
        for(int tick=5;tick<8;tick++){
            var entry=entries.get(0).deepCopy().getAsJsonObject();entry.addProperty("tick",tick);entries.add(entry);
        }
        assertEquals(4,LocalWeaponMotion.parse(json).contactBounds().size());
        entries.get(1).getAsJsonObject().getAsJsonArray("max").set(0,new com.google.gson.JsonPrimitive(-2));
        assertThrows(IllegalArgumentException.class,()->LocalWeaponMotion.parse(json));
    }

    @Test void rejectsInvalidCurvesAndUnsynchronizedFeetAndRoot() {
        var json = motion();
        json.getAsJsonArray("animation").get(0).getAsJsonObject().getAsJsonArray("time").set(1, new com.google.gson.JsonPrimitive(0));
        assertThrows(IllegalArgumentException.class, () -> LocalWeaponMotion.parse(json));
        var truncated = motion();
        truncated.getAsJsonArray("animation").get(10).getAsJsonObject().getAsJsonArray("time").set(1, new com.google.gson.JsonPrimitive(.5));
        assertThrows(IllegalArgumentException.class, () -> LocalWeaponMotion.parse(truncated));
    }

    @Test void chargeUsesTheSameReleaseClockAndRejectsInvalidIdsAndFractionalTicks() {
        var json = motion();
        var charge = com.google.gson.JsonParser.parseString("""
                {"windupTicks":2,"holdTicks":7,"releaseTime":0.1,"animation":"blade_cut_charge","chargedMove":"blade_cut_charged"}
                """).getAsJsonObject();
        json.getAsJsonObject("blacklight").add("charge", charge);
        var parsed = LocalWeaponMotion.parse(json).charge().orElseThrow();
        assertEquals(2, parsed.windupTicks());
        assertEquals("blade_cut_charged", parsed.chargedMove().orElseThrow());
        charge.addProperty("windupTicks", 2.5);
        assertThrows(IllegalArgumentException.class, () -> LocalWeaponMotion.parse(json));
        charge.addProperty("windupTicks", 2);
        charge.addProperty("animation", "../blade_cut");
        assertThrows(IllegalArgumentException.class, () -> LocalWeaponMotion.parse(json));
        charge.addProperty("animation", "blade_cut_charge");
        charge.addProperty("releaseTime", 1);
        assertThrows(IllegalArgumentException.class, () -> LocalWeaponMotion.parse(json));
        charge.addProperty("releaseTime", .1);
        charge.addProperty("windupTicks", 5);
        assertThrows(IllegalArgumentException.class, () -> LocalWeaponMotion.parse(json));
    }
}
