package com.codex.blacklight.movement;

import com.codex.blacklight.BlacklightNeoMod;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Runtime-editable Prototype movement constants. Dedicated servers synchronize a snapshot to clients. */
public final class MovementTuningSettings {
    private static final String FILE_NAME = "blacklight-movement-engine.properties";
    public static final MovementTuningSnapshot MVP_DEFAULTS = new MovementTuningSnapshot(
            0.23968446601941748D, 2.0645631067961165D,
            1.2407766990291262D, 3.0D, 40.95388349514563D,
            0.48D, 0.36D, 0.085D, 0.82D, 1.45D, 7.0D, 0.16D);

    private static double sprintAcceleration = MVP_DEFAULTS.sprintAcceleration();
    private static double sprintMaxSpeed = MVP_DEFAULTS.sprintMaxSpeed();
    private static double jumpMinVelocity = MVP_DEFAULTS.jumpMinVelocity();
    private static double jumpMaxVelocity = MVP_DEFAULTS.jumpMaxVelocity();
    private static double jumpChargeTicks = MVP_DEFAULTS.jumpChargeTicks();
    private static double wallDetectDistance = MVP_DEFAULTS.wallDetectDistance();
    private static double wallClimbVelocity = MVP_DEFAULTS.wallClimbVelocity();
    private static double glideFallSpeed = MVP_DEFAULTS.glideFallSpeed();
    private static double glideMaxSpeed = MVP_DEFAULTS.glideMaxSpeed();
    private static double airDashSpeed = MVP_DEFAULTS.airDashSpeed();
    private static double airDashTicks = MVP_DEFAULTS.airDashTicks();
    private static double fovBoost = MVP_DEFAULTS.fovBoost();
    private static boolean loaded;

    private MovementTuningSettings() { }

    public static void load() {
        if (loaded) return;
        loaded = true;
        Path path = path();
        if (!Files.isRegularFile(path)) return;
        Properties values = new Properties();
        try (Reader reader = Files.newBufferedReader(path)) {
            values.load(reader);
            setSprintAcceleration(read(values, "sprint_acceleration", sprintAcceleration));
            setSprintMaxSpeed(read(values, "sprint_max_speed", sprintMaxSpeed));
            setJumpMinVelocity(read(values, "jump_min_velocity", jumpMinVelocity));
            setJumpMaxVelocity(read(values, "jump_max_velocity", jumpMaxVelocity));
            setJumpChargeTicks(read(values, "jump_charge_ticks", jumpChargeTicks));
            setWallDetectDistance(read(values, "wall_detect_distance", wallDetectDistance));
            setWallClimbVelocity(read(values, "wall_climb_velocity", wallClimbVelocity));
            setGlideFallSpeed(read(values, "glide_fall_speed", glideFallSpeed));
            setGlideMaxSpeed(read(values, "glide_max_speed", glideMaxSpeed));
            setAirDashSpeed(read(values, "air_dash_speed", airDashSpeed));
            setAirDashTicks(read(values, "air_dash_ticks", airDashTicks));
            setFovBoost(read(values, "fov_boost", fovBoost));
        } catch (IOException | NumberFormatException exception) {
            BlacklightNeoMod.LOGGER.warn("Unable to load movement engine tuning", exception);
        }
    }

    public static void save() {
        load();
        Properties values = new Properties();
        put(values, "sprint_acceleration", sprintAcceleration);
        put(values, "sprint_max_speed", sprintMaxSpeed);
        put(values, "jump_min_velocity", jumpMinVelocity);
        put(values, "jump_max_velocity", jumpMaxVelocity);
        put(values, "jump_charge_ticks", jumpChargeTicks);
        put(values, "wall_detect_distance", wallDetectDistance);
        put(values, "wall_climb_velocity", wallClimbVelocity);
        put(values, "glide_fall_speed", glideFallSpeed);
        put(values, "glide_max_speed", glideMaxSpeed);
        put(values, "air_dash_speed", airDashSpeed);
        put(values, "air_dash_ticks", airDashTicks);
        put(values, "fov_boost", fovBoost);
        try {
            Files.createDirectories(path().getParent());
            try (Writer writer = Files.newBufferedWriter(path())) {
                values.store(writer, "Blacklight: Ascension movement engine tuner");
            }
        } catch (IOException exception) {
            BlacklightNeoMod.LOGGER.error("Unable to save movement engine tuning", exception);
        }
    }

    public static double sprintAcceleration() { load(); return sprintAcceleration; }
    public static double sprintMaxSpeed() { load(); return sprintMaxSpeed; }
    public static double jumpMinVelocity() { load(); return jumpMinVelocity; }
    public static double jumpMaxVelocity() { load(); return jumpMaxVelocity; }
    public static int jumpChargeTicks() { load(); return Math.max(1, (int)Math.round(jumpChargeTicks)); }
    public static double wallDetectDistance() { load(); return wallDetectDistance; }
    public static double wallClimbVelocity() { load(); return wallClimbVelocity; }
    public static double glideFallSpeed() { load(); return glideFallSpeed; }
    public static double glideMaxSpeed() { load(); return glideMaxSpeed; }
    public static double airDashSpeed() { load(); return airDashSpeed; }
    public static int airDashTicks() { load(); return Math.max(1, (int)Math.round(airDashTicks)); }
    public static double fovBoost() { load(); return fovBoost; }

    public static MovementTuningSnapshot snapshot() {
        load();
        return new MovementTuningSnapshot(
                sprintAcceleration, sprintMaxSpeed, jumpMinVelocity, jumpMaxVelocity,
                jumpChargeTicks, wallDetectDistance, wallClimbVelocity, glideFallSpeed,
                glideMaxSpeed, airDashSpeed, airDashTicks, fovBoost);
    }

    /** Applies the authoritative values received from a multiplayer server. */
    public static void applyServerSnapshot(MovementTuningSnapshot snapshot) {
        setSprintAcceleration(snapshot.sprintAcceleration());
        setSprintMaxSpeed(snapshot.sprintMaxSpeed());
        setJumpMinVelocity(snapshot.jumpMinVelocity());
        setJumpMaxVelocity(snapshot.jumpMaxVelocity());
        setJumpChargeTicks(snapshot.jumpChargeTicks());
        setWallDetectDistance(snapshot.wallDetectDistance());
        setWallClimbVelocity(snapshot.wallClimbVelocity());
        setGlideFallSpeed(snapshot.glideFallSpeed());
        setGlideMaxSpeed(snapshot.glideMaxSpeed());
        setAirDashSpeed(snapshot.airDashSpeed());
        setAirDashTicks(snapshot.airDashTicks());
        setFovBoost(snapshot.fovBoost());
        loaded = true;
    }

    public static void setSprintAcceleration(double value) { sprintAcceleration = clamp(value, 0.0D, 0.25D); }
    public static void setSprintMaxSpeed(double value) { sprintMaxSpeed = clamp(value, 0.2D, 2.5D); }
    public static void setJumpMinVelocity(double value) { jumpMinVelocity = clamp(value, 0.3D, 2.0D); }
    public static void setJumpMaxVelocity(double value) { jumpMaxVelocity = clamp(value, 0.5D, 3.0D); }
    public static void setJumpChargeTicks(double value) { jumpChargeTicks = clamp(value, 1.0D, 60.0D); }
    public static void setWallDetectDistance(double value) { wallDetectDistance = clamp(value, 0.1D, 1.2D); }
    public static void setWallClimbVelocity(double value) { wallClimbVelocity = clamp(value, 0.05D, 1.2D); }
    public static void setGlideFallSpeed(double value) { glideFallSpeed = clamp(value, 0.01D, 0.5D); }
    public static void setGlideMaxSpeed(double value) { glideMaxSpeed = clamp(value, 0.1D, 2.0D); }
    public static void setAirDashSpeed(double value) { airDashSpeed = clamp(value, 0.2D, 3.0D); }
    public static void setAirDashTicks(double value) { airDashTicks = clamp(value, 1.0D, 20.0D); }
    public static void setFovBoost(double value) { fovBoost = clamp(value, 0.0D, 0.5D); }

    private static Path path() { return FMLPaths.CONFIGDIR.get().resolve(FILE_NAME); }
    private static double read(Properties values, String key, double fallback) {
        String value = values.getProperty(key);
        if (value == null) return fallback;
        double parsed = Double.parseDouble(value);
        return Double.isFinite(parsed) ? parsed : fallback;
    }
    private static void put(Properties values, String key, double value) {
        values.setProperty(key, Double.toString(value));
    }
    private static double clamp(double value, double min, double max) {
        return Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : min;
    }
}
