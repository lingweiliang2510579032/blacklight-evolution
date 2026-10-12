# Rebuilt block shield

54 authored cuboids with stepped carapace, spine, side barbs and forearm roots. Shares the six-material original pixel atlas with armor-block-v4. Run `python tools/author_shield_block.py` from the repository to regenerate the Blockbench files and runtime geometry.

Damage stages retain 54, 44 and 32 cuboids; the broken state retains three attachment pieces. The 60% and 30% thresholds follow the locally inspected source definition. Shapes, missing cells and render effects are authored Minecraft adaptations. No original game mesh, texture or animation is stored here.

Runtime geometry uses calibrated wrist-local axes (asset revision 4). The renderer binds directly to Tool_L and applies the independent native L_Wrist_Grapple track from the selected private body clip. The old Hand_L attachment and inverse wrist-rest transform are removed. Model center/size remain authored adaptations. Blockbench coordinates remain upright.
