"""Minecraft-style utility model review 02: hand-authored cuboids and limited 22.5-degree plates.
No original meshes, textures, animations or AI concept images are imported.
"""
import argparse,json
from pathlib import Path
import author_utility_block_models as base

base.PALETTE={'shell':(39,43,49),'plate':(64,70,80),'ridge':(91,98,109),'edge':(118,123,130),'seam':(22,23,28),'fiber':(127,40,45)}
MODELS=base.MODELS

def add(model,name,bone,center,size,material='plate',angle=0):
    lo=[c-s/2 for c,s in zip(center,size)];hi=[c+s/2 for c,s in zip(center,size)]
    base.cube(model,name,bone,lo,hi,material)
    if angle:MODELS[model][-1].update(rotation=[0,0,angle],pivot=list(center))

def build():
    def a(name,bone,c,s,m='plate',angle=0):add('armor',name,bone,c,s,m,angle)
    a('Helmet core','head',(0,28,0),(8.2,8.2,8.2),'shell')
    a('Closed face recess','head',(0,27.8,-4.18),(7,6.6,.4),'seam')
    a('Forehead keel','head',(0,30.8,-4.42),(2.4,2.2,.7),'ridge')
    a('Crown center','head',(0,32.25,.5),(2.4,.7,6.4),'plate')
    for s in [-1,1]:
        a('Mask cheek '+str(s),'head',(s*2.1,27.7,-4.45),(2.1,5,.7),'plate',s*22.5)
        a('Mask brow '+str(s),'head',(s*2.1,30,-4.6),(3.8,1.1,.8),'ridge',s*22.5)
        a('Small sensory slit '+str(s),'head',(s*1.6,29.15,-4.76),(1.6,.4,.22),'fiber',s*22.5)
        a('Lower face plane '+str(s),'head',(s*.95,25.5,-4.4),(1.7,1.9,.65),'ridge',s*22.5)
        a('Temple cap '+str(s),'head',(s*4.05,29.6,0),(.65,3.4,5.9),'plate')
    a('Body core','torso',(0,18,0),(8,12,4),'shell')
    a('Waist recess','torso',(0,13.3,0),(7.2,2.6,4.3),'seam')
    for s in [-1,1]:
        a('Pectoral blade '+str(s),'torso',(s*2.6,21,-2.6),(4.8,4.1,1.5),'plate',s*22.5)
        a('Pectoral ridge '+str(s),'torso',(s*2.7,22.1,-3.4),(4.8,.7,.45),'ridge',s*22.5)
        a('Collar '+str(s),'torso',(s*3.6,24,-.1),(1.5,3.3,4.8),'plate',s*22.5)
        a('Hip scale '+str(s),'torso',(s*3.1,12.8,-.3),(2.3,2.7,4.5),'plate',s*15)
        a('Back shoulder wing '+str(s),'torso',(s*2.8,21.7,2.1),(4.4,3.2,1.2),'plate',-s*22.5)
    a('Sternum dark channel','torso',(0,19.2,-2.8),(1.6,7,.5),'seam')
    for i in range(3):
        y=15+i*1.3
        for s in [-1,1]:a('Abdominal chevron %s %s'%(s,i),'torso',(s*(.9+i*.22),y,-2.6),(1.85+i*.44,1,.85),'plate',s*22.5)
    for y in [16.3,18.7,21.1]:a('Sternum fiber '+str(y),'torso',(0,y,-3.35),(.6,.95,.25),'fiber')
    for i in range(4):
        a('Spinal carapace '+str(i),'torso',(0,15.2+i*2,2.5),(2.3,1.45,1),'ridge')
        a('Spinal tendon '+str(i),'torso',(0,14.6+i*2,2.7),(.65,.55,.3),'fiber')
    for s in [-1,1]:
        tag='left' if s>0 else 'right';bone=tag+'_arm';x=s*6
        a('Arm core '+tag,bone,(x,18,0),(4,12,4),'shell')
        a('Shoulder lower layer '+tag,bone,(s*6.6,22.2,0),(5.6,2.5,5.1),'plate',s*22.5)
        a('Shoulder upper blade '+tag,bone,(s*6.8,24.2,-.1),(6.1,1.65,5.5),'ridge',s*22.5)
        a('Shoulder outer crest '+tag,bone,(s*8.75,25.5,.2),(1.35,2.8,3.1),'plate',s*22.5)
        a('Bicep plate '+tag,bone,(x,19.7,-2.2),(4.2,2.5,1),'plate',s*15)
        a('Elbow recess '+tag,bone,(x,17,0),(4.2,1.2,4.2),'seam')
        a('Forearm shell '+tag,bone,(x,14.6,-.2),(4.6,4,4.4),'plate')
        a('Forearm diagonal '+tag,bone,(x,15,-2.65),(2.2,4,.75),'ridge',s*22.5)
        a('Forearm fiber '+tag,bone,(x-s*.95,14.7,-2.76),(.5,2.4,.25),'fiber',s*22.5)
        a('Knuckle block '+tag,bone,(x,12,-.4),(4.5,1.8,4.5),'shell')
        a('Knuckle crest '+tag,bone,(x,12.2,-2.8),(3.8,.85,.6),'ridge')
        bone=tag+'_leg';x=s*2
        a('Leg core '+tag,bone,(x,6,0),(3.8,12,3.8),'shell')
        a('Thigh plate '+tag,bone,(x,9.6,-2.1),(3.7,3.6,1.1),'plate',s*7.5)
        a('Knee guard '+tag,bone,(x,6.5,-2.5),(4.4,1.8,1.6),'ridge')
        a('Shin plate '+tag,bone,(x,3.8,-2.2),(3.8,3.1,1.1),'plate')
        a('Shin center rib '+tag,bone,(x,3.8,-2.9),(1,3,.55),'ridge')
        a('Boot '+tag,bone,(x,1,-.5),(4.25,2,4.9),'shell')
        a('Toe cap '+tag,bone,(x,1.3,-2.9),(4.25,1.15,1.1),'plate')
    def sh(n,c,z,m='plate',angle=0):add('shield',n,'shield',c,z,m,angle)
    for i,(y,h,w) in enumerate([(1,2,2),(3,2,4),(5.5,3,6),(9,4,7.5),(13.5,5,8),(18,4,6.8),(21,2,4.5)]):
        sh('Stepped foundation '+str(i),(0,y,.7),(w*2,h,1.7),'shell')
    for s in [-1,1]:
        for i in range(3):
            sh('Overlapping wing %s %s'%(s,i),(s*(3.9-i*.65),17-i*4,-.7),(7-i*.8,3,1.1),'plate',s*22.5)
            sh('Raised wing lip %s %s'%(s,i),(s*(4.05-i*.65),18.1-i*4,-1.4),(7-i*.8,.6,.45),'ridge',s*22.5)
        sh('Upper talon base '+str(s),(s*6.1,20.5,.05),(3,4.5,2),'plate',s*22.5)
        sh('Upper talon tip '+str(s),(s*7.2,23.1,.05),(1.15,2.5,1.35),'ridge',s*22.5)
        for i in range(2):
            sh('Edge blade %s %s'%(s,i),(s*(8-i*.75),14-i*6,.45),(1.6,3.6,2),'ridge',s*22.5)
        sh('Back raised rib '+str(s),(s*4,12,1.8),(1.2,13,.65),'plate',-s*7.5)
    sh('Central core',(0,12,-1.1),(2.3,18,1.8),'ridge')
    sh('Central channel',(0,12,-2.05),(.85,14,.2),'seam')
    for i in range(4):sh('Core fiber '+str(i),(0,7+i*3,-2.2),(.6,1.3,.2),'fiber')
    sh('Tip',(0,2,-.35),(1.5,3.2,1.6),'ridge')
    sh('Back mount',(0,12,2),(5,8,2),'seam')
    sh('Upper biomass cuff',(0,15,3.05),(7,1.5,1.9),'plate')
    sh('Lower biomass cuff',(0,9,3.05),(7,1.5,1.9),'plate')

def export(name,boxes,directory):
    base.export(name,boxes,directory)
    path=directory/(name+'.bbmodel');data=json.loads(path.read_text())
    for box,element in zip(boxes,data['elements']):
        if 'rotation' in box:element.update(rotation=box['rotation'],origin=box['pivot'])
    data['name']='Blacklight '+name+' block model v2'
    path.write_text(json.dumps(data,indent=2),encoding='utf-8')

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--output',type=Path,required=True);args=parser.parse_args();args.output.mkdir(parents=True,exist_ok=True)
    build()
    for name,boxes in MODELS.items():export(name,boxes,args.output)
    (args.output/'greybox-layout.json').write_text(json.dumps({'provenance':'Hand-authored Minecraft cuboids; no extracted meshes. Review only.','units':'Minecraft pixels','palette':base.PALETTE,'models':MODELS},indent=2),encoding='utf-8')
    print({name:len(boxes) for name,boxes in MODELS.items()})
