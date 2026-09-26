# RC15: Minecraft-style Hunter

Replaces the rejected rounded Hunter with 103 original cuboids and a 512 x 512
pixel atlas. The crouched silhouette retains a broad shoulder girdle, low head,
long forelimbs, broad hands, short claws and bent rear legs. No original-game
mesh, texture or reference image is loaded by the authoring scripts.
Stepped shoulder/chest planes, pectoral and abdominal pixel patterns, tendon
planes, knuckles, kneecaps, cheekbones, brow, mouth cavity and individual block
teeth refine the silhouette while retaining rigid cuboids and crisp texels.

Uses ordinary GeckoLib cubes and per-face UVs, with the existing 12 anatomical
bones. Previous hit/consume pose bindings and anatomical fracture rendering
are retained. Local run/swipe swings are reduced for the new crouched rest
shape. This is a visual adaptation, not a claim of identical original geometry.

Sources: tools/author_hunter_model.py, tools/preview_hunter_blocks.py,
art/hunter/hunter_authored.blend. Geometry JSON opens in Blockbench.

Validation: assemble only. No automated tests, GameTest or gameplay acceptance.
Blender images show the actual authored boxes/atlas, not in-game screenshots.
User playtest and visual acceptance remain pending.
