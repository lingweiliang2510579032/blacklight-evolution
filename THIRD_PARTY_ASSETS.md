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

## Kenney Particle Pack 1.1

- Creator: Kenney Vleugels (Kenney.nl)
- Source: https://kenney.nl/assets/particle-pack
- License: Creative Commons Zero (CC0 1.0)
- License URL: https://creativecommons.org/publicdomain/zero/1.0/
- Imported files: `biomass_slash.png`, `destruction_burst.png`, `impact_smoke.png`
- Modifications: filenames changed; particles are tinted and animated at runtime.

The source pack explicitly permits use in personal and commercial projects. Attribution is
not required, but is retained here to preserve provenance.
