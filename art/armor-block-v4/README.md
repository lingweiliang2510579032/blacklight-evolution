# Prototype-referenced Minecraft armor review 04

Supersedes the speculative mechanical heavy armor direction of review 03.
The user asked to reference the original Prototype armor while preserving
Minecraft style and reviewing the model before runtime integration.

Reference: local `alex_armour.p3d`, assembled `armor_body` and `armor_arms`
surfaces viewed at their native proportions, without Minecraft retargeting.
Observed landmarks translated into hand-authored cuboids:

- Closed featureless face shell, upright neck flanges.
- Broad pectoral forms with a narrower segmented abdomen.
- Compact shoulder shells and forearms tapering toward the hands.
- Forearm blade ridges, outward hip crests, overlapping thigh shells.
- Long shin plates; subdued charcoal and dark organic seams.

No native geometry, textures or animations are embedded in these files.
All cuboids are authored in `tools/author_armor_block_v4.py`. The original
reference renders remain private outside the repository.

The user approved this revision. `armor.bbmodel` and `layout.json` preserve
the reviewed geometry and palette. RC30 now bakes this geometry into the
runtime model using `tools/export_block_armor.py`, with an authored pixel atlas
and separately installed private growth timing. See `docs/RC30-block-armor.md`
for completed integration and the limits of the source-motion adaptation.
