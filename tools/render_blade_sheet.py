"""Render three views of the ACTUAL runtime blade mesh, no generated concept art."""
import bpy,json,sys,math
from pathlib import Path
from mathutils import Vector
root=Path(__file__).resolve().parents[1]
out=Path(sys.argv[sys.argv.index('--')+1]).resolve();out.mkdir(parents=True,exist_ok=True)
data=json.loads((root/'src/main/resources/assets/blacklight/models/form_blade.json').read_text())
bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete(use_global=False)
scene=bpy.context.scene;scene.render.engine='BLENDER_WORKBENCH'
scene.render.resolution_x=1536;scene.render.resolution_y=1024;scene.render.resolution_percentage=100
scene.render.image_settings.file_format='PNG'
scene.display.shading.light='STUDIO';scene.display.shading.studio_light='paint.sl'
scene.display.shading.color_type='MATERIAL';scene.display.shading.show_shadows=True
scene.display.shading.show_cavity=True;scene.display.shading.cavity_type='BOTH'
scene.display.shading.background_type='WORLD';scene.world.color=(.62,.61,.59)
scene.view_settings.view_transform='Standard'
bpy.ops.object.camera_add(location=(0,-6,.09));cam=bpy.context.object
cam.rotation_euler=(Vector((0,0,.09))-cam.location).to_track_quat('-Z','Y').to_euler()
cam.data.type='ORTHO';cam.data.ortho_scale=2.95;scene.camera=cam
materials={}
def mat(c):
 c=tuple(c)
 if c not in materials:
  linear=lambda v:v/3294.6 if v<=10 else ((v/255+.055)/1.055)**2.4
  m=bpy.data.materials.new(str(c));m.diffuse_color=(*(linear(v) for v in c),1);materials[c]=m
 return materials[c]
for label,x,turn in [('FRONT',-.80,0),('BACK',0,math.pi),('THREE QUARTER',.80,-math.pi/5)]:
 col=bpy.data.collections.new(label);scene.collection.children.link(col)
 for joint in ['Arm_R','Hand_R']:
  faces=data['joints'][joint];verts=[];polys=[];normals=[];colors=[]
  for f in faces:
   start=len(verts)
   for vx,vy,vz in f['p']:verts.append((vx,vz,.85-vy-(.3 if joint=='Hand_R' else 0)))
   polys.append((start,start+1,start+2));colors.append(tuple(f['c']))
   normals.extend((nx,nz,-ny) for nx,ny,nz in f['n'])
  mesh=bpy.data.meshes.new(label+'_'+joint);mesh.from_pydata(verts,[],polys);mesh.update()
  for p in mesh.polygons:p.use_smooth=True
  mesh.normals_split_custom_set(normals)
  mats=list(dict.fromkeys(colors));lookup={c:i for i,c in enumerate(mats)}
  for c in mats:mesh.materials.append(mat(c))
  for p,c in zip(mesh.polygons,colors):p.material_index=lookup[c]
  obj=bpy.data.objects.new(label+'_'+joint,mesh);col.objects.link(obj)
  obj.location.x=x;obj.rotation_euler.z=math.radians(data.get('mountDegrees',0))+turn
 bpy.ops.object.text_add(location=(x,-.16,-.68),rotation=(math.pi/2,0,0))
 text=bpy.context.object;text.data.body=label;text.data.align_x='CENTER';text.data.size=.045;text.data.materials.append(mat((40,40,40)))
scene.render.filepath=str(out/'blade-r17-three-views.png');bpy.ops.render.render(write_still=True)
bpy.ops.wm.save_as_mainfile(filepath=str(out/'blade-r17-three-views.blend'))
