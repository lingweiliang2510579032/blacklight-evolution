package com.codex.blacklight.animation;

import com.google.gson.JsonObject;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Engine-independent sampler for Blacklight's row-major MATRIX motion files. */
public final class MatrixMotionClip {
    private record Frame(Vector3f translation, Quaternionf rotation, Vector3f scale) {
        Matrix4f interpolate(Frame other, float alpha) {
            return new Matrix4f().translationRotateScale(
                    new Vector3f(translation).lerp(other.translation, alpha),
                    new Quaternionf(rotation).slerp(other.rotation, alpha).normalize(),
                    new Vector3f(scale).lerp(other.scale, alpha));
        }
    }

    private record Track(float[] times, Frame[] frames) {
        Matrix4f sample(float seconds) {
            int at = Arrays.binarySearch(times, seconds);
            if (at >= 0) return frames[at].interpolate(frames[at], 0);
            int right = -at - 1;
            if (right == 0) return frames[0].interpolate(frames[0], 0);
            if (right == times.length) return frames[right - 1].interpolate(frames[right - 1], 0);
            float alpha = (seconds - times[right - 1]) / (times[right] - times[right - 1]);
            return frames[right - 1].interpolate(frames[right], alpha);
        }
    }

    private final Map<String, Track> tracks;
    private final float duration;

    private MatrixMotionClip(Map<String, Track> tracks, float duration) {
        this.tracks = Map.copyOf(tracks);
        this.duration = duration;
    }

    public float duration() { return duration; }
    public Set<String> joints() { return tracks.keySet(); }

    public Matrix4f sample(String joint, float seconds, boolean loop) {
        if (!Float.isFinite(seconds)) throw new IllegalArgumentException("Non-finite sample time");
        var track = tracks.get(joint);
        if (track == null) throw new IllegalArgumentException("Missing joint: " + joint);
        // A negative playback offset starts at the first frame, never wraps backwards.
        float time = Math.max(0, seconds);
        if (loop && duration > 0) time %= duration;
        else time = Math.min(time, duration);
        return track.sample(time);
    }

    public Map<String, Matrix4f> sample(float seconds, boolean loop) {
        var pose = new LinkedHashMap<String, Matrix4f>();
        for (String joint : tracks.keySet()) pose.put(joint, sample(joint, seconds, loop));
        return pose;
    }

    public static MatrixMotionClip parse(JsonObject json) {
        if (json.has("format") && !"MATRIX".equalsIgnoreCase(json.get("format").getAsString()))
            throw new IllegalArgumentException("Expected MATRIX motion");
        var animation = json.getAsJsonArray("animation");
        if (animation == null || animation.isEmpty()) throw new IllegalArgumentException("Missing tracks");
        var tracks = new LinkedHashMap<String, Track>();
        float duration = 0;
        for (var element : animation) {
            var track = element.getAsJsonObject();
            String name = track.get("name").getAsString();
            if (name.isBlank() || tracks.containsKey(name)) throw new IllegalArgumentException("Duplicate or empty joint: " + name);
            var times = track.getAsJsonArray("time");
            var transforms = track.getAsJsonArray("transform");
            if (times == null || transforms == null || times.isEmpty() || times.size() != transforms.size())
                throw new IllegalArgumentException("Mismatched frames: " + name);
            float[] clock = new float[times.size()];
            Frame[] frames = new Frame[times.size()];
            for (int index = 0; index < times.size(); index++) {
                float time = times.get(index).getAsFloat();
                if (!Float.isFinite(time) || time < 0 || index == 0 && time != 0
                        || index > 0 && time <= clock[index - 1]) throw new IllegalArgumentException("Invalid clock: " + name);
                clock[index] = time;
                var values = transforms.get(index).getAsJsonArray();
                if (values.size() != 16) throw new IllegalArgumentException("Expected 4x4 matrix: " + name);
                float[] matrix = new float[16];
                for (int i = 0; i < matrix.length; i++) {
                    matrix[i] = values.get(i).getAsFloat();
                    if (!Float.isFinite(matrix[i])) throw new IllegalArgumentException("Non-finite matrix: " + name);
                }
                if (Math.abs(matrix[12]) > .0001F || Math.abs(matrix[13]) > .0001F
                        || Math.abs(matrix[14]) > .0001F || Math.abs(matrix[15] - 1) > .0001F)
                    throw new IllegalArgumentException("Non-affine matrix: " + name);
                // JOML reads column-major arrays; authored files store rows.
                var transform = new Matrix4f().set(matrix).transpose();
                var scale = transform.getScale(new Vector3f());
                if (scale.x < 1e-6F || scale.y < 1e-6F || scale.z < 1e-6F)
                    throw new IllegalArgumentException("Singular transform: " + name);
                // Preserve reflected transforms instead of turning them into rotations.
                if (transform.determinant3x3() < 0) scale.x = -scale.x;
                var rotationMatrix = new Matrix4f(transform).scale(1 / scale.x, 1 / scale.y, 1 / scale.z);
                var rotation = rotationMatrix.getNormalizedRotation(new Quaternionf()).normalize();
                var frame = new Frame(transform.getTranslation(new Vector3f()), rotation, scale);
                if (!transform.equals(frame.interpolate(frame, 0), .001F))
                    throw new IllegalArgumentException("Sheared transform cannot be interpolated as TRS: " + name);
                frames[index] = frame;
            }
            duration = Math.max(duration, clock[clock.length - 1]);
            tracks.put(name, new Track(clock, frames));
        }
        return new MatrixMotionClip(tracks, duration);
    }
}
