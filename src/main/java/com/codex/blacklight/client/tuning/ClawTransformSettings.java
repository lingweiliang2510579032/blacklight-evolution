package com.codex.blacklight.client.tuning;

import com.codex.blacklight.BlacklightNeoMod;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Live client-side transforms, stored independently for the right and left biological arm. */
public final class ClawTransformSettings {
    public enum HandSide {
        RIGHT("right"), LEFT("left");

        private final String key;
        HandSide(String key) { this.key = key; }
        public String key() { return key; }
        public HandSide opposite() { return this == RIGHT ? LEFT : RIGHT; }
    }

    private static final String FILE_NAME = "blacklight-claw-transform.properties";
    // Artist-approved defaults captured from the in-game tuner on 2026-08-08.
    private static final Transform RIGHT = new Transform(
            0.1546391752577323D, -0.2989690721649483D, 0.17525773195876315D,
            -11.597938144329902D, -84.89690721649484D, 43.60824742268042D,
            0.9822809278350516D);
    private static final Transform LEFT = new Transform(
            -0.15463917525773185D, -0.39175257731958757D, 0.17525773195876315D,
            -122.47422680412372D, 83.96907216494844D, 54.27835051546393D,
            0.9822809278350516D);
    private static boolean loaded;
    private static boolean preview;

    private ClawTransformSettings() { }

    public static void load() {
        if (loaded) return;
        loaded = true;
        Path path = path();
        if (!Files.isRegularFile(path)) return;
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(path)) {
            properties.load(reader);
            // Legacy unprefixed values become the default for both hands, preserving the
            // player's carefully tuned transform during migration to independent controls.
            Transform legacy = readTransform(properties, "", new Transform());
            RIGHT.copyFrom(readTransform(properties, "right_", legacy));
            LEFT.copyFrom(readTransform(properties, "left_", legacy));
        } catch (IOException | NumberFormatException exception) {
            BlacklightNeoMod.LOGGER.warn("Unable to load claw transform settings", exception);
            resetAll();
        }
    }

    public static void save() {
        load();
        Path path = path();
        Properties properties = new Properties();
        writeTransform(properties, "right_", RIGHT);
        writeTransform(properties, "left_", LEFT);
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                properties.store(writer, "Blacklight Evolution independent claw transform tuner");
            }
            BlacklightNeoMod.LOGGER.info("Saved independent claw transforms to {}", path);
        } catch (IOException exception) {
            BlacklightNeoMod.LOGGER.error("Unable to save claw transform settings", exception);
        }
    }

    public static void reset(HandSide side) { transform(side).copyFrom(defaultTransform(side)); }
    public static void resetAll() {
        RIGHT.copyFrom(defaultTransform(HandSide.RIGHT));
        LEFT.copyFrom(defaultTransform(HandSide.LEFT));
    }
    public static void copy(HandSide from, HandSide to) { transform(to).copyFrom(transform(from)); }

    public static boolean preview() { return preview; }
    public static void setPreview(boolean value) { preview = value; }

    public static double offsetX(HandSide side) { load(); return transform(side).offsetX; }
    public static double offsetY(HandSide side) { load(); return transform(side).offsetY; }
    public static double offsetZ(HandSide side) { load(); return transform(side).offsetZ; }
    public static double rotationX(HandSide side) { load(); return transform(side).rotationX; }
    public static double rotationY(HandSide side) { load(); return transform(side).rotationY; }
    public static double rotationZ(HandSide side) { load(); return transform(side).rotationZ; }
    public static double scale(HandSide side) { load(); return transform(side).scale; }
    // Anatomical proportions keep the shoulder and elbow pivots fixed. Legacy rigid
    // transforms remain in the file for rollback, but do not drive the segmented rig.
    public static double limbWidth(HandSide side) { load(); return transform(side).limbWidth; }
    public static double talonLength(HandSide side) { load(); return transform(side).talonLength; }
    public static void setLimbWidth(HandSide side, double value) {
        transform(side).limbWidth = clamp(value, 1.0D, 1.4D);
    }
    public static void setTalonLength(HandSide side, double value) {
        transform(side).talonLength = clamp(value, 0.7D, 1.5D);
    }

    public static void setOffsetX(HandSide side, double value) { transform(side).offsetX = clamp(value, -4.0D, 4.0D); }
    public static void setOffsetY(HandSide side, double value) { transform(side).offsetY = clamp(value, -4.0D, 4.0D); }
    public static void setOffsetZ(HandSide side, double value) { transform(side).offsetZ = clamp(value, -4.0D, 4.0D); }
    public static void setRotationX(HandSide side, double value) { transform(side).rotationX = clamp(value, -180.0D, 180.0D); }
    public static void setRotationY(HandSide side, double value) { transform(side).rotationY = clamp(value, -180.0D, 180.0D); }
    public static void setRotationZ(HandSide side, double value) { transform(side).rotationZ = clamp(value, -180.0D, 180.0D); }
    public static void setScale(HandSide side, double value) { transform(side).scale = clamp(value, 0.05D, 8.0D); }

    private static Transform transform(HandSide side) { return side == HandSide.LEFT ? LEFT : RIGHT; }

    private static Transform defaultTransform(HandSide side) {
        return side == HandSide.LEFT
                ? new Transform(-0.15463917525773185D, -0.39175257731958757D,
                        0.17525773195876315D, -122.47422680412372D,
                        83.96907216494844D, 54.27835051546393D,
                        0.9822809278350516D)
                : new Transform(0.1546391752577323D, -0.2989690721649483D,
                        0.17525773195876315D, -11.597938144329902D,
                        -84.89690721649484D, 43.60824742268042D,
                        0.9822809278350516D);
    }

    private static Transform readTransform(Properties properties, String prefix, Transform fallback) {
        Transform value = new Transform();
        value.offsetX = read(properties, prefix + "offset_x", fallback.offsetX);
        value.offsetY = read(properties, prefix + "offset_y", fallback.offsetY);
        value.offsetZ = read(properties, prefix + "offset_z", fallback.offsetZ);
        value.rotationX = read(properties, prefix + "rotation_x", fallback.rotationX);
        value.rotationY = read(properties, prefix + "rotation_y", fallback.rotationY);
        value.rotationZ = read(properties, prefix + "rotation_z", fallback.rotationZ);
        value.scale = clamp(read(properties, prefix + "scale", fallback.scale), 0.05D, 8.0D);
        value.limbWidth = clamp(read(properties, prefix + "limb_width", 1.0D), 1.0D, 1.4D);
        value.talonLength = clamp(read(properties, prefix + "talon_length", 1.0D), 0.7D, 1.5D);
        return value;
    }

    private static void writeTransform(Properties properties, String prefix, Transform value) {
        properties.setProperty(prefix + "offset_x", Double.toString(value.offsetX));
        properties.setProperty(prefix + "offset_y", Double.toString(value.offsetY));
        properties.setProperty(prefix + "offset_z", Double.toString(value.offsetZ));
        properties.setProperty(prefix + "rotation_x", Double.toString(value.rotationX));
        properties.setProperty(prefix + "rotation_y", Double.toString(value.rotationY));
        properties.setProperty(prefix + "rotation_z", Double.toString(value.rotationZ));
        properties.setProperty(prefix + "scale", Double.toString(value.scale));
        properties.setProperty(prefix + "limb_width", Double.toString(value.limbWidth));
        properties.setProperty(prefix + "talon_length", Double.toString(value.talonLength));
    }

    private static Path path() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve(FILE_NAME);
    }

    private static double read(Properties properties, String key, double fallback) {
        String value = properties.getProperty(key);
        if (value == null) return fallback;
        double parsed = Double.parseDouble(value);
        return Double.isFinite(parsed) ? parsed : fallback;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class Transform {
        private double offsetX;
        private double offsetY;
        private double offsetZ;
        private double rotationX;
        private double rotationY;
        private double rotationZ;
        private double scale = 1.0D;
        private double limbWidth = 1.0D;
        private double talonLength = 1.0D;

        private Transform() { }

        private Transform(double offsetX, double offsetY, double offsetZ,
                          double rotationX, double rotationY, double rotationZ,
                          double scale) {
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.offsetZ = offsetZ;
            this.rotationX = rotationX;
            this.rotationY = rotationY;
            this.rotationZ = rotationZ;
            this.scale = scale;
        }

        private void copyFrom(Transform other) {
            offsetX = other.offsetX;
            offsetY = other.offsetY;
            offsetZ = other.offsetZ;
            rotationX = other.rotationX;
            rotationY = other.rotationY;
            rotationZ = other.rotationZ;
            scale = other.scale;
            limbWidth = other.limbWidth;
            talonLength = other.talonLength;
        }
    }
}
