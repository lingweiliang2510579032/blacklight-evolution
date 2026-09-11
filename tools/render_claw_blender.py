"""Render the saved editable study, switching rig and IK controls together."""
import bpy,sys,json
from pathlib import Path
from mathutils import Vector
out=Path(sys.argv[sys.argv.index('--')+1]);out.mkdir(parents=True,exist_ok=True)
scene=bpy.context.scene;rig=bpy.data.objects['Blacklight_EpicFight']
controls=[o for o in bpy.data.objects if o.name.startswith('CTRL /')]
def select(name):
    rig.animation_data.action=bpy.data.actions[name]
    for ob in controls:ob.animation_data.action=bpy.data.actions[name+' / '+ob.name]
scene.render.engine='BLENDER_WORKBENCH'
scene.render.resolution_x=640;scene.render.resolution_y=640
scene.display.shading.light='STUDIO';scene.display.shading.studio_light='paint.sl'
scene.display.shading.color_type='MATERIAL';scene.display.shading.show_shadows=True
scene.display.shading.show_cavity=True;scene.display.shading.cavity_type='BOTH'
scene.display.shading.background_type='WORLD';scene.world.color=(.075,.085,.10)
scene.render.image_settings.file_format='PNG'
scene.camera.location=(3,5,2.35);scene.camera.rotation_euler=(Vector((0,0,.96))-scene.camera.location).to_track_quat('-Z','Y').to_euler()
scene.camera.data.ortho_scale=2.65
measure=[]
names=[a.name for a in bpy.data.actions if (a.name.startswith('combat/') or a.name.startswith('living/') or a.name.startswith('action/')) and ' / ' not in a.name]
args=sys.argv[sys.argv.index('--')+1:]
if len(args)>1:names=[name for name in names if name.startswith(args[1])]
for name in sorted(names):
    end=round(bpy.data.actions[name].frame_range[1]);index=0
    folder=out/name.split('/')[-1];folder.mkdir(exist_ok=True)
    select(name)
    for f in sorted(set([*range(0,end+1,4),end])):
        scene.frame_set(f)
        measure.append({'clip':name,'frame':f,'wristR':list(rig.pose.bones['Hand_R'].tail),
                        'wristL':list(rig.pose.bones['Hand_L'].tail),
                        'footR':list(rig.pose.bones['Leg_R'].tail),'footL':list(rig.pose.bones['Leg_L'].tail)})
        scene.render.filepath=str(folder/f'{index:04d}.png');bpy.ops.render.render(write_still=True);index+=1
(out/'measures.json').write_text(json.dumps(measure,indent=2))
print('RENDERED',index,flush=True)
