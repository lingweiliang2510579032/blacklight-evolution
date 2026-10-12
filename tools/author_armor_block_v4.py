"""Minecraft cuboid interpretation of locally inspected Prototype 1 armor.

Authored geometry only. Native source was viewed for reference, never imported.
"""
import json
from pathlib import Path
import author_utility_block_models_v2 as common

common.base.PALETTE={'shell':(31,32,32),'plate':(53,55,54),'ridge':(73,76,72),
                     'edge':(89,92,87),'seam':(19,20,20),'fiber':(65,31,29)}

def build():
    common.MODELS.clear()
    def a(n,b,c,s,m='plate',r=0):common.add('armor',n,b,c,s,m,r)
    # Closed organic mask: no eyes, goggles, mechanical grille or separate jaw.
    a('Head core','head',(0,28,0),(8,8,8),'shell')
    a('Cranial front','head',(0,29.35,-4.1),(6.5,4.9,.65),'plate')
    a('Crown central plate','head',(0,32,.2),(4.8,.6,6.6),'plate')
    a('Face central ridge','head',(0,28.4,-4.58),(1.4,5.8,.65),'ridge')
    a('Lower face keel','head',(0,25.2,-4.36),(1.7,2,.7),'plate')
    for s in [-1,1]:
        a(f'Cranial flank {s}','head',(s*3.8,29.1,.1),(.6,4.8,6.4),'plate')
        a(f'Upper face plane {s}','head',(s*1.95,29.8,-4.52),(2.9,2.4,.6),'ridge',s*15)
        a(f'Lower face lamella {s}','head',(s*1.8,27,-4.42),(2.25,3.2,.6),'plate',-s*22.5)
        a(f'Rear skull ridge {s}','head',(s*1.9,28.9,4),(2.5,5.9,.7),'plate',-s*7.5)
    a('Torso core','torso',(0,18,0),(8,12,4),'shell')
    a('Narrow abdomen','torso',(0,16,-.2),(6.4,6,4.5),'shell')
    a('Rear neck mantle','torso',(0,24,2.2),(5.6,3.5,1.5),'plate')
    for s in [-1,1]:
        # Upright neck flanges are a prominent source silhouette landmark.
        a(f'Neck flange {s}','torso',(s*4.25,24.65,.35),(1.35,4.3,3.5),'plate',-s*15)
        a(f'Neck flange tip {s}','torso',(s*4.65,26.25,.5),(.8,1.65,2.3),'ridge',-s*15)
        a(f'Pectoral upper mass {s}','torso',(s*2.5,22,-2.65),(4.6,3.1,2.2),'plate',s*7.5)
        a(f'Pectoral lower plane {s}','torso',(s*2.1,20.25,-3.1),(3.7,1.65,1.1),'ridge',s*15)
        a(f'Clavicle sweep {s}','torso',(s*2.6,23.25,-2.95),(4.7,.75,1.1),'ridge',s*7.5)
        for i in range(3):
            a(f'Oblique rib {s} {i}','torso',(s*(3.25-i*.25),19.2-i*1.35,-1.25),(1.1,.8,3.6),'ridge',s*22.5)
        a(f'Waist organic seam {s}','torso',(s*3.5,14.9,.3),(.5,2.8,3.5),'fiber')
        a(f'Back shoulder plate {s}','torso',(s*2.25,21.3,2.5),(3.85,4.9,1.65),'plate',-s*15)
        a(f'Back lower plate {s}','torso',(s*1.85,18.35,2.45),(3,2.2,1.4),'ridge',-s*22.5)
        a(f'Flared hip crest {s}','torso',(s*3.25,13.5,0),(2.1,2.9,4.9),'plate',-s*22.5)
        a(f'Hip crest upper edge {s}','torso',(s*3.7,14.6,-1.6),(2.1,.65,1.3),'ridge',-s*22.5)
    for i in range(4):
        y=14.75+i*1.35
        for s in [-1,1]:
            a(f'Abdomen shell {s} {i}','torso',(s*(.85+i*.12),y,-2.65),(1.85+i*.24,1.15,1.15),'plate',s*15)
        a(f'Spine segment {i}','torso',(0,y+.4,2.9),(1.8,1.2,1),'plate')
    a('Pelvis shell','torso',(0,12.4,-2.25),(3.3,2,1.4),'plate')
    for s in [-1,1]:
        tag='left' if s>0 else 'right';b=tag+'_arm';x=s*6
        a('Arm core '+tag,b,(x,18,0),(4,12,4),'shell')
        a('Deltoid upper shell '+tag,b,(s*6.1,23.4,0),(4.6,2.7,5.1),'plate',-s*15)
        a('Deltoid outer overlap '+tag,b,(s*7.5,21.9,0),(2.5,2.6,4.9),'plate',-s*15)
        a('Deltoid frontal plane '+tag,b,(s*6.2,22.65,-2.55),(3.3,2.7,.85),'ridge',-s*15)
        a('Bicep sinew '+tag,b,(x,19.7,-2.2),(3.4,2.6,1),'plate',-s*7.5)
        a('Tricep sinew '+tag,b,(x,19.7,2),(3.3,3,1),'plate',s*7.5)
        a('Elbow joint '+tag,b,(x,17.55,0),(3.65,1.2,3.85),'seam')
        # Source forearms broaden near the elbow and taper to normal sized hands.
        a('Forearm upper shell '+tag,b,(s*6.35,16.55,-.1),(4.65,2.2,4.7),'plate')
        a('Forearm lower shell '+tag,b,(s*6.15,14.75,-.2),(4.1,1.9,4.25),'plate')
        a('Wrist taper '+tag,b,(x,13.35,-.1),(3.6,1.1,3.7),'shell')
        a('Forearm raised tendon '+tag,b,(s*6.4,15.75,-2.55),(1.25,3.9,.65),'ridge',-s*15)
        a('Forearm outer blade root '+tag,b,(s*8.25,16.4,.65),(1.25,3.2,2.1),'ridge',-s*15)
        a('Forearm outer blade tip '+tag,b,(s*8.6,18,.65),(.65,1.85,1.4),'ridge',-s*15)
        a('Forearm rear blade root '+tag,b,(s*7.05,16.6,2.15),(1.15,3.3,1.5),'plate',-s*15)
        a('Forearm rear blade tip '+tag,b,(s*7.4,18.25,2.15),(.6,1.4,.8),'ridge',-s*15)
        a('Hand shell '+tag,b,(x,11.7,-.25),(3.7,2.2,3.8),'shell')
        a('Hand tendon '+tag,b,(x,12,-2.1),(2.45,1.8,.5),'plate')
        b=tag+'_leg';x=s*2
        a('Leg core '+tag,b,(x,6,0),(3.8,12,3.8),'shell')
        for i in range(3):
            a(f'Thigh overlap {tag} {i}',b,(s*(2.1+.1*(2-i)),10.5-i*1.25,-.15),(3.75,1.7,4.45),'plate',s*7.5)
            a(f'Thigh front plane {tag} {i}',b,(s*2.1,10.5-i*1.25,-2.4),(3.2,.65,.7),'ridge',s*15)
        a('Knee shell '+tag,b,(x,6.5,-2.2),(3.75,1.9,1.4),'plate')
        a('Knee ridge '+tag,b,(x,6.45,-3),(1.9,1.55,.6),'ridge')
        a('Calf carapace '+tag,b,(x,3.7,.35),(3.85,4.4,4.45),'plate')
        a('Shin long keel '+tag,b,(x,3.6,-2.4),(2.4,4.1,1.1),'ridge')
        a('Calf rear tendon '+tag,b,(x,3.8,2.55),(1.55,3.8,.55),'shell')
        a('Ankle shell '+tag,b,(x,1.4,-.1),(3.6,1.8,3.8),'shell')
        a('Foot shell '+tag,b,(x,.65,-.6),(3.85,1.3,5),'plate')
        a('Foot dorsal ridge '+tag,b,(x,1.15,-2),(2.25,.9,2.5),'ridge')

if __name__=='__main__':
    dest=Path(__file__).resolve().parents[1]/'art/armor-block-v4'
    dest.mkdir(parents=True,exist_ok=True);build();common.export('armor',common.MODELS['armor'],dest)
    p=dest/'armor.bbmodel';d=json.loads(p.read_text());d['name']='Blacklight source-referenced Minecraft armor v4';p.write_text(json.dumps(d,indent=2),encoding='utf-8')
    (dest/'layout.json').write_text(json.dumps({'provenance':'Authored cuboids based on visual study of local Prototype 1 armor. No source mesh or texture imported.','palette':common.base.PALETTE,'models':common.MODELS},indent=2),encoding='utf-8')
    print('Armor cuboids:',len(d['elements']))
