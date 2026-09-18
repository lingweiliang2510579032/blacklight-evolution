"""Author a close-contact claw consume: left restraint, right puncture, spread, biomass recoil."""
import bpy,ast,math,json,sys,importlib.util
from pathlib import Path
from mathutils import Matrix,Vector
WORK,SOURCE,OUT=map(Path,sys.argv[sys.argv.index('--')+1:][:3]);OUT.mkdir(parents=True,exist_ok=True)
ROOT=Path(__file__).resolve().parents[1];RES=ROOT/'src/main/resources/assets/blacklight'
bpy.ops.wm.open_mainfile(filepath=str(SOURCE/'claw-study-authoring.blend'))
scene=bpy.context.scene;rig=bpy.data.objects['Blacklight_EpicFight'];controls={o.name:o for o in bpy.data.objects if o.name.startswith('CTRL /')}
data=json.loads((WORK/'epic-biped.json').read_text())
spec=importlib.util.spec_from_file_location('epic_vendor',WORK/'epic-blender-addon/__init__.py',submodule_search_locations=[str(WORK/'epic-blender-addon')]);vendor=importlib.util.module_from_spec(spec);sys.modules[spec.name]=vendor;vendor.__path__=[str(WORK/'epic-blender-addon')]
from epic_vendor import compat
tree=ast.parse((ROOT/'tools/build_claw_blender.py').read_text(encoding='utf-8'))
for name in ['set_pose','clear_animation','stash_and_export','clip_text']:
 fn=next(n for n in tree.body if isinstance(n,ast.FunctionDef) and n.name==name);exec(compile(ast.Module(body=[fn],type_ignores=[]),'<author>','exec'))
clear_animation()
for a in list(bpy.data.actions):
 if a.name.startswith('action/consume'):bpy.data.actions.remove(a)
stash_and_export('action/consume',2.3,[
 (0,{}),(.22,dict(lean=12,twist=-10,R=(.68,.06,1.06),L=(-.63,-.54,1.10))),
 (.42,dict(lean=19,twist=12,height=-.06,R=(.47,-.67,1.14),L=(-.63,-.58,1.12))),
 (.65,dict(lean=16,twist=8,height=-.07,R=(.47,-.66,1.11),L=(-.63,-.57,1.10))),
 (.9,dict(lean=-3,twist=-7,height=-.055,R=(.62,-.39,1.10),L=(-.65,-.40,1.10))),
 (1.15,dict(lean=-6,height=-.06,R=(.71,-.30,1.16),L=(-.68,-.31,1.16))),
 (1.42,dict(lean=14,height=-.085,R=(.62,-.26,1.01),L=(-.61,-.25,1.03))),
 (1.72,dict(lean=10,height=-.055,R=(.57,-.19,.92),L=(-.56,-.18,.92))),
 (2.05,dict(lean=7,R=(.53,-.11,.85),L=(-.53,-.11,.85))),(2.3,{})])
rig.animation_data.action=bpy.data.actions['action/consume']
for ob in controls.values():ob.animation_data.action=bpy.data.actions['action/consume / '+ob.name]
scene.frame_start=0;scene.frame_end=230;scene.frame_set(42);bpy.context.preferences.filepaths.save_version=0
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'player-consume-authoring.blend'))
print('CONSUME_AUTHORED',flush=True)
