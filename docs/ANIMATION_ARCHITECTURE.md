# Animation architecture

Target: Minecraft 1.21.1, NeoForge 21.1.244, Java 21.

## Runtime responsibilities

- Epic Fight 21.17.3.1 owns the player armature, combat mode, locomotion blending, attack phases, hit windows, root motion, stun and dodge state.
- GeckoLib 4.9.2 owns biological weapon bones, morph transitions, infected/Hunter/Boss entities, tentacles and keyed particle/sound events.
- Blacklight gameplay remains server authoritative. Animation keyframes request effects, but the server calculates targets, reach, damage and biomass costs.

## Required action tracks

Every combat action is split into startup, active, recovery and cancel windows. Damage is applied only during the active window. `CombatAnimationSpec` is the server timing contract and Epic Fight owns the matching player-armature tracks. The current first slice registers original clips for claw sprint, wall run, claw transformation, a three-hit claw combo, groundspike, consume and air dash. GeckoLib contains the synchronized biological overlay tracks. Hammerfists, Whipfist, Musclemass, Blade, Shield, Armor and both sensory modes remain separate authored tracks, not reskins of the claw track.

## Asset rules

- Blockbench sources live beside the project as `.bbmodel` files but are excluded from the final JAR.
- Runtime models are exported to `assets/blacklight/geo`.
- GeckoLib clips are exported to `assets/blacklight/animations`.
- Epic Fight movesets and animation registration are versioned independently so gameplay timing can be tested without re-exporting geometry.
- Original game assets are not redistributed; geometry, textures and sound must be authored for this project.
