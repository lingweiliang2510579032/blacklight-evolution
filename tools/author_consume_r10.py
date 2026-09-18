"""Author original unarmed, hammerfist, blade and whipfist consume animations."""
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

# Original key poses, authored in Blender IK on the installed Epic Fight armature.
clips={
'normal':(2.3,[(0,{}),(.25,dict(lean=12,L=(-.38,-.63,1.12),R=(.50,-.10,1.0))),
 (.48,dict(lean=22,twist=15,R=(.25,-.70,1.05),L=(-.35,-.61,1.12))),
 (.8,dict(lean=8,twist=-10,R=(.32,-.44,1.04),L=(-.32,-.44,1.10))),
 (1.2,dict(lean=-7,R=(.56,-.20,1.12),L=(-.56,-.20,1.12))),
 (1.65,dict(lean=12,height=-.05,R=(.42,-.18,.95),L=(-.42,-.18,.95))),
 (2.3,{})]),
'hammer':(2.8,[(0,{}),(.28,dict(lean=10,height=-.07,L=(-.67,-.56,1.04),R=(.72,.10,1.15))),
 (.62,dict(lean=-8,twist=-16,height=-.07,R=(.85,.01,1.66),L=(-.67,-.55,1.03))),
 (.88,dict(lean=42,twist=18,height=-.20,R=(.58,-.66,.64),L=(-.65,-.50,.77),footR=(.25,-.18,.015),footL=(-.25,.16,.015))),
 (1.1,dict(lean=40,twist=12,height=-.22,R=(.57,-.62,.58),L=(-.65,-.46,.73),footR=(.25,-.18,.015),footL=(-.25,.16,.015))),
 (1.6,dict(lean=-4,height=-.07,R=(.72,-.26,1.0),L=(-.73,-.27,1.0))),
 (2.1,dict(lean=13,height=-.06,R=(.68,-.16,.9),L=(-.68,-.16,.9))),
 (2.8,{})]),
'blade':(2.4,[(0,{}),(.28,dict(twist=-25,lean=7,R=(.77,.20,1.0),L=(-.40,-.55,1.10))),
 (.56,dict(twist=19,lean=20,height=-.06,R=(.57,-.63,1.06),L=(-.40,-.52,1.10))),
 (.85,dict(twist=24,lean=18,height=-.08,R=(.58,-.64,1.02),L=(-.43,-.49,1.08))),
 (1.2,dict(twist=-13,lean=-3,R=(.76,-.22,1.06),L=(-.48,-.30,1.10))),
 (1.65,dict(lean=12,R=(.65,-.19,.96),L=(-.51,-.23,1.0))),
 (2.4,{})]),
'whip':(3.0,[(0,{}),(.22,dict(twist=-38,lean=-6,R=(.85,.22,1.42),L=(-.47,-.12,1.0))),
 (.46,dict(twist=28,lean=20,height=-.05,R=(.62,-.72,1.10),L=(-.51,.02,.95))),
 (.8,dict(twist=17,lean=12,R=(.62,-.57,1.10),L=(-.43,-.36,1.04))),
 (1.14,dict(twist=-20,lean=-10,height=-.055,R=(.77,-.02,1.08),L=(-.44,-.49,1.06))),
 (1.5,dict(twist=-8,lean=8,R=(.55,-.32,1.03),L=(-.43,-.46,1.03))),
 (1.9,dict(lean=-4,R=(.62,-.20,1.10),L=(-.56,-.20,1.06))),
 (2.4,dict(lean=12,height=-.04,R=(.55,-.14,.91),L=(-.53,-.17,.93))),
 (3.0,{})])}
for name,(duration,keys) in clips.items():
 stash_and_export('action/consume_'+name,duration,keys)
rig.animation_data.action=bpy.data.actions['action/consume_hammer']
for ob in controls.values():ob.animation_data.action=bpy.data.actions['action/consume_hammer / '+ob.name]
scene.frame_start=0;scene.frame_end=280;scene.frame_set(88);bpy.context.preferences.filepaths.save_version=0
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'consume-forms-authoring.blend'))
print('R10_CONSUME_FORMS_AUTHORED',flush=True)
