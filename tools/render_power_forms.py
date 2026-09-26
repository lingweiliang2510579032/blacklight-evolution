"""Blender-native, same-scale before/after studies of runtime weapon surfaces.
Usage: blender -b --python tools/render_power_forms.py -- BEFORE_DIR OUTPUT_DIR
"""
import bpy,json,sys,math
from pathlib import Path
from mathutils import Vector
before,out=[Path(p).resolve() for p in sys.argv[sys.argv.index('--')+1:]];out.mkdir(parents=True,exist_ok=True)
root=Path(__file__).resolve().parents[1]
bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete(use_global=False)
scene=bpy.context.scene;scene.render.engine='BLENDER_WORKBENCH'
scene.render.resolution_x=1200;scene.render.resolution_y=900;scene.render.resolution_percentage=100
scene.render.image_settings.file_format='PNG'
scene.display.shading.light='STUDIO';scene.display.shading.studio_light='paint.sl'
scene.display.shading.color_type='MATERIAL';scene.display.shading.show_shadows=True
scene.display.shading.show_cavity=True;scene.display.shading.cavity_type='BOTH'
scene.display.shading.background_type='WORLD';scene.world.color=(.11,.13,.16)
scene.view_settings.view_transform='Standard'
bpy.ops.object.camera_add(location=(2.4,-6,1.5));camera=bpy.context.object
camera.rotation_euler=(Vector((0,0,-.05))-camera.location).to_track_quat('-Z','Y').to_euler()
camera.data.type='ORTHO';camera.data.ortho_scale=3.1;scene.camera=camera
cache={}
def material(c):
 c=tuple(c)
 if c not in cache:
  linear=lambda v: v/3294.6 if v<=10 else ((v/255+.055)/1.055)**2.4
  m=bpy.data.materials.new('biomass_'+str(c));m.diffuse_color=(*(linear(v) for v in c),1);cache[c]=m
 return cache[c]
collections=[];latest_revision=0
for form in ['claw_blender','form_hammerfists','form_blade','form_whipfist']:
 col=bpy.data.collections.new(form);scene.collection.children.link(col);collections.append(col)
 for title,folder,x in [('R13',before,-.7),('R15',root/'src/main/resources/assets/blacklight/models',.7)]:
  data=json.loads((folder/f'{form}.json').read_text())
  title='R'+str(data.get('designRevision',13))
  latest_revision=max(latest_revision,data.get('designRevision',13))
  for joint in ['Arm_R','Hand_R']:
   faces=data['joints'][joint];verts=[];polys=[];colors=[];normals=[]
   for face in faces:
    start=len(verts)
    for vx,vy,vz in face['p']:verts.append((vx,vz,.85-vy-(.3 if joint.startswith('Hand') else 0)))
    polys.append((start,start+1,start+2));colors.append(tuple(face['c']))
    normals.extend([(nx,nz,-ny) for nx,ny,nz in face.get('n',[(0,0,0)]*3)])
   mesh=bpy.data.meshes.new(title+'_'+joint);mesh.from_pydata(verts,[],polys);mesh.update()
   for polygon,face in zip(mesh.polygons,faces):polygon.use_smooth='n' in face
   if any('n' in face for face in faces):mesh.normals_split_custom_set(normals)
   obj=bpy.data.objects.new(title+'_'+joint,mesh);col.objects.link(obj)
   obj.location.x=x
   # Show each revision's broad blade face for silhouette comparison; keep the
   # runtime axis correction as an explicit object rotation in the study file.
   if form=='form_blade' and data.get('designRevision',0)>=16:obj.rotation_euler.z=math.radians(data.get('mountDegrees',0))
   mats=list(dict.fromkeys(colors))
   for c in mats:mesh.materials.append(material(c))
   lookup={c:i for i,c in enumerate(mats)}
   for p,c in zip(mesh.polygons,colors):p.material_index=lookup[c]
 for c in collections:c.hide_render=c!=col
 scene.render.filepath=str(out/f'{form}-comparison.png');bpy.ops.render.render(write_still=True)
for col in collections:
 col.hide_render=col.name!='form_blade'
 col.hide_viewport=col.name!='form_blade'
bpy.ops.wm.save_as_mainfile(filepath=str(out/f'reference-weapons-r{latest_revision}.blend'))
