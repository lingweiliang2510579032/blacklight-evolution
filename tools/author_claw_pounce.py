"""Original P2-inspired pounce on the existing editable Epic Fight rig.

No Prototype clip is copied. Authoring targets use the project's -Y-front
convention; set_pose reflects it once to the shipped rig's +Y front.
Run Blender -b --python this_file -- WORK SOURCE_BLEND OUTPUT.
"""
import bpy, ast, math, json, sys, importlib.util
from pathlib import Path
from mathutils import Matrix

WORK,SOURCE,OUT=map(Path,sys.argv[sys.argv.index('--')+1:][:3])
ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources/assets/blacklight/authored_motions'
RES.mkdir(parents=True,exist_ok=True);OUT.mkdir(parents=True,exist_ok=True)
bpy.ops.wm.open_mainfile(filepath=str(SOURCE))
scene=bpy.context.scene;scene.render.fps=100
rig=bpy.data.objects['Blacklight_EpicFight']
controls={o.name:o for o in bpy.data.objects if o.name.startswith('CTRL /')}
data=json.loads((WORK/'epic-biped.json').read_text())
spec=importlib.util.spec_from_file_location('epic_vendor',WORK/'epic-blender-addon/__init__.py',submodule_search_locations=[str(WORK/'epic-blender-addon')])
vendor=importlib.util.module_from_spec(spec);sys.modules[spec.name]=vendor
vendor.__path__=[str(WORK/'epic-blender-addon')]
from epic_vendor import compat
tree=ast.parse((ROOT/'tools/build_claw_blender.py').read_text(encoding='utf8'))
for name in ['set_pose','clear_animation']:
 fn=next(n for n in tree.body if isinstance(n,ast.FunctionDef) and n.name==name)
 exec(compile(ast.Module(body=[fn],type_ignores=[]),'<author>','exec'))

# Gather the knees under the pelvis, open the elbows behind the shoulders,
# then project both claws while the legs trail. A separate raking stroke and
# recoil close the attack; neither foot is ever presented as the weapon.
guard=dict(lean=12,R=(.55,-.12,1.05),L=(-.55,-.12,1.05),footR=(.18,.08,.20),footL=(-.18,-.08,.25))
load=dict(lean=28,height=-.14,R=(.54,.24,1.12),L=(-.54,.24,1.12),footR=(.22,-.22,.55),footL=(-.22,-.18,.48))
flight=dict(root_pitch=32,lean=20,height=-.05,R=(.46,-.78,1.30),L=(-.46,-.78,1.30),footR=(.22,.55,.55),footL=(-.22,.43,.42))
hit=dict(root_pitch=24,lean=32,height=-.1,R=(.56,-.78,1.00),L=(-.56,-.78,1.00),footR=(.25,.42,.30),footL=(-.25,.30,.22))
rake=dict(root_pitch=12,lean=30,height=-.14,R=(.80,-.32,.70),L=(-.80,-.32,.70),footR=(.22,.20,.23),footL=(-.22,-.06,.17))
clips={
 'claw_pounce_charge':[(0,guard),(.18,load),(.30,load)],
 'claw_pounce':[(0,guard),(.30,load),(.40,flight),(.48,flight),(.50,hit),(.65,rake),(.80,rake),(.98,guard),(1.25,guard)]
}
for name,keys in clips.items():
 clear_animation()
 for t,args in keys:set_pose(round(t*100),**args)
 rig.animation_data.action.name=name;rig.animation_data.action.use_fake_user=True
 for ob in [rig,*controls.values()]:
  if ob.animation_data:
   for fc in compat.get_fcurves_from_action(ob.animation_data.action):
    for k in fc.keyframe_points:k.interpolation='BEZIER';k.handle_left_type='AUTO_CLAMPED';k.handle_right_type='AUTO_CLAMPED'
 tracks=json.loads(json.dumps(compat.export_animation(rig,data['armature']['joints'],'MAT',bake=True),cls=compat.NoIndentEncoder))
 end=keys[-1][0]
 tracks=[t for t in tracks if t['name']!='Coord']
 tracks.append(dict(name='Coord',time=[0,end],transform=[[1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1]]*2))
 meta=dict(durationTicks=math.ceil((end+.05)*20-.00001),transitionSeconds=.05)
 if name=='claw_pounce':
  meta.update(contactStartTick=11,contactEndTick=15,charge=dict(windupTicks=6,holdTicks=12,releaseTime=.30,animation='claw_pounce_charge'))
  # Bounds follow the two visible forearms and claw tips on each active frame.
  bounds=[]
  for tick in range(11,15):
   points=[]
   for frame in range((tick-2)*5,(tick-1)*5+1):
    scene.frame_set(frame);bpy.context.view_layer.update()
    from mathutils import Vector
    for side in ('R','L'):
     mat=rig.pose.bones['Hand_'+side].matrix
     for local in [(0,0,0),(0,.3,0),(0,.65,0)]:
      v=mat@Vector(local);points.append((v.x,v.z,v.y))
   bounds.append(dict(tick=tick,min=[round(min(v[i] for v in points)-.17,5) for i in range(3)],max=[round(max(v[i] for v in points)+.17,5) for i in range(3)]))
  meta['contactBounds']=bounds
 result=dict(format='MATRIX',animation=tracks,blacklight=meta,authorship='Original Blacklight animation, P2-inspired claw pounce; not an extracted clip.')
 (RES/(name+'.json')).write_text(json.dumps(result,separators=(',',':')),encoding='utf8')
 print(name,meta,flush=True)
bpy.context.preferences.filepaths.save_version=0
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'claw-pounce.blend'))
