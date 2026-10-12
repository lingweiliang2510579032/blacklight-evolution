# Original traversal motion profile

Basic player movement uses locally retargeted original motion tracks exclusively.
The former authored locomotion resources, procedural gait posing and third-party
wall-run fallback have been removed. Missing tracks stop startup with an explicit
profile error rather than silently substituting an old pose.

Install the complete private profile and launch with
`-PlocalWeaponMotions=<profile directory>` during development. Installed clients
and dedicated servers use the JVM property
`-Dblacklight.localWeaponMotions=<profile directory>`. The code requires all 41
`basic_*.json` tracks listed in `NativeTraversalLibrary.IDS`; the profile also
contains the existing weapon tracks. Original-derived assets are not distributed
in this source tree or the JAR.

Coverage: five weapon stances, walking/running/three sprint tiers, braking and
turning, charge and three takeoff poses, falling, aerial dash/recovery, four glide
directions, directional wall runs/climbs, cling, leap, mantle, vault, and four
landing poses. The claw mesh now follows the retargeted wrist as well as elbow.

The existing traversal system remains authoritative for collision, displacement,
stamina and landing recovery. Animation `Coord` records are diagnostic and never
add a second displacement. Moving can cancel the tail of a landing pose. Traversal
refresh must not replace an ongoing weapon attack or absorption.

Wall-run tracks use a surface frame: source forward follows wall travel and source
up points away from the wall. Climbing retains its authored vertical frame. Wall
normals are stored per entity for authority, prediction and observers; camera yaw
does not rotate the stance away from the wall. Placement includes Epic Fight's
renderer half-turn. Final blended player poses check the outer head bounds against
the actual wall, including the first transition from falling. These are visual
root corrections; they do not teleport the player's collision body.

Animation instances are shared between the integrated server and renderer. Their
clip cache publishes fully baked clips atomically. Weak identity maps keep entity
clocks and contacts separate even when client/server players have the same numeric
ID. Regression coverage includes simultaneous first wall-clip loads.

These are original keyframes adapted to Minecraft's proportions. Gait cadence,
glide sector blending and some transition conditions are local adaptations, not
a claim that the original game's entire animation state machine is reproduced.

Verification: `test build` for unit tests, and `runGameTestServer
-PwallTraversalTest -PlocalWeaponMotions=<profile directory>` for skeletal loading,
wall collision and combat regression cases. The visual harness is private and is
excluded from normal builds.
