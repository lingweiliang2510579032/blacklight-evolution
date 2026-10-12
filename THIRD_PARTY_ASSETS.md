# Third-party assets

## Package status and license scope

`LICENSE` contains the project's GPL-3.0-only code license. It does not relicense
separately owned dependency assets or decoded Prototype materials. No permission
to publicly distribute or commercially sell the private Prototype profiles has
been established in this repository. A filename, notice or source-code license
does not provide that permission.

Current Gradle packages are deliberately classified:

- `*-local-preview.jar` / `*-local-preview.zip`: explicitly requested local
  builds containing private profiles. Keep these private; they are not cleared
  for public distribution or commercial submission.
- `*-source-only.jar` / `*-source-only.zip`: compilation/package artifacts with
  the private resource namespace excluded. They are **not a complete playable
  release**: animation registration currently requires original-derived motion
  and armor-growth profiles. Removing the private assets alone does not complete
  a public release.

Both archive types contain `LICENSE`, this file and `THIRD_PARTY_NOTICES.md`.
The JAR manifest records `Blacklight-Build-Profile`, `Blacklight-Private-Assets`
and `Blacklight-Public-Release-Ready: false`. Maven publishing is disabled for
both current profiles. Public release requires reviewed rights and a complete,
playable asset set; these packaging safeguards do not claim either is complete.

## Independent player animation runtime

The current independent runtime has no Epic Fight / Epic ParCool / Weapons of
Miracles dependency or external animation namespace lookup. Historical
adapters have been archived outside the source tree. The project-authored
mathematical bind hierarchy and continuous skin mesh do not copy the earlier
dependency's reference skeleton or mannequin. The generator is
`tools/author_player_bind.py`; tests now use this project's hierarchy.

The removal of the framework does not change the provenance or distribution
status of local Prototype-derived motions, camera and HUD profiles.

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
versioned resources. Its JAR and ZIP names end in `-local-preview`. Omission of
the property produces `-source-only` artifacts. The build explicitly clears
private copies only under its generated/processed resource directories and also
excludes `blacklight/bundled_motions/**` from source-only resource and JAR tasks;
it never deletes the externally supplied profile directory. This boundary does
not rely on an empty-source `Sync` task clearing a previous local build.

Local candidate packages belong in ignored `release/local-*/` directories and
must not be uploaded with public pull requests or releases. Old unclassified
RC17–RC56 build artifacts are not retroactively made public by this policy;
their contents must be inspected rather than trusting their names. The examined
RC56 local JAR contained 224 private files (213 JSON and 11 `.data` files),
including four original HUD textures embedded in `health-hud.data`.

The historical local build workflow is described in
`docs/RC17-bundled-motions.md`; current archive classifiers above take precedence
over its older filenames and counts.

## Project-authored resource provenance

The checked-in resource namespace is distinct from the private profile
directory. The 82 infected meshes are imported from the project's editable
model library by `tools/import_infected_models.py`; its manifest retains source
hashes. Hive textures, the hunter model and armor geometry have source authoring
scripts/layouts in `tools/` and `art/`. `claw_biomass.png` is recorded as a
generated project texture in `docs/VISUAL_FIDELITY_20260911.md`. Runtime native
mob proxies refer to Minecraft's installed models and textures rather than
embedding those game assets in this JAR.

These provenance records describe how assets were made; they are not a grant
over third-party names, designs, reference material or private inputs. A public
asset inventory still needs a per-file review before commercial submission.
No audio files are currently tracked in this project's resource tree.

## Kenney Particle Pack 1.1

- Creator: Kenney Vleugels (Kenney.nl)
- Source: https://kenney.nl/assets/particle-pack
- License: Creative Commons Zero (CC0 1.0)
- License URL: https://creativecommons.org/publicdomain/zero/1.0/
- Imported files: `biomass_slash.png`, `destruction_burst.png`, `impact_smoke.png`
- Modifications: filenames changed; particles are tinted and animated at runtime.

The source pack explicitly permits use in personal and commercial projects. Attribution is
not required, but is retained here to preserve provenance.
