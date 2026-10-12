> 历史阶段说明：当前版本状态与验收边界以 [RC1 收口说明](RELEASE_RC1.md) 为准。本文的旧协议号和待办不能直接代表当前实现。

# Local weapon motion imports

The optional `blacklight.localWeaponMotions` JVM property selects a directory of locally authored or retargeted Epic Fight clips. No imported files are included in the mod JAR. A normal launch keeps the existing animations and timings.

For the development client:

```text
gradlew runClient -PlocalWeaponMotions=<absolute directory>
```

Each file is named after a weapon action, for example `blade_cut.json`. It contains a standard Epic Fight `MATRIX` animation with Root first and Coord last, plus a `blacklight` object containing `durationTicks`, `contactStartTick`, `contactEndTick`, and `transitionSeconds`. The contact end is exclusive. Server duration includes the initial animation link and final tick rounding.

An optional `blacklight.contactWindows` array can describe separate active intervals as objects with `startTick` and `endTick`. Intervals are ordered, non-overlapping and end-exclusive; the first start and last end must match the outer contact bounds. Gaps do not cause damage. All of these intervals share one target hit ledger for the action. That single-hit policy is the Mod's adaptation, not a claim about deduplication between native Prototype collision tracks.

Optional `blacklight.contactBounds` entries contain `tick`, `min: [right, up, forward]`, and `max: [right, up, forward]`. When present, every active tick needs exactly one entry. Bounds describe the blade swept during that tick, relative to the actor's current feet; remove the tick-end Coord displacement once when exporting. The server projects each candidate's bounding box into the action's fixed facing axes and requires intersection with this envelope in addition to reach, facing and wall checks. This avoids broad-reach hits before the blade arrives. It is a sampled swept AABB gate, not an exact triangle or original-game collider implementation.

The first read caches the complete file for the process. Server timing and visual animation consequently use the same snapshot; edits require a restart. Missing actions use their existing animations. A malformed existing file aborts loading rather than mixing a new damage schedule with an unrelated fallback pose.

Root must already have its horizontal movement removed in armature/world coordinates. Coord contains that same horizontal trajectory in the exporter's coordinate system. Epic Fight applies its usual Blender-to-game conversion while loading the MATRIX Coord track. Do not convert the Coord axis a second time, remove vertical crouching from Root, or substitute an unrelated forward movement curve.

The action uses collision-aware Epic Fight movement. It does not inherit foreign damage events: WeaponCombatSystem still owns the damage window and single-hit bookkeeping. This import mechanism does not reproduce the original game's charging, input arbitration, target correction, or complete state machine.

`blade_dive` and `hammer_drop` imports are rejected: those actions require the existing landing-synchronized animation clock, which this local importer does not replace.

The current workflow is for the local single-player development client. A client-only file is not distributed to remote servers; it is not a multiplayer animation synchronization protocol.

## Original input reference

The [Activision PC manual](https://shared.fastly.steamstatic.com/store_item_assets/steam/apps/10150/manuals/Manual_english.pdf?t=1655760765), printed pages 3–4, distinguishes Attack (left mouse), Special (right mouse), and Grab (E). Critical Mass devastators use held chords: Attack+Special, Special+Grab, and Attack+Grab. A chord must be resolved before dispatching its constituent single-button actions.

Local P1 FIG data additionally distinguishes blade Special on the ground, during sprint, and in the air. Several attack nodes use press, release, and charge conditions. Grounded blade RMB now sends the explicit `WEAPON_SPECIAL` intent and selects `BLADE_RUSH`, from either standing or sprinting. Its motion supplies collision-aware forward displacement; ordinary movement and jump/air-dash inputs cannot replace it during its committed action. A standard launch uses an Epic Fight dash adaptation, while an explicit local profile can replace it with a retargeted source clip.

This does not implement the complete source input state machine. The air branch, charging, combo chords and Critical Mass devastators still require separate work; do not count those as restored. Protocol version 12 prevents the new Special action id from being interpreted by an older server as a claw transform.
