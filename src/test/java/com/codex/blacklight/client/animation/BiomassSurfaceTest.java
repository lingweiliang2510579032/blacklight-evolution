package com.codex.blacklight.client.animation;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BiomassSurfaceTest {
    private static final List<String> MODELS = List.of("claw_blender", "form_blade", "form_hammerfists", "form_muscle_mass", "form_whipfist");

    @Test void everyAuthoredWeaponVertexAndColorRemainExactAfterHardening() throws Exception {
        float[] result = new float[6];
        for (String model : MODELS) try (var stream = getClass().getResourceAsStream("/assets/blacklight/models/" + model + ".json")) {
            assertNotNull(stream);
            var joints = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("joints");
            for (var entry : joints.entrySet()) for (var element : entry.getValue().getAsJsonArray()) {
                var face = element.getAsJsonObject();
                var channels = face.getAsJsonArray("c");
                int color = 0xFF000000 | channels.get(0).getAsInt() << 16 | channels.get(1).getAsInt() << 8 | channels.get(2).getAsInt();
                for (var corner : face.getAsJsonArray("p")) {
                    var p = corner.getAsJsonArray();
                    float x = p.get(0).getAsFloat(), y = p.get(1).getAsFloat(), z = p.get(2).getAsFloat();
                    var vertex = BiomassSurface.vertex(x, y, z, 1.4F, entry.getKey().startsWith("Hand"));
                    BiomassSurface.deform(vertex, BiomassSurface.Frame.stable(1), 91.3F, x, y, z, result, 0);
                    assertEquals(Float.floatToIntBits(x), Float.floatToIntBits(result[0]), model);
                    assertEquals(Float.floatToIntBits(y), Float.floatToIntBits(result[1]), model);
                    assertEquals(Float.floatToIntBits(z), Float.floatToIntBits(result[2]), model);
                    assertEquals(color, BiomassSurface.color(color, result[3], result[4], result[5]), model);
                }
            }
        }
    }

    @Test void sharedVerticesStayWeldedAndBothJointCapsRemainFixed() throws Exception {
        var frame = new BiomassSurface.Frame(.53F, .6F, .97F, false);
        float[] result = new float[6];
        for (String model : MODELS) try (var stream = getClass().getResourceAsStream("/assets/blacklight/models/" + model + ".json")) {
            var joints = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("joints");
            for (var entry : joints.entrySet()) {
                var seen = new HashMap<String, float[]>();
                float length = .08F;
                for (var face : entry.getValue().getAsJsonArray()) for (var corner : face.getAsJsonObject().getAsJsonArray("p"))
                    length = Math.max(length, Math.abs(corner.getAsJsonArray().get(1).getAsFloat()));
                boolean forearm = entry.getKey().startsWith("Hand") || entry.getKey().startsWith("Tip");
                for (var face : entry.getValue().getAsJsonArray()) for (var corner : face.getAsJsonObject().getAsJsonArray("p")) {
                    var p = corner.getAsJsonArray();
                    float x = p.get(0).getAsFloat(), y = p.get(1).getAsFloat(), z = p.get(2).getAsFloat();
                    var vertex = BiomassSurface.vertex(x, y, z, length, forearm);
                    BiomassSurface.deform(vertex, frame, 47.23F, x, y, z, result, 0);
                    String key = x + "," + y + "," + z;
                    float[] previous = seen.putIfAbsent(key, result.clone());
                    if (previous != null) assertArrayEquals(previous, result, 0, model + "/" + entry.getKey());
                    if (Math.abs(y) <= .025F || !forearm && y >= length - .025F) {
                        assertEquals(x, result[0], 0); assertEquals(y, result[1], 0); assertEquals(z, result[2], 0);
                    }
                }
            }
        }
    }

    @Test void subtickFlowIsContinuousBoundedAndRetractionDoesNotSnap() {
        var vertex = BiomassSurface.vertex(.14F, .32F, -.045F, .8F, true);
        float[] a = new float[6], b = new float[6], reverse = new float[6];
        var frame = new BiomassSurface.Frame(.58F, .61F, .9F, false);
        BiomassSurface.deform(vertex, frame, 111.2F, vertex.x(), vertex.y(), vertex.z(), a, 0);
        BiomassSurface.deform(vertex, frame, 111.25F, vertex.x(), vertex.y(), vertex.z(), b, 0);
        BiomassSurface.deform(vertex, new BiomassSurface.Frame(frame.growth(), frame.phase(), frame.flow(), true),
                111.2F, vertex.x(), vertex.y(), vertex.z(), reverse, 0);
        assertArrayEquals(a, reverse, 0, "reversing the target must not move the current surface");
        double motion = distance(a, b), displacement = distance(a, new float[]{vertex.x(), vertex.y(), vertex.z()});
        assertTrue(motion > 1E-7 && motion < .001, "liquid must move at partial ticks without stepping");
        assertTrue(displacement > .001 && displacement < .045, "liquid must be visible without shifting the arm anchor");
        for (float value : a) assertTrue(Float.isFinite(value));
        assertFalse(new BiomassSurface.Frame(0, .2F, 1, false).visible());
    }

    private static double distance(float[] a, float[] b) {
        double x = a[0] - b[0], y = a[1] - b[1], z = a[2] - b[2];
        return Math.sqrt(x * x + y * y + z * z);
    }
}
