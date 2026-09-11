"""Extend the R6 editable rig with wall run, climb, cling and upward leap actions.
Blender arguments: WORK SOURCE_OUTPUT NEW_OUTPUT. Existing non-wall actions are retained.
"""
import bpy,ast,math,json,sys,importlib.util
from pathlib import Path
from mathutils import Matrix,Vector
WORK,SOURCE,OUT=map(Path,sys.argv[sys.argv.index('--')+1:][:3]);OUT.mkdir(parents=True,exist_ok=True)
ROOT=Path(__file__).resolve().parents[1];RES=ROOT/'src/main/resources/assets/blacklight'
bpy.ops.wm.open_mainfile(filepath=str(SOURCE/'claw-study-authoring.blend'))
scene=bpy.context.scene;rig=bpy.data.objects['Blacklight_EpicFight']
controls={o.name:o for o in bpy.data.objects if o.name.startswith('CTRL /')}
data=json.loads((WORK/'epic-biped.json').read_text())
spec=importlib.util.spec_from_file_location('epic_vendor',WORK/'epic-blender-addon/__init__.py',submodule_search_locations=[str(WORK/'epic-blender-addon')])
vendor=importlib.util.module_from_spec(spec);sys.modules[spec.name]=vendor;vendor.__path__=[str(WORK/'epic-blender-addon')]
from epic_vendor import compat
tree=ast.parse((ROOT/'tools/build_claw_blender.py').read_text(encoding='utf-8'))
for name in ['set_pose','clear_animation','stash_and_export','clip_text']:
    fn=next(n for n in tree.body if isinstance(n,ast.FunctionDef) and n.name==name)
    exec(compile(ast.Module(body=[fn],type_ignores=[]),'<author>','exec'))
clear_animation()
for a in list(bpy.data.actions):
    if a.name.startswith(('living/wall_run','living/wall_climb','living/wall_cling','living/wall_leap')):bpy.data.actions.remove(a)
for name,duration in [('wall_run',.48),('wall_climb',.96)]:
    keys=[]
    for i in range(9):
        phase=i/8;run=name=='wall_run'
        args=dict(lean=22 if run else 12,height=-.06 if run else -.10,twist=math.sin(phase*math.tau)*(4 if run else 2))
        for side,sign,offset in [('R',1,0),('L',-1,.5)]:
            p=(phase+offset)%1
            if run:
                z=.18+.32*(1-2*p if p<.5 else 2*p-1)
                y=-.32+(0 if p<.5 else .16*math.sin((p-.5)*math.tau))
                args['foot'+side]=(sign*.17,y,z)
                args[side]=(sign*.45,.07+.13*math.sin(p*math.tau),.94+.11*math.cos(p*math.tau))
            else:
                args['foot'+side]=(sign*.18,-.30,.17+.22*(1+math.sin(p*math.tau)))
                # Long claws need a wide grip ahead of the face. A narrow wrist
                # target turns the forearms inward and drives the blades into it.
                args[side]=(sign*.62,-.48,1.40+.20*math.sin(p*math.tau))
        keys.append((round(phase*duration,3),args))
    stash_and_export('living/'+name,duration,keys)
cling=dict(lean=10,height=-.10,R=(.62,-.48,1.48),L=(-.62,-.48,1.36),footR=(.18,-.30,.34),footL=(-.18,-.30,.18))
stash_and_export('living/wall_cling',1,[(0,cling),(1,cling)])
stash_and_export('living/wall_leap',.40,[
    (0,dict(lean=20,height=-.16,R=(.45,.13,.88),L=(-.45,.13,.88),footR=(.18,-.26,.42),footL=(-.18,-.26,.34))),
    (.08,dict(lean=16,height=-.02,R=(.62,-.24,1.10),L=(-.62,-.24,1.10),footR=(.17,-.32,.18),footL=(-.17,-.30,.26))),
    (.22,dict(lean=14,height=-.04,R=(.64,-.48,1.38),L=(-.64,-.46,1.18),footR=(.17,-.22,.50),footL=(-.17,-.30,.22))),
    (.40,dict(lean=20,height=-.06,R=(.45,.05,.94),L=(-.45,.10,1.02),footR=(.17,-.32,.46),footL=(-.17,-.24,.22)))])
rig.animation_data.action=bpy.data.actions['living/wall_cling']
for ob in controls.values():ob.animation_data.action=bpy.data.actions['living/wall_cling / '+ob.name]
scene.frame_start=0;scene.frame_end=100;scene.frame_set(0)
bpy.context.preferences.filepaths.save_version=0
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'claw-study.blend'))
print('WALL_ACTIONS_AUTHORED',flush=True)
