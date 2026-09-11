"""Upgrade R5 Blender meshes without changing any baked motion or IK curve.

Run in Blender with arguments SOURCE_OUTPUT NEW_OUTPUT. This one-time migration
matches build_claw_blender.py's anatomical chirality fix.
"""
import bpy,bmesh,sys,ast,json
from pathlib import Path
from mathutils import Matrix,Vector
source,out=map(Path,sys.argv[sys.argv.index('--')+1:][:2]);out.mkdir(parents=True,exist_ok=True)
ROOT=Path(__file__).resolve().parents[1];RES=ROOT/'src/main/resources/assets/blacklight'
S=Matrix.Diagonal((1,-1,1,1));X=Matrix.Diagonal((-1,1,1,1))
for filename in ['claw-study-authoring.blend','claw-study.blend']:
    bpy.ops.wm.open_mainfile(filepath=str(source/filename));rig=bpy.data.objects['Blacklight_EpicFight']
    claw_objects=[o for o in bpy.data.objects if 'runtime_joint' in o]
    for ob in claw_objects:
        bone=ob['runtime_joint']
        if not bone.startswith('Hand_'):continue
        rest=rig.data.bones[bone].matrix_local;correction=S@rest@X@rest.inverted()@S
        for v in ob.data.vertices:v.co=correction@v.co
        bm=bmesh.new();bm.from_mesh(ob.data);bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));bm.to_mesh(ob.data);bm.free()
    bpy.context.preferences.filepaths.save_version=0
    bpy.ops.wm.save_as_mainfile(filepath=str(out/filename))
    if filename=='claw-study.blend':
        mats=list(claw_objects[0].data.materials)
        tree=ast.parse((ROOT/'tools/build_claw_blender.py').read_text(encoding='utf-8'))
        fn=next(n for n in tree.body if isinstance(n,ast.FunctionDef) and n.name=='export_meshes')
        exec(compile(ast.Module(body=[fn],type_ignores=[]),'<export>','exec'));export_meshes()
        scene=bpy.context.scene;rig.animation_data.action=bpy.data.actions['living/claw_idle'];scene.frame_set(0)
        for name,loc in [('front',(0,5,1.15)),('side',(5,0,1.15)),('three-quarter',(3,5,2.4))]:
            scene.camera.location=loc;scene.camera.rotation_euler=(Vector((0,0,.94))-scene.camera.location).to_track_quat('-Z','Y').to_euler()
            scene.render.filepath=str(out/f'model-{name}.png');bpy.ops.render.render(write_still=True)
print('HANDEDNESS_FIXED',flush=True)
