# Third-party assets

## Epic Fight reference data (not bundled)

Epic Fight 21.17.3.1 declares its code as GPL-3.0 and its assets as All Rights
Reserved. The dependency is obtained separately through Gradle; this repository
does not redistribute its reference skeleton, mannequin, or mod JAR. Tests read
the installed dependency locally. Blender preparation also reads local inputs
into the ignored `art-work/` directory; see `docs/ASSET_WORKFLOW.md`.

Claw geometry and the authored animation tracks in the Blacklight resource
namespace are this project's work. This statement does not grant permission to
redistribute Epic Fight's own assets or commercial Prototype game assets.

## Runtime motion adapters (Epic Fight / Weapons of Miracles)

The nine `AdaptedWeaponAnimation` actions read joint keyframes from separately
installed dependencies at runtime, retime them, and fit the arms to Blacklight's
meshes in memory. They do not copy source attack events, colliders, skills, or
animation files into this repository or its JAR. These dependency motions are
not claimed as original Blacklight animation assets.

- Epic Fight: 21.17.3.1, sword and fist motions; required existing dependency.
- Weapons of Miracles by reascer: 2.0.178, Torment motions; optional dependency.
  Source: https://www.curseforge.com/minecraft/mc-mods/weapons-of-miracles-epicfight/files/8829395
  License: All Rights Reserved. Obtain its JAR separately; do not bundle it.
- If WOM is absent, its three adapted moves use the existing authored Blacklight
  clips. Install matching dependency versions on both client and server.

Temporary runtime pose dumps used for local geometry checks are under ignored
`tmp/` directories and are not release assets.

## Private Prototype profiles (not publicly distributed)

Local development can read profiles decoded from the user's local Prototype
files. These include the original HUD textures and layout in `health-hud.data`,
camera parameters and timelines in `camera-profile.data`, `camera-events.data`
and `camera-film.data`, and consume receiver choreography in
`consume-placement.data`, `receiver-poses.data` and
`consume-receiver-events.data`. These profiles and the decoded motion files are
not original GPL assets of this project.

Original game archives, extracted assets and generated private profiles are
excluded from public commits and public releases. The extraction tools and
runtime readers are source code; including those tools does not include their
private inputs or outputs. The authored infected models and hive textures are
separate project assets.

A normal `assemble` build and the default CI do not bundle the private profiles.
Only an explicitly requested local build with
`-PbundleMotionDirectory=<private profile directory>` stages them from outside
versioned resources. Such local candidate packages remain in ignored
`release/local-*/` directories and are not uploaded with public pull requests or
releases. See `docs/RC17-bundled-motions.md` for the local build workflow.

## Kenney Particle Pack 1.1

- Creator: Kenney Vleugels (Kenney.nl)
- Source: https://kenney.nl/assets/particle-pack
- License: Creative Commons Zero (CC0 1.0)
- License URL: https://creativecommons.org/publicdomain/zero/1.0/
- Imported files: `biomass_slash.png`, `destruction_burst.png`, `impact_smoke.png`
- Modifications: filenames changed; particles are tinted and animated at runtime.

The source pack explicitly permits use in personal and commercial projects. Attribution is
not required, but is retained here to preserve provenance.
