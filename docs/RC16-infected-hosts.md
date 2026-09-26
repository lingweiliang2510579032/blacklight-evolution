# RC16: consumable infected hosts and articulated block morphology

Converted creatures use blacklight:infected_host. That type was missing from
the consumable entity tag, causing the acquisition filter to reject every
converted host before health checks. The type is now included, including for
existing saved hosts. The common health gate is unchanged: maximum HP below
50 can be grabbed directly; maximum HP at least 50 requires current HP <= 15%.
The gate uses the infected creature's current maximum health, not its source's.

Removes the common surface-vertex cones. Authored cuboid layouts distinguish
17 morphology families, with explicit mappings for 73 vanilla types and
anatomical fallbacks for other standard LivingEntityRenderer hosts. These are
family adaptations of the articulated species body, not 73 separately authored
full replacement skeletons. Species pixels remain visible under a restrained
infection tint; new chitin, sinew and claws use an original pixel atlas.

Growth attaches to actual model-part transforms, including species walk and
hit/consume poses. Cuboids emit at the renderer's return through its current
buffer, before consume fracture finalization, with head/body/limb anatomy tags.
No shared vanilla model parts are permanently mutated. Custom renderers that
do not use LivingEntityRenderer/ModelPart retain the tinted source appearance;
they need dedicated adapters for articulated growth.

Source: tools/author_infected_morphs.py; models/infected_morphology.json;
textures/entity/infected_morphology.png. No original Prototype mesh or texture.

Validation is compile/package only; no automated tests or gameplay acceptance.
Visual fit, especially unusual third-party skeletons, remains for user playtest.
