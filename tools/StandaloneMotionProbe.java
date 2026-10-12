import com.codex.blacklight.animation.MatrixMotionClip;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipFile;

/** Run with only Gson, JOML and the new sampler on the classpath (no game or Epic Fight). */
public final class StandaloneMotionProbe {
    public static void main(String[] args) throws Exception {
        try {
            Class.forName("yesman.epicfight.api.animation.Pose");
            throw new IllegalStateException("Epic Fight must be absent from the probe classpath");
        } catch (ClassNotFoundException expected) { }
        int clips = 0, tracks = 0, samples = 0, failures = 0;
        boolean armorTransform = false, armorIdle = false;
        try (var archive = new ZipFile(args[0])) {
            var entries = archive.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (!entry.getName().startsWith("blacklight/bundled_motions/") || !entry.getName().endsWith(".json")) continue;
                try (var reader = new InputStreamReader(archive.getInputStream(entry), StandardCharsets.UTF_8)) {
                    var json = JsonParser.parseReader(reader).getAsJsonObject();
                    if (!json.has("animation")) continue;
                    var clip = MatrixMotionClip.parse(json);
                    for (var element : json.getAsJsonArray("animation")) {
                        var track = element.getAsJsonObject();
                        String name = track.get("name").getAsString();
                        var times = track.getAsJsonArray("time");
                        var matrices = track.getAsJsonArray("transform");
                        for (int i = 0; i < times.size(); i++) {
                            float time = times.get(i).getAsFloat();
                            float[] actual = clip.sample(name, time, false).transpose().get(new float[16]);
                            var expected = matrices.get(i).getAsJsonArray();
                            for (int j = 0; j < 16; j++) {
                                if (Math.abs(actual[j] - expected.get(j).getAsFloat()) > .001F)
                                    throw new IllegalStateException("Keyframe changed: " + name + " frame=" + i + " component=" + j);
                            }
                            if (i > 0) {
                                var between = clip.sample(name, (time + times.get(i - 1).getAsFloat()) / 2, false);
                                if (!between.isFinite()) throw new IllegalStateException("Non-finite interpolation: " + name);
                                samples++;
                            }
                            samples++;
                        }
                        tracks++;
                    }
                    clips++;
                    armorTransform |= entry.getName().endsWith("/utility_armor_transform.json");
                    armorIdle |= entry.getName().endsWith("/utility_armor_idle.json");
                } catch (RuntimeException problem) {
                    failures++;
                    System.err.println(entry.getName() + ": " + problem.getMessage());
                }
            }
        }
        System.out.printf("clips=%d tracks=%d samples=%d failures=%d armorTransform=%s armorIdle=%s epicFightAbsent=true%n",
                clips, tracks, samples, failures, armorTransform, armorIdle);
        if (clips == 0 || failures != 0 || !armorTransform || !armorIdle) System.exit(1);
    }
}
