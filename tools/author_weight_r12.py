"""Original contact-led grab/consume poses. Run after R11; preserves gameplay durations.

Blender seconds: hold -> load -> accelerate -> contact settle -> recoil -> recover.
The close contact keys deliberately occupy 60-100 ms, rather than smoothing an
entire strike over 250-350 ms. No animation or mesh is extracted from Prototype.
"""
import bpy, ast, math, json, sys, importlib.util
from pathlib import Path
from mathutils import Matrix, Vector

WORK,SOURCE,OUT=map(Path,sys.argv[sys.argv.index('--')+1:][:3])
OUT.mkdir(parents=True,exist_ok=True)
ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources/assets/blacklight'
bpy.ops.wm.open_mainfile(filepath=str(SOURCE/'claw-study-authoring.blend'))
scene=bpy.context.scene
rig=bpy.data.objects['Blacklight_EpicFight']
controls={o.name:o for o in bpy.data.objects if o.name.startswith('CTRL /')}
data=json.loads((WORK/'epic-biped.json').read_text())
spec=importlib.util.spec_from_file_location('epic_vendor',WORK/'epic-blender-addon/__init__.py',submodule_search_locations=[str(WORK/'epic-blender-addon')])
vendor=importlib.util.module_from_spec(spec)
sys.modules[spec.name]=vendor
vendor.__path__=[str(WORK/'epic-blender-addon')]
from epic_vendor import compat
tree=ast.parse((ROOT/'tools/build_claw_blender.py').read_text(encoding='utf-8'))
for name in ['set_pose','clear_animation','stash_and_export','clip_text']:
    fn=next(n for n in tree.body if isinstance(n,ast.FunctionDef) and n.name==name)
    exec(compile(ast.Module(body=[fn],type_ignores=[]),'<author>','exec'))

# Keep the restrained hand outside the face and continue the actual carry pose.
hold=dict(lean=7,twist=-6,L=(-.62,-.60,1.16),R=(.68,-.12,1.0))
brace=dict(footR=(.23,-.16,.015),footL=(-.23,.14,.015))
def pose(**kw):
    return dict(hold,**brace) | kw

hammer_load=pose(lean=-10,twist=-20,height=-.10,R=(.89,.08,1.63),L=(-.67,-.55,1.05))
hammer_hit=pose(lean=42,twist=18,height=-.23,R=(.59,-.66,.60),L=(-.65,-.50,.77))
blade_load=pose(lean=-3,twist=-29,height=-.065,R=(.83,.22,1.07))
blade_hit=pose(lean=23,twist=23,height=-.11,R=(.61,-.65,1.06))
normal_load=pose(lean=-4,twist=-20,height=-.07,R=(.65,.04,1.04))
normal_hit=pose(lean=24,twist=20,height=-.12,R=(.35,-.69,1.04))
claw_load=pose(lean=-6,twist=-24,height=-.07,R=(.80,.03,1.12))
claw_hit=pose(lean=25,twist=20,height=-.12,R=(.66,-.66,1.02))
whip_hit=pose(lean=19,twist=17,height=-.10,R=(.63,-.55,1.02))

clips={
 'grab_reach':(.6,[(0,{}),(.12,dict(lean=3,twist=7,height=-.055,L=(-.68,-.16,1.10),R=(.72,.01,1.02))),
     (.22,dict(hold,lean=16,twist=-10,height=-.10)),(.28,dict(hold,lean=16,twist=-10,height=-.10)),
     (.43,dict(hold,lean=5,height=-.04)),(.6,hold)]),
 'grab_hold':(2.,[(0,hold),(.36,dict(hold,lean=8,L=(-.63,-.61,1.17))),
     (.72,dict(hold,lean=6,twist=-5)),(1.12,dict(hold,lean=8,twist=-7,L=(-.62,-.60,1.15))),
     (1.6,dict(hold,lean=6)),(2.,hold)]),
 'grab_throw':(.7,[(0,hold),(.12,pose(lean=-5,twist=18,height=-.09,L=(-.82,-.08,1.30))),
     (.22,pose(lean=-8,twist=26,height=-.11,L=(-.86,.02,1.39))),
     (.30,pose(lean=22,twist=-22,height=-.13,L=(-.86,-.72,.89))),
     (.37,pose(lean=25,twist=-29,height=-.12,L=(-.79,-.57,.79))),
     (.5,pose(lean=17,twist=-15,height=-.07,L=(-.70,-.28,.86))),(.7,{})]),
 'grab_whip':(1.8,[(0,{}),(.22,pose(twist=-35,lean=-8,height=-.08,R=(.89,.24,1.40))),
     (.37,pose(twist=-39,lean=-9,height=-.10,R=(.90,.25,1.43))),
     (.46,pose(twist=26,lean=20,height=-.10,R=(.74,-.72,1.09))),
     (.53,pose(twist=26,lean=20,height=-.10,R=(.74,-.72,1.09))),
     (.72,pose(twist=18,lean=10,height=-.07,R=(.65,-.59,1.10))),
     (1.02,pose(twist=-22,lean=-12,height=-.12,R=(.81,-.03,1.10))),
     (1.15,pose(twist=-24,lean=-13,height=-.13,R=(.82,.01,1.11))),
     (1.42,dict(hold,lean=12,height=-.07)),(1.8,hold)]),
 'consume_normal':(2.3,[(0,hold),(.22,pose(lean=3,twist=-15,height=-.065)),
     (.42,normal_load),(.50,normal_hit),(.58,normal_hit),
     (.77,pose(lean=13,twist=27,height=-.10,R=(.43,-.55,1.01))),
     (1.05,pose(lean=-9,twist=-14,height=-.06,R=(.62,-.23,1.10),L=(-.64,-.38,1.10))),
     (1.35,pose(lean=15,height=-.12,R=(.56,-.19,.95),L=(-.61,-.23,.98))),
     (1.7,pose(lean=9,height=-.06,R=(.61,-.14,.95),L=(-.63,-.15,.99))),(2.3,{})]),
 'consume':(2.3,[(0,hold),(.24,pose(lean=1,twist=-17,height=-.06)),
     (.46,claw_load),(.55,claw_hit),(.63,claw_hit),
     (.82,pose(lean=19,twist=26,height=-.10,R=(.72,-.45,.97))),
     (1.08,pose(lean=-9,twist=-17,height=-.065,R=(.80,-.18,1.08),L=(-.71,-.34,1.08))),
     (1.36,pose(lean=15,height=-.12,R=(.66,-.15,.91),L=(-.68,-.20,.98))),
     (1.75,pose(lean=8,height=-.05,R=(.66,-.12,.97),L=(-.64,-.16,1.0))),(2.3,{})]),
 'consume_hammer':(2.8,[(0,hold),(.26,pose(lean=5,twist=-13,height=-.10,R=(.79,.08,1.15))),
     (.62,hammer_load),(.80,dict(hammer_load,lean=-12,height=-.12)),
     (.90,hammer_hit),(1.00,hammer_hit),
     (1.16,dict(hammer_hit,lean=35,height=-.19,R=(.62,-.57,.71))),
     (1.46,pose(lean=-5,twist=-8,height=-.10,R=(.75,-.25,1.03),L=(-.73,-.28,1.03))),
     (1.85,pose(lean=16,height=-.13,R=(.70,-.17,.89),L=(-.70,-.17,.94))),
     (2.25,pose(lean=7,height=-.045,R=(.69,-.10,.98),L=(-.68,-.10,.98))),(2.8,{})]),
 'consume_blade':(2.4,[(0,hold),(.24,pose(lean=4,twist=-20,height=-.06)),
     (.51,blade_load),(.60,blade_hit),(.68,blade_hit),
     (.86,pose(lean=24,twist=31,height=-.12,R=(.64,-.51,.96))),
     (1.12,pose(lean=-8,twist=-16,height=-.07,R=(.80,-.15,1.08),L=(-.67,-.38,1.09))),
     (1.48,pose(lean=15,height=-.12,R=(.69,-.15,.95),L=(-.65,-.23,1.0))),
     (1.88,pose(lean=8,height=-.05,R=(.68,-.12,.98),L=(-.65,-.16,1.0))),(2.4,{})]),
 'consume_whip':(1.9,[(0,hold),(.20,pose(lean=-6,twist=-19,height=-.09,R=(.78,.02,1.11))),
     (.36,pose(lean=-10,twist=-24,height=-.11,R=(.80,.03,1.12))),
     (.45,whip_hit),(.53,whip_hit),
     (.72,pose(lean=-6,twist=-15,height=-.07,R=(.74,-.14,1.10),L=(-.66,-.40,1.10))),
     (1.02,pose(lean=15,height=-.12,R=(.65,-.16,.93),L=(-.64,-.23,.98))),
     (1.42,pose(lean=8,height=-.05,R=(.65,-.12,.98),L=(-.63,-.17,1.0))),(1.9,{})])
}
for name,(duration,keys) in clips.items():
    stash_and_export('action/'+name,duration,keys)
rig.animation_data.action=bpy.data.actions['action/consume_hammer']
for ob in controls.values():
    ob.animation_data.action=bpy.data.actions['action/consume_hammer / '+ob.name]
scene.frame_start=0;scene.frame_end=280;scene.frame_set(90)
bpy.context.preferences.filepaths.save_version=0
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'weight-authoring.blend'))
print('R12_WEIGHT_AUTHORED',flush=True)
