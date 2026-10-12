"""Original Blacklight infected zombie overlay, editable in Blender; no third-party mesh is exported.
blender -b --python tools/build_infected_zombie.py -- WORK OUTPUT
"""
import bpy,bmesh,json,math,sys,importlib.util,random
from pathlib import Path
from mathutils import Vector
WORK,OUT=map(Path,sys.argv[sys.argv.index('--')+1:][:2]);OUT.mkdir(parents=True,exist_ok=True)
ROOT=Path(__file__).resolve().parents[1];RES=ROOT/'src/main/resources/assets/blacklight'
bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete(use_global=False)
spec=importlib.util.spec_from_file_location('epic_vendor',WORK/'epic-blender-addon/__init__.py',submodule_search_locations=[str(WORK/'epic-blender-addon')]);vendor=importlib.util.module_from_spec(spec);sys.modules[spec.name]=vendor;vendor.__path__=[str(WORK/'epic-blender-addon')]
from epic_vendor import import_mc_json as importer
rig=importer.import_armature(bpy.context,json.loads((WORK/'epic-biped.json').read_text())['armature'],'MAT','Infected_Zombie_Rig')['armature_obj'];rig.show_in_front=True
scene=bpy.context.scene;objects=[];mats=[]
for name,rgb in [('charcoal hide',(38,39,38)),('chitin facets',(53,56,53)),('tendon',(92,29,28)),('living tissue',(133,42,37)),('keratin edge',(121,118,102)),('scar ridge',(70,64,59)),('recess',(19,17,18))]:
 m=bpy.data.materials.new(name);m.diffuse_color=tuple((x/255)**2.2 for x in rgb)+(1,);m['rgb']=rgb;mats.append(m)
def mesh(name,vs,fs,bone,mi):
 me=bpy.data.meshes.new(name);me.from_pydata(vs,[],fs);me.update();ob=bpy.data.objects.new(name,me);scene.collection.objects.link(ob)
 for m in mats:me.materials.append(m)
 for f in me.polygons:f.material_index=mi if isinstance(mi,int) else mi[f.index%len(mi)]
 bm=bmesh.new();bm.from_mesh(me);bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));bm.to_mesh(me);bm.free()
 ob.vertex_groups.new(name=bone).add(list(range(len(vs))),1,'REPLACE');ob.parent=rig;ob.modifiers.new('Follow infected skeleton','ARMATURE').object=rig;ob['joint']=bone;objects.append(ob);return ob
def tube(name,pts,rs,bone,mi=0,n=6):
 vs=[]
 for i,p in enumerate(pts):
  tangent=Vector(pts[min(i+1,len(pts)-1)])-Vector(pts[max(0,i-1)]);tangent.normalize();side=tangent.cross(Vector((0,1,0)))
  if side.length<.01:side=tangent.cross(Vector((1,0,0)))
  side.normalize();up=tangent.cross(side).normalized()
  rx,ry=rs[i] if isinstance(rs[i],tuple) else (rs[i],rs[i])
  for j in range(n):
   a=j*math.tau/n;vs.append(Vector(p)+side*(rx*math.cos(a))+up*(ry*math.sin(a)))
 fs=[tuple(reversed(range(n)))]
 for i in range(len(pts)-1):
  for j in range(n):fs.append((i*n+j,i*n+(j+1)%n,(i+1)*n+(j+1)%n,(i+1)*n+j))
 fs.append(tuple(range((len(pts)-1)*n,len(pts)*n)))
 return mesh(name,vs,fs,bone,mi)
# Shoulder mass flows into the upper arm. Tendons cross seams instead of floating bands.
tube('R / burst shoulder',[(.28,0,1.57),(.39,.01,1.52),(.40,.01,1.36),(.37,0,1.25)],[.14,(.26,.205),(.21,.17),.125],'Arm_R',[0,1,0,5],8)
tube('R / muscular forearm',[(.37,0,1.30),(.40,.03,1.16),(.42,.03,.99),(.40,.02,.88)],[.125,(.205,.18),(.24,.19),(.18,.14)],'Hand_R',[0,1,0,2,0,5],8)
for j in range(4):
 x=.24+j*.105
 tube('R / hooked talon %d'%j,[(x,.045,.94),(x+.014,.10,.77),(x+.023,.19,.57-(.035 if j in (1,2) else 0)),(x+.013,.25,.46)],[.065,.047,.025,.003],'Hand_R',[0,1,4,1],5)
tube('R / opposing thumb',[(.23,.02,1.03),(.14,.06,.93),(.13,.18,.80),(.17,.24,.73)],[.07,.064,.032,.003],'Hand_R',[0,1,4],5)
for j in range(3):
 x=.29+j*.084
 tube('R / exposed forearm tendon %d'%j,[(x,.13,1.28),(x+.024,.191,1.15),(x+.03,.20,.99),(x+.015,.154,.87)],[.013,.026,.022,.009],'Hand_R',2 if j!=1 else 3,5)
for j in range(3):
 x=.25+j*.11
 tube('R / shoulder crest %d'%j,[(x,-.07,1.49),(x+.045,-.10,1.68),(x+.11,-.18,1.80-j*.03)],[.077,.06,.002],'Arm_R',[0,1,5],5)
# Smaller hand retains recognizable fingers; the right is the attacking silhouette.
tube('L / infected cuff',[(-.375,0,1.18),(-.38,.015,1.02),(-.37,0,.9)],[.145,.165,.135],'Hand_L',[0,1,2],6)
for j in range(3):
 x=-.45+j*.075
 tube('L / short claw %d'%j,[(x,.03,.93),(x,.10,.78),(x,.17,.68)],[.034,.027,.002],'Hand_L',[0,1,4],5)
# Chest fissure: narrow tissue core surrounded by diagonal strips of dark biomass.
for j in range(5):
 z=1.10+j*.088
 tube('Chest / overlapping rib %d'%j,[(.24,.07,z+.085),(.17,.155,z+.025),(.02,.161,z),(-.09,.14,z-.01)],[.06,.062,.042,.008],'Chest',[0,1,0],5)
tube('Chest / open red seam',[(.03,.174,1.06),(.01,.188,1.21),(.05,.18,1.39),(.15,.12,1.51)],[.026,.046,.04,.011],'Chest',[2,3,2],6)
for j in range(4):
 x=-.22+j*.13
 tube('Chest / spreading tendril %d'%j,[(.16,.155,1.43),(x,.173,1.34),(x-.055,.161,1.21),(x-.06,.145,1.11)],[.025,.023,.017,.003],'Chest',2 if j%2 else 0,5)
# Half-face corruption: one original eye stays readable, jaw becomes a split hooked plate.
tube('Head / temple plate',[(.23,.10,1.64),(.258,.11,1.76),(.255,.09,1.93),(.14,.08,2.01)],[.065,(.10,.085),(.105,.08),.025],'Head',[0,1,5],6)
tube('Head / torn cheek',[(.22,.13,1.87),(.19,.266,1.79),(.10,.264,1.67),(.04,.22,1.64)],[.038,.055,.042,.016],'Head',[0,2,0,1],5)
tube('Head / jaw hook',[(.17,.25,1.70),(.12,.29,1.65),(.025,.285,1.67)],[.038,.023,.002],'Head',[0,5,4],5)
tube('Head / red eye slit',[(.07,.262,1.83),(.125,.285,1.825),(.18,.271,1.85)],[.009,.016,.004],'Head',3,4)
for j in range(3):
 tube('Back / scapula ridge %d'%j,[(.12-j*.10,-.105,1.33+j*.09),(.16-j*.10,-.25,1.48+j*.08),(.20-j*.10,-.33,1.54+j*.08)],[.07,.043,.002],'Chest',[0,1,5],5)
tube('Leg / infection root',[(.17,.07,.77),(.22,.135,.66),(.22,.136,.49),(.16,.115,.43)],[.014,.034,.018,.003],'Leg_R',2,5)
# Export rigid joint-local triangles with artist-authored material color.
joints={};parts=[]
for ob in objects:
 me=ob.data;me.calc_loop_triangles();bone=ob['joint'];inv=rig.data.bones[bone].matrix_local.inverted();faces=joints.setdefault(bone,[]);start=len(faces)
 for tri in me.loop_triangles:
  faces.append({'p':[[round(v,7) for v in inv@me.vertices[i].co] for i in tri.vertices],'c':list(mats[me.polygons[tri.polygon_index].material_index]['rgb'])})
 parts.append({'name':ob.name,'joint':bone,'start':start,'count':len(faces)-start})
(RES/'models/infected_zombie.json').write_text(json.dumps({'format':1,'front':'+Y','joints':joints,'parts':parts},separators=(',',':')),encoding='utf-8')
# Original block silhouette for inspection only; runtime uses the actual zombie underneath.
def box(name,loc,scale,color):
 bpy.ops.mesh.primitive_cube_add(size=1,location=loc);ob=bpy.context.object;ob.name=name;ob.scale=scale
 m=bpy.data.materials.new(name);m.diffuse_color=(*color,1);ob.data.materials.append(m)
box('Preview / green head',(0,0,1.75),(.49,.49,.49),(.22,.30,.16))
box('Preview / shirt',(0,0,1.125),(.49,.25,.73),(.045,.23,.24))
for s in [-1,1]:
 box('Preview / sleeve',(s*.375,0,1.40),(.24,.25,.31),(.045,.23,.24));box('Preview / arm',(s*.375,0,1.065),(.24,.25,.37),(.22,.30,.16))
 box('Preview / trousers',(s*.125,0,.39),(.24,.25,.74),(.12,.12,.21))
 box('Preview / eye',(s*.11,.253,1.83),(.095,.012,.043),(.018,.027,.016))
box('Preview / mouth',(0,.253,1.67),(.12,.012,.035),(.025,.04,.018))
box('Stage',(0,0,-.025),(200,200,.04),(.12,.15,.16))
scene.render.engine='BLENDER_WORKBENCH';scene.display.shading.light='STUDIO';scene.display.shading.studio_light='paint.sl';scene.display.shading.color_type='MATERIAL';scene.display.shading.show_shadows=True;scene.display.shading.show_cavity=True;scene.display.shading.cavity_type='BOTH';scene.display.shading.show_object_outline=False;scene.display.shading.background_type='WORLD';scene.world.color=(.075,.085,.10)
camdata=bpy.data.cameras.new('Inspection camera');cam=bpy.data.objects.new('Inspection camera',camdata);scene.collection.objects.link(cam);scene.camera=cam;camdata.type='ORTHO';camdata.ortho_scale=2.7
scene.render.resolution_x=1000;scene.render.resolution_y=1000;scene.render.resolution_percentage=100;scene.view_settings.view_transform='Standard';scene.render.image_settings.file_format='PNG';rig.hide_render=True
for name,loc in [('front',(0,5,1.5)),('three-quarter',(3,5,2.3)),('back',(-3,-5,2.3))]:
 cam.location=loc;cam.rotation_euler=(Vector((0,0,1.0))-cam.location).to_track_quat('-Z','Y').to_euler();scene.render.filepath=str(OUT/(name+'.png'));bpy.ops.render.render(write_still=True)
bpy.context.preferences.filepaths.save_version=0;bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'infected-zombie.blend'))
print('EXPORTED',len(objects),'parts',sum(len(x) for x in joints.values()),'triangles',flush=True)
