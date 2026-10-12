package com.codex.blacklight.combat;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.HashMap;

/** Reviewed FIG Input hooks, expressed relative to the imported release start.
 * Recovery hooks include the reviewed release duration; a negative source event
 * sentinel is never converted into an immediate transition.
 */
final class SourceComboTiming {
    private record Window(float release, float hook) {}
    private static final Map<String, Window> WINDOWS = load();
    private SourceComboTiming() {}

    static int linkTick(String id, LocalWeaponMotion.Definition motion) {
        var window = WINDOWS.get(id);
        if (window == null || motion == null || motion.charge().isEmpty()) return -1;
        // A replacement developer clip must not inherit another clip's source clock.
        if (Math.abs(motion.charge().orElseThrow().releaseTime() - window.release()) > .001F) return -1;
        int tick = (int)Math.ceil((window.release() + window.hook() + motion.transitionSeconds()) * 20 - .0001);
        if (tick >= motion.durationTicks()) return -1;
        // Retain every active contact. Source hooks evaluate branches; they do not
        // authorize deleting the tail of a Minecraft damage window.
        return Math.max(motion.contactEndTick(), tick);
    }

    private static Map<String, Window> load() {
        try (var stream = SourceComboTiming.class.getResourceAsStream("/blacklight/source_combo_timing.json")) {
            if (stream == null) throw new IllegalStateException("Missing source combo timing");
            var root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.get("schema").getAsInt() != 1) throw new IllegalStateException("Unsupported combo timing schema");
            var result = new HashMap<String, Window>();
            for (var entry : root.getAsJsonObject("moves").entrySet()) {
                var value = entry.getValue().getAsJsonObject();
                float release = value.get("releaseTime").getAsFloat(), hook = value.get("inputHookSeconds").getAsFloat();
                if (!Float.isFinite(release) || !Float.isFinite(hook) || release < 0 || hook < 0 || release + hook > 10)
                    throw new IllegalStateException("Invalid source combo window: " + entry.getKey());
                result.put(entry.getKey(), new Window(release, hook));
            }
            return Map.copyOf(result);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Cannot read source combo timing", e);
        }
    }
}
