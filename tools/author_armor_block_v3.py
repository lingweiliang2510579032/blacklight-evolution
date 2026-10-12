"""Hand-authored heavy Minecraft armor. Review asset; no native meshes read."""
import json
from pathlib import Path
import author_utility_block_models_v2 as common

common.base.PALETTE = {
    'shell': (33, 38, 44), 'plate': (56, 65, 73),
    'ridge': (79, 89, 99), 'edge': (105, 114, 122),
    'seam': (19, 22, 27), 'fiber': (137, 37, 42),
}

def build():
    common.MODELS.clear()
    def a(n,b,c,s,m='plate',r=0): common.add('armor',n,b,c,s,m,r)
    # Familiar square head; a narrow brow and broad jaw inset behind the collar.
    a('Helmet','head',(0,28,0),(8.2,8.2,8.2),'shell')
    a('Recessed visor','head',(0,28.5,-4.2),(7,4.5,.45),'seam')
    a('Forehead carapace','head',(0,31,-4.2),(6.6,2.1,1),'plate')
    a('Crown keel','head',(0,32.2,.3),(2.2,.65,6.9),'ridge')
    a('Nasal armor','head',(0,27.9,-4.65),(1.7,3.5,.85),'plate')
    a('Jaw block','head',(0,25.5,-4.3),(4.6,1.5,1.25),'plate')
    for s in [-1,1]:
        a(f'Heavy brow {s}','head',(s*1.95,29.6,-4.6),(3.65,1.1,1),'ridge',s*7.5)
        a(f'Visor slit {s}','head',(s*1.8,28.7,-4.53),(2.1,.35,.25),'fiber',s*7.5)
        a(f'Jaw cheek {s}','head',(s*2.75,26.7,-4.25),(1.65,3.3,1.4),'plate',s*15)
        a(f'Temple block {s}','head',(s*4.05,28.2,.2),(.7,4.9,5.5),'plate')
        a(f'Rear skull plate {s}','head',(s*2.1,28.7,4.1),(3.7,5.6,.6),'plate')
    a('Torso core','torso',(0,18,0),(8,12,4),'shell')
    a('Abdomen flexible recess','torso',(0,15.8,-.1),(7.2,6,4.8),'seam')
    a('High rear collar','torso',(0,24.1,2.6),(8.7,3.3,2.1),'plate')
    a('Sternum core','torso',(0,20.6,-3.2),(2.4,6.4,2.1),'shell')
    a('Sternum raised keel','torso',(0,20.5,-4.25),(1.1,5,.65),'ridge')
    for s in [-1,1]:
        a(f'Collar flank {s}','torso',(s*4,23.9,0),(1.5,3.4,6),'plate')
        a(f'Pectoral mass {s}','torso',(s*2.6,21.25,-2.8),(4.8,4.8,2.8),'plate',s*7.5)
        a(f'Pectoral upper edge {s}','torso',(s*2.65,23.35,-4.1),(4.6,.65,.6),'ridge',s*7.5)
        a(f'Pectoral lower overlap {s}','torso',(s*2.15,18.9,-3),(3.7,1.6,1.8),'shell',s*15)
        for i in range(3):
            a(f'Lateral rib {s} {i}','torso',(s*3.65,15.1+i*1.35,.2),(1.3,.95,4.6),'ridge',s*7.5)
        a(f'Back muscle plate {s}','torso',(s*2.45,20.8,2.5),(4.2,5.5,2),'plate',-s*7.5)
        a(f'Hip skirt {s}','torso',(s*3.2,12.9,-.2),(2.2,3.2,5.1),'plate',s*7.5)
    for i in range(3):
        a(f'Abdominal overlapping plate {i}','torso',(0,14.4+i*1.35,-2.65),(4.3+i*.6,1.1,1.2),'plate')
        a(f'Abdominal central cleft {i}','torso',(0,14.4+i*1.35,-3.3),(.45,.85,.16),'seam')
    a('Pelvis guard','torso',(0,12.5,-2.3),(3.1,2.4,1.5),'plate')
    for i in range(5):
        a(f'Back spine scale {i}','torso',(0,14.6+i*1.9,3),(2.1,1.5,1.3),'ridge')
    for s in [-1,1]:
        tag='left' if s>0 else 'right';b=tag+'_arm';x=s*6
        a('Arm core '+tag,b,(x,18,0),(4,12,4),'shell')
        # Shoulder mass wraps down around the arm; no thin wing-like extensions.
        a('Shoulder main mass '+tag,b,(s*6.7,23.1,0),(6.1,4.8,6.8),'plate',-s*7.5)
        a('Shoulder crown '+tag,b,(s*6.6,25.25,0),(5.2,1.15,5.9),'ridge',-s*7.5)
        a('Shoulder front lamella '+tag,b,(s*6.9,22,-3.35),(5.3,2.15,1.15),'shell',-s*15)
        a('Shoulder outer plate '+tag,b,(s*9.05,22.1,.2),(1.35,3.8,5.2),'ridge',-s*7.5)
        a('Shoulder rear lamella '+tag,b,(s*6.8,22.1,3.25),(5.2,2.7,1.1),'plate',-s*7.5)
        a('Upper arm block '+tag,b,(s*6.25,19.15,0),(4.6,2.5,4.8),'plate')
        a('Elbow recess '+tag,b,(x,17.3,.2),(4.15,1.2,4.3),'seam')
        a('Elbow cap '+tag,b,(x,17.2,2.3),(3.6,1.9,1.7),'ridge')
        a('Heavy gauntlet '+tag,b,(s*6.6,14.7,-.25),(5.3,4.5,5.6),'plate')
        a('Gauntlet frontal overlap '+tag,b,(s*6.7,15.4,-3),(4.6,3.1,1.05),'ridge',-s*7.5)
        a('Gauntlet outer armor '+tag,b,(s*9,14.8,0),(1.1,3.8,4.4),'shell')
        a('Gauntlet cleft '+tag,b,(s*6.6,15.35,-3.6),(.55,2.2,.18),'seam')
        a('Gauntlet tendon '+tag,b,(s*6.6,15.7,-3.71),(.3,1.1,.13),'fiber')
        a('Closed fist '+tag,b,(s*6.5,11.65,-.7),(4.7,2.2,5.1),'shell')
        for k in range(3):
            a(f'Knuckle {tag} {k}',b,(s*6.5+(k-1)*1.35,11.7,-3.2),(1.1,1.25,.85),'ridge')
        b=tag+'_leg';x=s*2
        a('Leg core '+tag,b,(x,6,0),(3.8,12,3.8),'shell')
        a('Thigh mass '+tag,b,(s*2.1,9.9,0),(3.9,3.6,4.5),'plate')
        a('Thigh face '+tag,b,(s*2.1,9.8,-2.4),(3.5,3.1,1.25),'ridge',s*7.5)
        a('Knee recess '+tag,b,(x,7.3,0),(3.65,1.25,4),'seam')
        a('Knee guard '+tag,b,(s*2.12,6.65,-2.55),(3.85,2.2,2),'plate')
        a('Knee crown '+tag,b,(s*2.12,7,-3.4),(2.85,.85,.8),'ridge')
        a('Calf mass '+tag,b,(s*2.15,3.75,.3),(4,4.3,4.7),'plate')
        a('Shin face '+tag,b,(s*2.15,3.75,-2.4),(3.6,3.45,1.25),'ridge')
        a('Shin central groove '+tag,b,(s*2.15,3.75,-3.08),(.65,2.7,.17),'shell')
        a('Heel reinforcement '+tag,b,(s*2.15,1.4,2.2),(3.95,2.1,1.4),'shell')
        a('Heavy boot '+tag,b,(s*2.15,1,-.75),(4,2,6.2),'shell')
        a('Stepped toe '+tag,b,(s*2.15,1.15,-3.25),(4,1.4,1.8),'plate')
        a('Toe ridge '+tag,b,(s*2.15,1.65,-3.65),(3.3,.45,1),'ridge')

if __name__=='__main__':
    dest=Path(__file__).resolve().parents[1]/'art/armor-block-v3'
    dest.mkdir(parents=True,exist_ok=True);build()
    common.export('armor',common.MODELS['armor'],dest)
    path=dest/'armor.bbmodel';data=json.loads(path.read_text())
    data['name']='Blacklight heavy armor block model v3'
    path.write_text(json.dumps(data,indent=2),encoding='utf-8')
    (dest/'layout.json').write_text(json.dumps({'provenance':'Hand-authored Minecraft cuboids; appearance review only.', 'palette':common.base.PALETTE,'models':common.MODELS},indent=2),encoding='utf-8')
    print('Armor cuboids:',len(data['elements']))
