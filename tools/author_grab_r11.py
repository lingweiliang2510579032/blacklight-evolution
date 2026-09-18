"""Author original reach, whip capture, sustained hold, throw and close-range consumption."""
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

# Original grab/hold/throw phases. No motion auto-transitions from hold to consumption.
hold=dict(lean=7,twist=-6,L=(-.62,-.60,1.16),R=(.68,-.12,1.0))
clips={
'grab_reach':(.6,[(0,{}),(.16,dict(lean=12,L=(-.64,-.32,1.12),R=(.68,-.08,1.05))),(.38,hold),(.6,hold)]),
'grab_hold':(2.,[(0,hold),(.5,dict(hold,lean=8,L=(-.63,-.60,1.18))),
 (1.,dict(hold,lean=6,L=(-.62,-.61,1.15))),(1.5,dict(hold,lean=8)),(2.,hold)]),
'grab_throw':(.7,[(0,hold),(.18,dict(lean=-6,twist=22,L=(-.83,.04,1.42),R=(.76,-.12,1.02))),
 (.3,dict(lean=18,twist=-12,L=(-.84,-.67,.90),R=(.72,-.07,1.02))),
 (.48,dict(lean=18,twist=-18,L=(-.70,-.46,.97),R=(.72,-.04,.99))),(.7,{})]),
'grab_whip':(1.8,[(0,{}),(.22,dict(twist=-38,lean=-6,R=(.85,.22,1.42),L=(-.47,-.12,1.0))),
 (.46,dict(twist=28,lean=20,height=-.05,R=(.62,-.72,1.10),L=(-.51,.02,.95))),
 (.8,dict(twist=17,lean=12,R=(.62,-.57,1.10),L=(-.43,-.36,1.04))),
 (1.14,dict(twist=-20,lean=-10,height=-.055,R=(.77,-.02,1.08),L=(-.44,-.49,1.06))),
 (1.5,hold),(1.8,hold)]),
'consume_whip':(1.9,[(0,hold),(.28,dict(twist=-8,lean=8,R=(.55,-.32,1.03),L=(-.63,-.58,1.13))),
 (.65,dict(lean=-4,R=(.62,-.20,1.10),L=(-.63,-.43,1.10))),
 (1.2,dict(lean=12,height=-.04,R=(.55,-.14,.91),L=(-.56,-.25,.98))),(1.9,{})])}
for name,(duration,keys) in clips.items():stash_and_export('action/'+name,duration,keys)
rig.animation_data.action=bpy.data.actions['action/grab_hold']
for ob in controls.values():ob.animation_data.action=bpy.data.actions['action/grab_hold / '+ob.name]
scene.frame_start=0;scene.frame_end=200;scene.frame_set(0);bpy.context.preferences.filepaths.save_version=0
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'grab-authoring.blend'))
print('R11_GRAB_AUTHORED',flush=True)
