"""Reauthor the 24-tick player morph using the existing editable IK rig."""
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
 if a.name.startswith('action/transform_claws'):bpy.data.actions.remove(a)
stash_and_export('action/transform_claws',1.2,[
 (0,{}),(.16,dict(lean=14,height=-.07,R=(.48,-.24,.94),L=(-.48,-.22,.92))),
 (.34,dict(lean=20,twist=-5,height=-.10,R=(.52,-.30,1.04),L=(-.52,-.28,.98))),
 (.49,dict(lean=14,twist=4,height=-.08,R=(.60,-.30,1.08),L=(-.55,-.28,1.06))),
 (.66,dict(lean=-6,twist=-4,height=-.02,R=(.77,-.15,1.08),L=(-.70,-.15,1.08))),
 (.83,dict(lean=9,height=-.07,R=(.67,-.12,.89),L=(-.64,-.12,.92))),
 (1.05,dict(lean=7,R=(.54,-.10,.84),L=(-.54,-.10,.84))),(1.2,{})])
rig.animation_data.action=bpy.data.actions['action/transform_claws']
for ob in controls.values():ob.animation_data.action=bpy.data.actions['action/transform_claws / '+ob.name]
scene.frame_start=0;scene.frame_end=120;scene.frame_set(66);bpy.context.preferences.filepaths.save_version=0
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'player-morph-authoring.blend'))
print('MORPH_AUTHORED',flush=True)
