"""Bake approved cuboids to EF joints and sample private native growth timing.

Public geometry/atlas are authored. Source-derived curves are written ONLY to
the explicit private profile directory. Requires the local numpy retargeter.
"""
import argparse,copy,hashlib,json,sys
from pathlib import Path
import numpy as np
from PIL import Image,ImageDraw

parser=argparse.ArgumentParser()
parser.add_argument('--retarget',type=Path,required=True)
parser.add_argument('--skeleton',type=Path,required=True)
parser.add_argument('--plates',type=Path,required=True)
parser.add_argument('--profile',type=Path,required=True)
args=parser.parse_args();sys.path.insert(0,str(args.retarget))
from numpy_retarget import Target,Source,Retarget
repo=Path(__file__).resolve().parents[1]
layout=json.loads((repo/'art/armor-block-v4/layout.json').read_text())
target=Target();raw=args.plates.read_bytes();clip=json.loads(raw)
source=Source(clip,skeleton=json.loads(args.skeleton.read_text())[0])
rt=Retarget({'groups':[]},skeleton='alex')
duration=clip['num_frames']/clip['frame_rate'];times=np.arange(round(duration*30)+1)/30
worlds=[source.world(float(t)) for t in times]
mapping={'Torso':'Spine_1','Chest':'Spine_3','Head':'Head'}
for side in ['L','R']:
    mapping.update({f'Arm_{side}':f'Shoulder_{side}',f'Hand_{side}':f'Elbow_{side}',f'Thigh_{side}':f'Hip_{side}',f'Leg_{side}':f'Knee_{side}'})

def region(name):
    if name in ['Pelvis','Spine_1','Spine_2','Spine_3']:return 'torso'
    if name in ['Head','Neck']:return 'head'
    for side,tag in [('L','left'),('R','right')]:
        if name in [f'Clavicle_{side}',f'Shoulder_{side}',f'Elbow_{side}',f'Wrist_{side}']:return tag+'_arm'
        if name in [f'Hip_{side}',f'Knee_{side}',f'Ankle_{side}',f'Ball_{side}']:return tag+'_leg'
    return None

candidates={}
for j in source.joints:
    n=j['name'];parent=n
    while parent is not None and region(parent) is None:parent=source.parents[parent]
    if parent is None:continue
    if not n.startswith('armor_Joint') and n not in mapping.values():continue
    scale=np.array([np.cbrt(abs(np.linalg.det(w[n][:3,:3]))) for w in worlds])
    if scale[-1]<1e-7:continue
    scale/=scale[-1]
    if abs(1-scale[0])<1e-4:continue
    growth=np.clip((scale-scale[0])/(1-scale[0]),0,1)
    candidates[n]=(region(parent),growth)

def split_core(box):
    cut=18 if box['bone'].endswith('_arm') or box['bone']=='torso' else 6
    if box['name'] not in ['Torso core','Arm core left','Arm core right','Leg core left','Leg core right']:return [box]
    pieces=[]
    for upper in [False,True]:
        b=copy.deepcopy(box);b['name']+=('_upper' if upper else '_lower')
        b['from_' if upper else 'to'][1]=cut;pieces.append(b)
    return pieces

def joint(box,center):
    bone=box['bone']
    if bone=='head':return 'Head'
    if bone=='torso':return 'Chest' if center[1]>=18 else 'Torso'
    side='L' if bone.startswith('left') else 'R'
    return ('Arm_' if center[1]>=18 else 'Hand_')+side if bone.endswith('arm') else ('Thigh_' if center[1]>=6.8 else 'Leg_')+side

parts=[];tracks={};colors=list(layout['palette'])
face_indices=[[0,3,2,1],[4,5,6,7],[0,4,7,3],[1,2,6,5],[3,7,6,2],[0,1,5,4]]
face_indices=[list(reversed(face)) for face in face_indices] # Author-to-EF mapping reflects X.
for original in layout['models']['armor']:
    for b in split_core(original):
        lo,hi=np.array(b['from_']),np.array(b['to']);center=(lo+hi)/2;j=joint(b,center)
        vertices=np.array([[lo[0],lo[1],lo[2]],[hi[0],lo[1],lo[2]],[hi[0],hi[1],lo[2]],[lo[0],hi[1],lo[2]],
                           [lo[0],lo[1],hi[2]],[hi[0],lo[1],hi[2]],[hi[0],hi[1],hi[2]],[lo[0],hi[1],hi[2]]])
        rad=np.radians(b.get('rotation',[0,0,0])[2]);c,s=np.cos(rad),np.sin(rad);rot=np.array([[c,-s,0],[s,c,0],[0,0,1]])
        pivot=np.array(b.get('pivot',center));vertices=(vertices-pivot)@rot.T+pivot
        # Blender EF bind coordinates: X mirrored, Z up, front +Y.
        global_points=np.column_stack([-vertices[:,0],-vertices[:,2],vertices[:,1]])/16
        global_points[:,2]+=.014
        local=(np.column_stack([global_points,np.ones(8)])@np.linalg.inv(target.bind[j]).T)[:,:3]
        lc=local.mean(0)
        src_center=rt.source.bind[mapping[j]][:3,3]+rt.calibrated[j]@lc/rt.scale
        possible=[n for n,(r,g) in candidates.items() if r==b['bone']]
        assert possible,b['name']
        chosen=min(possible,key=lambda n:np.linalg.norm(source.bind[n][:3,3]-src_center))
        track=candidates[chosen][1]
        tracks[b['name']]={'sourceJoint':chosen,'growth':np.round(track,6).tolist()}
        parts.append({'name':b['name'],'joint':j,'region':b['bone'],'core':'core' in b['name'].lower(),
                      'shoulder':b['name'].startswith('Deltoid'),
                      'material':colors.index(b['material']),'points':np.round(local,7).tolist()})

asset=repo/'src/main/resources/assets/blacklight'
(asset/'models/armor_block.json').write_text(json.dumps({'revision':4,'faces':face_indices,'materials':colors,'parts':parts},separators=(',',':')),encoding='utf-8')
# Authored low-contrast pixel atlas. No original-game texture pixels are copied.
image=Image.new('RGB',(16*len(colors),16));draw=ImageDraw.Draw(image)
for index,material in enumerate(colors):
    rgb=layout['palette'][material];x=index*16
    draw.rectangle((x,0,x+15,15),fill=tuple(rgb))
    for rect,delta in [((1,1,5,2),5),((9,3,13,4),-4),((3,8,4,13),-5),((10,10,12,14),3),((6,5,8,6),2)]:
        draw.rectangle((x+rect[0],rect[1],x+rect[2],rect[3]),fill=tuple(int(np.clip(v+delta,0,255)) for v in rgb))
image.save(asset/'textures/entity/armor_block.png')
args.profile.mkdir(parents=True,exist_ok=True)
data={'version':1,'fps':30,'duration':duration,'parts':tracks,'source':{'animation':clip['name'],'sha256':hashlib.sha256(raw).hexdigest()},
      'adaptation':'Native accumulated scale timing normalized per plate; authored cubes grow about their centers. No source translation, shear or mesh imported.'}
(args.profile/'armor-growth.data').write_text(json.dumps(data,separators=(',',':')),encoding='utf-8')
assert all(t['growth'][0]==0 and abs(t['growth'][-1]-1)<1e-5 for t in tracks.values())
assert len(parts)==len(tracks)
print('Baked',len(parts),'joint-bound cuboids;',len(set(t['sourceJoint'] for t in tracks.values())),'native growth tracks; seconds',duration)
