package com.codex.blacklight.client.tuning;

import com.codex.blacklight.BlacklightNeoMod;
import net.minecraftforge.fml.loading.FMLPaths;
import org.joml.Quaternionf;
import com.codex.blacklight.animation.BoneTransform;
import com.codex.blacklight.animation.BlacklightPose;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Properties;
import java.util.TreeMap;

/** Full position/rotation/scale keyframes used to author the final playable clips. */
public final class AnimationPoseSettings {
    public enum Action {
        CHARGED_JUMP("charged_jump", "蓄力跳跃", 2.05F, 1.0F),
        GROUND_SPIKE("ground_spike", "利爪地刺", 2.20F, 2.0F);

        private final String key;
        private final String displayName;
        private final float totalTime;
        private final float runtimeSpeed;

        Action(String key, String displayName, float totalTime, float runtimeSpeed) {
            this.key = key;
            this.displayName = displayName;
            this.totalTime = totalTime;
            this.runtimeSpeed = runtimeSpeed;
        }

        public String key() { return key; }
        public String displayName() { return displayName; }
        public float totalTime() { return totalTime; }
        public float runtimeSpeed() { return runtimeSpeed; }
    }

    /** All 20 joints authored in both Prototype player clips. */
    public enum Joint {
        ROOT("Root", "根节点"), TORSO("Torso", "躯干"), CHEST("Chest", "胸腔"),
        HEAD("Head", "头部"), SHOULDER_R("Shoulder_R", "右肩"),
        SHOULDER_L("Shoulder_L", "左肩"), ARM_R("Arm_R", "右上臂"),
        ARM_L("Arm_L", "左上臂"), ELBOW_R("Elbow_R", "右肘"),
        ELBOW_L("Elbow_L", "左肘"), HAND_R("Hand_R", "右手"),
        HAND_L("Hand_L", "左手"), TOOL_R("Tool_R", "右武器点"),
        TOOL_L("Tool_L", "左武器点"), THIGH_R("Thigh_R", "右大腿"),
        THIGH_L("Thigh_L", "左大腿"), LEG_R("Leg_R", "右小腿"),
        LEG_L("Leg_L", "左小腿"), KNEE_R("Knee_R", "右膝脚"),
        KNEE_L("Knee_L", "左膝脚");

        private final String boneName;
        private final String displayName;

        Joint(String boneName, String displayName) {
            this.boneName = boneName;
            this.displayName = displayName;
        }

        public String boneName() { return boneName; }
        public String displayName() { return displayName; }
    }

    public enum Axis { X, Y, Z }

    public enum TransformMode {
        MOVE("位移", "W"), ROTATE("旋转", "E"), SCALE("缩放", "R");

        private final String displayName;
        private final String shortcut;
        TransformMode(String displayName, String shortcut) {
            this.displayName = displayName;
            this.shortcut = shortcut;
        }
        public String displayName() { return displayName; }
        public String shortcut() { return shortcut; }
        public TransformMode next() { return values()[(ordinal() + 1) % values().length]; }
    }

    public record Transform(double positionX, double positionY, double positionZ,
                            double rotationX, double rotationY, double rotationZ,
                            double scaleX, double scaleY, double scaleZ) {
        private static Transform identity() {
            return new Transform(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D,
                    1.0D, 1.0D, 1.0D);
        }

        private double value(TransformMode mode, Axis axis) {
            return switch (mode) {
                case MOVE -> switch (axis) {
                    case X -> positionX; case Y -> positionY; case Z -> positionZ;
                };
                case ROTATE -> switch (axis) {
                    case X -> rotationX; case Y -> rotationY; case Z -> rotationZ;
                };
                case SCALE -> switch (axis) {
                    case X -> scaleX; case Y -> scaleY; case Z -> scaleZ;
                };
            };
        }

        private Transform with(TransformMode mode, Axis axis, double rawValue) {
            double value = clampChannel(mode, rawValue);
            if (mode == TransformMode.MOVE) {
                return switch (axis) {
                    case X -> new Transform(value, positionY, positionZ,
                            rotationX, rotationY, rotationZ, scaleX, scaleY, scaleZ);
                    case Y -> new Transform(positionX, value, positionZ,
                            rotationX, rotationY, rotationZ, scaleX, scaleY, scaleZ);
                    case Z -> new Transform(positionX, positionY, value,
                            rotationX, rotationY, rotationZ, scaleX, scaleY, scaleZ);
                };
            }
            if (mode == TransformMode.ROTATE) {
                return switch (axis) {
                    case X -> new Transform(positionX, positionY, positionZ,
                            value, rotationY, rotationZ, scaleX, scaleY, scaleZ);
                    case Y -> new Transform(positionX, positionY, positionZ,
                            rotationX, value, rotationZ, scaleX, scaleY, scaleZ);
                    case Z -> new Transform(positionX, positionY, positionZ,
                            rotationX, rotationY, value, scaleX, scaleY, scaleZ);
                };
            }
            return switch (axis) {
                case X -> new Transform(positionX, positionY, positionZ,
                        rotationX, rotationY, rotationZ, value, scaleY, scaleZ);
                case Y -> new Transform(positionX, positionY, positionZ,
                        rotationX, rotationY, rotationZ, scaleX, value, scaleZ);
                case Z -> new Transform(positionX, positionY, positionZ,
                        rotationX, rotationY, rotationZ, scaleX, scaleY, value);
            };
        }

        private Transform lerp(Transform other, double amount) {
            double t = Math.max(0.0D, Math.min(1.0D, amount));
            return new Transform(
                    mix(positionX, other.positionX, t), mix(positionY, other.positionY, t),
                    mix(positionZ, other.positionZ, t), mix(rotationX, other.rotationX, t),
                    mix(rotationY, other.rotationY, t), mix(rotationZ, other.rotationZ, t),
                    mix(scaleX, other.scaleX, t), mix(scaleY, other.scaleY, t),
                    mix(scaleZ, other.scaleZ, t));
        }

        private boolean isIdentity() {
            return near(positionX, 0.0D) && near(positionY, 0.0D) && near(positionZ, 0.0D)
                    && near(rotationX, 0.0D) && near(rotationY, 0.0D) && near(rotationZ, 0.0D)
                    && near(scaleX, 1.0D) && near(scaleY, 1.0D) && near(scaleZ, 1.0D);
        }

        private static double mix(double from, double to, double amount) {
            return from + (to - from) * amount;
        }
        private static boolean near(double value, double expected) {
            return Math.abs(value - expected) < 1.0E-6D;
        }
    }

    public record TimelineKeyframe(float time, Transform transform) { }

    /** Immutable copy used by the editor's one-step restore undo. */
    public record ActionSnapshot(Action action, Map<Joint, List<TimelineKeyframe>> timelines) { }

    private static final String FILE_NAME = "blacklight-animation-poses.properties";
    private static final float KEY_EPSILON = 0.0125F;
    private static final NavigableMap<Float, Transform>[][] TIMELINES = createTimelines();
    private static boolean loaded;

    private AnimationPoseSettings() { }

    @SuppressWarnings("unchecked")
    private static NavigableMap<Float, Transform>[][] createTimelines() {
        NavigableMap<Float, Transform>[][] maps = new NavigableMap
                [Action.values().length][Joint.values().length];
        for (Action action : Action.values()) {
            for (Joint joint : Joint.values()) maps[action.ordinal()][joint.ordinal()] = new TreeMap<>();
        }
        return maps;
    }

    public static void load() {
        if (loaded) return;
        loaded = true;
        Path path = path();
        if (!Files.isRegularFile(path)) return;
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(path)) {
            properties.load(reader);
            for (Action action : Action.values()) {
                for (Joint joint : Joint.values()) {
                    NavigableMap<Float, Transform> timeline = timeline(action, joint);
                    timeline.clear();
                    String encoded = properties.getProperty(timelineKey(action, joint));
                    if (encoded != null && !encoded.isBlank()) decodeTimeline(action, timeline, encoded);
                    else migrateLegacyStages(properties, action, joint, timeline);
                }
            }
        } catch (IOException | NumberFormatException exception) {
            BlacklightNeoMod.LOGGER.warn("Unable to load animation transform timeline", exception);
            clearAll();
        }
    }

    public static void save() {
        load();
        Properties properties = new Properties();
        for (Action action : Action.values()) {
            for (Joint joint : Joint.values()) {
                NavigableMap<Float, Transform> timeline = timeline(action, joint);
                if (!timeline.isEmpty()) properties.setProperty(
                        timelineKey(action, joint), encodeTimeline(timeline));
            }
        }
        try {
            Files.createDirectories(path().getParent());
            try (Writer writer = Files.newBufferedWriter(path())) {
                properties.store(writer, "Blacklight: Ascension full animation transform timelines");
            }
            BlacklightNeoMod.LOGGER.info("Saved full animation transform timelines to {}", path());
        } catch (IOException exception) {
            BlacklightNeoMod.LOGGER.error("Unable to save animation transform timelines", exception);
        }
    }

    public static double valueAt(Action action, Joint joint, float time,
                                 TransformMode mode, Axis axis) {
        return sample(action, joint, time).value(mode, axis);
    }

    /** Backward-compatible rotation accessor for callers and previously authored poses. */
    public static double valueAt(Action action, Joint joint, float time, Axis axis) {
        return valueAt(action, joint, time, TransformMode.ROTATE, axis);
    }

    public static void setValueAt(Action action, Joint joint, float time,
                                  TransformMode mode, Axis axis, double value) {
        load();
        float clampedTime = clampTime(action, time);
        NavigableMap<Float, Transform> timeline = timeline(action, joint);
        Float key = keyNear(timeline, clampedTime);
        Transform base = key == null ? sample(action, joint, clampedTime) : timeline.get(key);
        if (key == null) key = quantizeTime(clampedTime);
        timeline.put(key, base.with(mode, axis, value));
    }

    public static void setValueAt(Action action, Joint joint, float time, Axis axis, double value) {
        setValueAt(action, joint, time, TransformMode.ROTATE, axis, value);
    }

    public static boolean hasKeyframeAt(Action action, Joint joint, float time) {
        load();
        return keyNear(timeline(action, joint), clampTime(action, time)) != null;
    }

    public static boolean deleteKeyframeAt(Action action, Joint joint, float time) {
        load();
        NavigableMap<Float, Transform> timeline = timeline(action, joint);
        Float key = keyNear(timeline, clampTime(action, time));
        return key != null && timeline.remove(key) != null;
    }

    public static List<Float> keyframeTimes(Action action, Joint joint) {
        load();
        return new ArrayList<>(timeline(action, joint).keySet());
    }

    public static void resetJoint(Action action, Joint joint) {
        load();
        timeline(action, joint).clear();
    }

    public static void resetAction(Action action) {
        load();
        for (Joint joint : Joint.values()) timeline(action, joint).clear();
    }

    public static ActionSnapshot snapshotAction(Action action) {
        load();
        Map<Joint, List<TimelineKeyframe>> copy = new EnumMap<>(Joint.class);
        for (Joint joint : Joint.values()) {
            List<TimelineKeyframe> frames = timeline(action, joint).entrySet().stream()
                    .map(entry -> new TimelineKeyframe(entry.getKey(), entry.getValue()))
                    .toList();
            copy.put(joint, frames);
        }
        return new ActionSnapshot(action, Map.copyOf(copy));
    }

    public static void restoreAction(ActionSnapshot snapshot) {
        load();
        for (Joint joint : Joint.values()) {
            NavigableMap<Float, Transform> target = timeline(snapshot.action(), joint);
            target.clear();
            for (TimelineKeyframe frame : snapshot.timelines().getOrDefault(joint, List.of())) {
                target.put(quantizeTime(clampTime(snapshot.action(), frame.time())), frame.transform());
            }
        }
    }

    public static Transform sample(Action action, Joint joint, float time) {
        load();
        NavigableMap<Float, Transform> timeline = timeline(action, joint);
        if (timeline.isEmpty()) return Transform.identity();
        float clampedTime = clampTime(action, time);
        var lower = timeline.floorEntry(clampedTime);
        var upper = timeline.ceilingEntry(clampedTime);
        if (lower == null) return upper.getValue();
        if (upper == null) return lower.getValue();
        if (lower.getKey().equals(upper.getKey())) return lower.getValue();
        double amount = (clampedTime - lower.getKey()) / (upper.getKey() - lower.getKey());
        return lower.getValue().lerp(upper.getValue(), amount);
    }

    /** Applies translation, rotation and scale after Blacklight samples the authored pose. */
    public static void apply(Action action, BlacklightPose pose, float time) {
        for (Joint joint : Joint.values()) {
            if (!pose.hasTransform(joint.boneName())) continue;
            Transform adjustment = sample(action, joint, time);
            if (adjustment.isIdentity()) continue;
            BoneTransform transform = pose.get(joint.boneName());
            transform.translation().add((float)adjustment.positionX(),
                    (float)adjustment.positionY(), (float)adjustment.positionZ());
            Quaternionf offset = new Quaternionf().rotationXYZ(
                    (float)Math.toRadians(adjustment.rotationX()),
                    (float)Math.toRadians(adjustment.rotationY()),
                    (float)Math.toRadians(adjustment.rotationZ()));
            transform.rotation().mul(offset).normalize();
            transform.scale().mul((float)adjustment.scaleX(),
                    (float)adjustment.scaleY(), (float)adjustment.scaleZ());
        }
    }

    public static String copyText(Action action, Joint joint, float time) {
        Transform t = sample(action, joint, time);
        return String.format(Locale.ROOT,
                "%s @ %.3fs · %s · P(%.3f,%.3f,%.3f) R(%.2f,%.2f,%.2f) S(%.3f,%.3f,%.3f)",
                action.key(), clampTime(action, time), joint.boneName(),
                t.positionX(), t.positionY(), t.positionZ(),
                t.rotationX(), t.rotationY(), t.rotationZ(),
                t.scaleX(), t.scaleY(), t.scaleZ());
    }

    private static NavigableMap<Float, Transform> timeline(Action action, Joint joint) {
        return TIMELINES[action.ordinal()][joint.ordinal()];
    }

    private static void clearAll() {
        for (Action action : Action.values()) {
            for (Joint joint : Joint.values()) timeline(action, joint).clear();
        }
    }

    private static String timelineKey(Action action, Joint joint) {
        return action.key() + "." + joint.boneName().toLowerCase(Locale.ROOT) + ".timeline";
    }

    private static String encodeTimeline(NavigableMap<Float, Transform> timeline) {
        StringBuilder encoded = new StringBuilder();
        timeline.forEach((time, t) -> {
            if (!encoded.isEmpty()) encoded.append(';');
            encoded.append(String.format(Locale.ROOT,
                    "%.4f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f",
                    time, t.positionX(), t.positionY(), t.positionZ(),
                    t.rotationX(), t.rotationY(), t.rotationZ(),
                    t.scaleX(), t.scaleY(), t.scaleZ()));
        });
        return encoded.toString();
    }

    private static void decodeTimeline(Action action, NavigableMap<Float, Transform> timeline,
                                       String encoded) {
        for (String frame : encoded.split(";")) {
            String[] values = frame.split(",");
            if (values.length == 4) {
                // Rotation-only timeline from the previous editor revision.
                float time = clampTime(action, Float.parseFloat(values[0]));
                timeline.put(quantizeTime(time), Transform.identity()
                        .with(TransformMode.ROTATE, Axis.X, Double.parseDouble(values[1]))
                        .with(TransformMode.ROTATE, Axis.Y, Double.parseDouble(values[2]))
                        .with(TransformMode.ROTATE, Axis.Z, Double.parseDouble(values[3])));
            } else if (values.length == 10) {
                float time = clampTime(action, Float.parseFloat(values[0]));
                timeline.put(quantizeTime(time), new Transform(
                        clampChannel(TransformMode.MOVE, Double.parseDouble(values[1])),
                        clampChannel(TransformMode.MOVE, Double.parseDouble(values[2])),
                        clampChannel(TransformMode.MOVE, Double.parseDouble(values[3])),
                        clampChannel(TransformMode.ROTATE, Double.parseDouble(values[4])),
                        clampChannel(TransformMode.ROTATE, Double.parseDouble(values[5])),
                        clampChannel(TransformMode.ROTATE, Double.parseDouble(values[6])),
                        clampChannel(TransformMode.SCALE, Double.parseDouble(values[7])),
                        clampChannel(TransformMode.SCALE, Double.parseDouble(values[8])),
                        clampChannel(TransformMode.SCALE, Double.parseDouble(values[9]))));
            }
        }
    }

    /** Imports the original three-stage rotation properties when present. */
    private static void migrateLegacyStages(Properties properties, Action action, Joint joint,
                                            NavigableMap<Float, Transform> timeline) {
        String[] stages = {"start", "middle", "end"};
        float[] times = {0.0F, action.totalTime() * 0.5F, action.totalTime()};
        for (int index = 0; index < stages.length; index++) {
            String prefix = action.key() + "." + stages[index] + "."
                    + joint.boneName().toLowerCase(Locale.ROOT) + ".";
            Transform value = Transform.identity()
                    .with(TransformMode.ROTATE, Axis.X, read(properties, prefix + "x", 0.0D))
                    .with(TransformMode.ROTATE, Axis.Y, read(properties, prefix + "y", 0.0D))
                    .with(TransformMode.ROTATE, Axis.Z, read(properties, prefix + "z", 0.0D));
            if (!value.isIdentity()) timeline.put(quantizeTime(times[index]), value);
        }
    }

    private static Float keyNear(NavigableMap<Float, Transform> timeline, float time) {
        Float floor = timeline.floorKey(time);
        if (floor != null && Math.abs(floor - time) <= KEY_EPSILON) return floor;
        Float ceiling = timeline.ceilingKey(time);
        return ceiling != null && Math.abs(ceiling - time) <= KEY_EPSILON ? ceiling : null;
    }

    private static double clampChannel(TransformMode mode, double value) {
        if (!Double.isFinite(value)) return mode == TransformMode.SCALE ? 1.0D : 0.0D;
        return switch (mode) {
            case MOVE -> Math.max(-3.0D, Math.min(3.0D, value));
            case ROTATE -> Math.max(-180.0D, Math.min(180.0D, value));
            case SCALE -> Math.max(0.05D, Math.min(4.0D, value));
        };
    }

    private static float clampTime(Action action, float time) {
        if (!Float.isFinite(time)) return 0.0F;
        return Math.max(0.0F, Math.min(action.totalTime(), time));
    }

    private static float quantizeTime(float time) { return Math.round(time * 1000.0F) / 1000.0F; }
    private static Path path() { return FMLPaths.CONFIGDIR.get().resolve(FILE_NAME); }

    private static double read(Properties properties, String key, double fallback) {
        String text = properties.getProperty(key);
        if (text == null) return fallback;
        double value = Double.parseDouble(text);
        return Double.isFinite(value) ? value : fallback;
    }
}
