"""Hand-authored Minecraft utility greyboxes. No extracted geometry is read.

Model units are Minecraft pixels: 8x8x8 head, 8x12x4 torso,
4x12x4 arms and legs. Outputs are review-only Blockbench projects.
Runtime resources are deliberately unchanged until the user reviews the shape.
"""
import argparse,base64,io,json,uuid
from pathlib import Path
from PIL import Image

PALETTE={'shell':(114,121,132),'plate':(151,158,167),'ridge':(177,183,190),'seam':(52,58,69)}
MODELS={}
def cube(model,name,bone,lo,hi,material='shell',damage=0):
    assert all(b>a for a,b in zip(lo,hi)),name
    MODELS.setdefault(model,[]).append(dict(name=name,bone=bone,from_=lo,to=hi,material=material,damage=damage))
def box(name,bone,x,y,z,w,h,d,material='shell'):cube('armor',name,bone,[x,y,z],[x+w,y+h,z+d],material)

def build():
    # The familiar player silhouette remains readable beneath stepped carapace plates.
    box('Square helmet','head',-4.15,24,-4.15,8.3,8.3,8.3)
    box('Inset face','head',-3.5,24.7,-4.35,7,6.8,.4,'seam')
    box('Forehead plate','head',-2,29.3,-4.7,4,2.5,.8,'plate')
    for side in [-1,1]:
        # Closed overlapping mask rather than a medieval T-shaped eye slit.
        for i in range(3):
            center=side*(2.9-i*.8)
            box('Stepped face plate %s %s'%(side,i),'head',center-1,28.4-i*1.45,-4.65,2,2.2,.7,'plate')
    box('Mask central point','head',-.7,24.4,-4.8,1.4,2.2,.85,'ridge')
    box('Crown ridge','head',-1,31.8,-2.6,2,1.2,6.4,'plate')
    box('Body core','torso',-4,12,-2,8,12,4)
    box('Waist belt','torso',-4.35,12,-2.45,8.7,2.1,4.9,'seam')
    for side in [-1,1]:
        x=-4.4 if side<0 else .4
        box('Upper breastplate '+str(side),'torso',x,20,-3.25,4,3.7,1.5,'plate')
        box('Lower breastplate '+str(side),'torso',(-3.6 if side<0 else .4),18.5,-3.1,3.2,1.5,1.2,'plate')
    box('Chest keel','torso',-.7,17.7,-3.65,1.4,6.2,.8,'ridge')
    for i in range(3):box('Abdominal plate '+str(i),'torso',-3.4+i*.35,14.3+i*1.15,-2.9,6.8-i*.7,1,1,'plate')
    box('Back mantle','torso',-4.25,20.5,1.65,8.5,3.4,1.4,'plate')
    for i in range(4):box('Spinal plate '+str(i),'torso',-1.15,14.7+i*1.85,2.05,2.3,1.5,1.2,'ridge')
    for side in [-1,1]:
        tag='left' if side>0 else 'right';a=side*6
        box('Arm core '+tag,tag+'_arm',a-2,12,-2,4,12,4)
        box('Shoulder cap '+tag,tag+'_arm',a-2.6,22,-2.6,5.2,3,5.2,'plate')
        outer=side*8
        box('Shoulder stepped edge '+tag,tag+'_arm',outer-1,24,-1.8,2,1.7,3.6,'ridge')
        box('Upper arm face '+tag,tag+'_arm',a-2.2,18,-2.5,4.4,3.3,.9,'plate')
        box('Elbow recess '+tag,tag+'_arm',a-2.1,16.8,-2.2,4.2,1,4.4,'seam')
        box('Forearm plate '+tag,tag+'_arm',a-2.35,13.4,-2.75,4.7,3.2,1.3,'plate')
        box('Forearm ridge '+tag,tag+'_arm',a-.6,13,-3.05,1.2,3.5,.6,'ridge')
        box('Glove '+tag,tag+'_arm',a-2.1,11.6,-2.2,4.2,1.9,4.4)
        leg=side*2
        box('Leg core '+tag,tag+'_leg',leg-1.9,.3,-1.9,3.8,11.7,3.8)
        box('Thigh plate '+tag,tag+'_leg',leg-2.15,8,-2.6,4.3,3.2,1.2,'plate')
        box('Knee guard '+tag,tag+'_leg',leg-2.25,5.6,-2.95,4.5,2,1.5,'ridge')
        box('Shin plate '+tag,tag+'_leg',leg-2.1,2.2,-2.6,4.2,3.1,1.2,'plate')
        box('Boot '+tag,tag+'_leg',leg-2.15,0,-3,4.3,2.2,5.2)
        box('Boot toe plate '+tag,tag+'_leg',leg-2.15,.8,-3.3,4.3,1.2,.7,'plate')
    # A stepped, forearm-grown carapace: broad upper face, tapered point, restrained side teeth.
    def shield(name,lo,hi,material='shell',damage=0):cube('shield',name,'shield',lo,hi,material,damage)
    for i,(y,h,w) in enumerate([(0,2,2),(2,2,4),(4,3,6),(7,10,8),(17,3,7),(20,2,5)]):
        shield('Stepped core '+str(i),[-w,y,0],[w,y+h,2])
    for side in [-1,1]:
        lo,hi=(-8,-6) if side<0 else (6,8)
        shield('Upper horn '+str(side),[lo,19,.1],[hi,23,1.8],'plate',1 if side<0 else 0)
        lo,hi=(-9,-7) if side<0 else (7,9)
        shield('Side tooth '+str(side),[lo,13,.25],[hi,16,1.75],'ridge',1 if side>0 else 2)
        lo,hi=(-7.6,-1.2) if side<0 else (1.2,7.6)
        shield('Upper face '+str(side),[lo,12,-.65],[hi,17.2,.15],'plate',2 if side<0 else 0)
        lo,hi=(-6.4,-1.2) if side<0 else (1.2,6.4)
        shield('Lower face '+str(side),[lo,7.2,-.65],[hi,11.6,.15],'plate',1 if side>0 else 0)
        for j in range(3):
            x=side*(2+j*1.5)
            shield('Stepped rib %s %s'%(side,j),[x-.85,16+j,-1],[x+.85,17.2+j,-.4],'ridge')
    shield('Central keel',[-1,3,-1.25],[1,21,.3],'ridge')
    shield('Lower point plate',[-2.6,3.2,-.6],[2.6,7,.15],'plate')
    shield('Back mounting pad',[-2.5,8,1.7],[2.5,15,3],'seam')
    shield('Back cuff upper',[-3.5,13,2.5],[3.5,15,4],'plate')
    shield('Back cuff lower',[-3.5,8,2.5],[3.5,10,4],'plate')


def export(name,boxes,directory):
    elements=[];groups={};textures=[]
    for i,(kind,color) in enumerate(PALETTE.items()):
        image=Image.new('RGBA',(16,16),color+(255,));buffer=io.BytesIO();image.save(buffer,format='PNG')
        textures.append(dict(name=kind,id=str(i),uuid=str(uuid.uuid5(uuid.NAMESPACE_URL,name+'/'+kind)),width=16,height=16,uv_width=16,uv_height=16,
                             source='data:image/png;base64,'+base64.b64encode(buffer.getvalue()).decode(),mode='bitmap',visible=True))
    for b in boxes:
        ident=str(uuid.uuid5(uuid.NAMESPACE_URL,name+'/'+b['name']));tex=list(PALETTE).index(b['material'])
        elements.append({'name':b['name'],'uuid':ident,'type':'cube','from':b['from_'],'to':b['to'],'origin':[0,0,0],
                         'faces':{f:{'uv':[0,0,16,16],'texture':tex} for f in ['north','south','east','west','up','down']}})
        groups.setdefault(b['bone'],[]).append(ident)
    data={'meta':{'format_version':'4.10','model_format':'free','box_uv':False},'name':'Blacklight '+name+' greybox',
          'resolution':{'width':16,'height':16},'elements':elements,'textures':textures,
          'outliner':[dict(name=bone,origin=[0,0,0],uuid=str(uuid.uuid5(uuid.NAMESPACE_URL,name+'/'+bone)),children=children) for bone,children in groups.items()]}
    (directory/(name+'.bbmodel')).write_text(json.dumps(data,indent=2),encoding='utf-8')

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--output',type=Path,required=True);args=parser.parse_args()
    args.output.mkdir(parents=True,exist_ok=True);build()
    for name,boxes in MODELS.items():export(name,boxes,args.output)
    (args.output/'greybox-layout.json').write_text(json.dumps({'provenance':'Hand-authored cuboids; no extracted meshes. Review only.','units':'Minecraft pixels','palette':PALETTE,'models':MODELS},indent=2),encoding='utf-8')
    print({name:len(boxes) for name,boxes in MODELS.items()})
