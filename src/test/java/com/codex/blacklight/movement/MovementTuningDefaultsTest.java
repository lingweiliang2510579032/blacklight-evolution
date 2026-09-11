package com.codex.blacklight.movement;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MovementTuningDefaultsTest {
    @Test
    void shippedPresetMatchesCompiledMvpDefaults() throws IOException {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(
                Path.of(System.getProperty("user.dir"), "config-presets",
                        "blacklight-movement-engine.properties"))) {
            properties.load(reader);
        }

        MovementTuningSnapshot defaults = MovementTuningSettings.MVP_DEFAULTS;
        assertProperty(properties, "sprint_acceleration", defaults.sprintAcceleration());
        assertProperty(properties, "sprint_max_speed", defaults.sprintMaxSpeed());
        assertProperty(properties, "jump_min_velocity", defaults.jumpMinVelocity());
        assertProperty(properties, "jump_max_velocity", defaults.jumpMaxVelocity());
        assertProperty(properties, "jump_charge_ticks", defaults.jumpChargeTicks());
        assertProperty(properties, "wall_detect_distance", defaults.wallDetectDistance());
        assertProperty(properties, "wall_climb_velocity", defaults.wallClimbVelocity());
        assertProperty(properties, "glide_fall_speed", defaults.glideFallSpeed());
        assertProperty(properties, "glide_max_speed", defaults.glideMaxSpeed());
        assertProperty(properties, "air_dash_speed", defaults.airDashSpeed());
        assertProperty(properties, "air_dash_ticks", defaults.airDashTicks());
        assertProperty(properties, "fov_boost", defaults.fovBoost());
    }

    private static void assertProperty(Properties properties, String key, double expected) {
        assertEquals(expected, Double.parseDouble(properties.getProperty(key)), 1.0E-12D, key);
    }
}
