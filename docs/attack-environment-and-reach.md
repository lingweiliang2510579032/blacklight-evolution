# Attack environment response and progression

Base attack reach is 125% of the former value. Three sequential Attack Extension upgrades in Biology raise it to 135%, 145% and 155%, costing 300, 600 and 900 biomass. Ownership uses new bits in the existing saved and synchronized growth attachment; old saves default to no new tiers. Purchase prerequisites and balance are checked by the server.

Melee contact bounds and acquisition range, whip centerlines and swept collision, ground strike radius, groundspike range, and environment probes use the shared AttackReach scale. The added melee reach is represented by extended cutting/pressure trails; whip geometry extends from its anchored hand. This does not increase ordinary ground auto-approach, damage or movement speed. Actual body-flight collisions remain body contacts. Whip grabbing/consumption range is a separate mechanic and is unchanged.

During active contact intervals, attacks probe solid terrain using authored contact bounds or the whip curve. Fallback moves use a directional contact ray. Each action has a six-surface budget and emits small pieces of the struck material, contact sounds, dust and limited three-dimensional fragments. Whip probes stop at the first wall. These probes approximate melee swept contact volumes rather than exact mesh/terrain triangle intersections.

Hammer ground strikes and blade dives now reuse the landing debris lifecycle for larger fragments, and visible nearby loose items receive a bounded outward impulse. Fragments are temporary block-display entities, never place or remove terrain, share a global budget of 64 and are not saved. Attack fragments shatter on terrain collision or expire shortly afterward.

Validation for this revision is compilation/packaging only. Automated tests, client capture and gameplay acceptance were not run, following the user's request. The active game session is not modified; the new build requires a restart.
