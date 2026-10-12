package com.codex.blacklight.animation;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Protect the extracted boundary from accidentally reintroducing renderer-specific linkage. */
class GameplayAnimationBoundaryTest {
    @Test void gameplayClassesDoNotLinkAgainstEpicFightOrItsAdapter() throws Exception {
        for (String name : new String[]{
                "combat/ClawCombatSystem", "combat/WeaponCombatSystem", "combat/ConsumeSystem",
                "movement/PrototypeMovementSystem", "mode/PrototypeModeSystem", "network/ClawAnimationPayload",
                "network/ClawAnimationPayload$Animation", "animation/PlayerAction", "animation/PlayerActionBackend",
                "animation/PlayerActions", "animation/MatrixMotionClip"}) {
            try (var input = getClass().getResourceAsStream("/com/codex/blacklight/" + name + ".class")) {
                assertNotNull(input, name);
                String constants = new String(input.readAllBytes(), StandardCharsets.ISO_8859_1);
                assertFalse(constants.contains("yesman/epicfight"), name);
                assertFalse(constants.contains("integration/epicfight"), name);
            }
        }
    }
}
