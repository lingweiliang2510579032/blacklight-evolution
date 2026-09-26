"""Authored Minecraft shield, three damage silhouettes and forearm attachment."""
import json,math
from pathlib import Path
import author_utility_block_models_v2 as common

repo=Path(__file__).resolve().parents[1]
common.base.PALETTE={k:tuple(v) for k,v in json.loads((repo/'art/armor-block-v4/layout.json').read_text())['palette'].items()}

def build():
    common.MODELS.clear()
    def add(n,c,s,m='plate',r=0,stage=2,attachment=False):
        common.add('shield',n,'shield',c,s,m,r)
        common.MODELS['shield'][-1].update(maxStage=stage,attachment=attachment)
    for row,(y,width,height) in enumerate([(2,3,3),(5.2,7,3.5),(9,11.5,4),(13,14,4),(17,13,4),(20.3,10,3)]):
        for col in [-1,0,1]:
            stage=0 if (col==-1 and row>=4) or (col==1 and row==1) else 1 if (col==1 and row in [3,4,5]) or (col==-1 and row==2) else 2
            x=col*width/3
            add(f'Carapace cell {row} {col}',(x,y,.4),(width/3,height,1.8),'shell',stage=stage)
            add(f'Layered face {row} {col}',(x,y,-.75),(width/3-.3,height-.4,.95),'plate',col*7.5,stage)
    for side in [-1,1]:
        add(f'Raised flank {side}',(side*5.7,15,-.9),(1,7.8,.8),'ridge',side*15,0 if side==-1 else 1)
        add(f'Upper blade root {side}',(side*4.8,21.2,.05),(2,3,1.8),'plate',side*22.5,0 if side==-1 else 1)
        add(f'Upper blade tip {side}',(side*5.4,23,.05),(.9,1.8,1.25),'ridge',side*22.5,0 if side==-1 else 1)
        add(f'Side barb {side}',(side*7,12.8,.3),(1.3,3,1.6),'ridge',side*22.5,1 if side==-1 else 0)
    for i in range(4):
        add(f'Central spine {i}',(0,5+i*4,-1.5),(1.35,3.9,1.3),'ridge')
    add('Lower point',(0,.2,-.15),(1.5,2.1,1.65),'plate')
    add('Living seam left',(-1.7,13,-1.28),(.4,5.6,.3),'fiber',7.5)
    add('Living seam right',(1.55,8,-1.28),(.35,3.8,.3),'fiber',-7.5)
    add('Forearm root',(0,12,2.4),(4.1,5.6,2.4),'shell',stage=3,attachment=True)
    for side in [-1,1]:
        add(f'Forearm attachment {side}',(side*2.1,12,3.3),(1.2,4.5,1.5),'plate',stage=3,attachment=True)

def export():
    build();dest=repo/'art/shield-block-v3';dest.mkdir(parents=True,exist_ok=True)
    boxes=common.MODELS['shield'];common.export('shield',boxes,dest)
    (dest/'layout.json').write_text(json.dumps({'palette':common.base.PALETTE,'models':common.MODELS},indent=2),encoding='utf-8')
    faces=[[0,3,2,1],[4,5,6,7],[0,4,7,3],[1,2,6,5],[3,7,6,2],[0,1,5,4]]
    right=[-.5233,-.8384,.1525];up=[-.3264,.0319,-.9447];front=[-.7871,.5442,.2904]
    mats=list(common.base.PALETTE);parts=[]
    for b in boxes:
        a,z=b['from_'],b['to'];points=[[a[0],a[1],a[2]],[z[0],a[1],a[2]],[z[0],z[1],a[2]],[a[0],z[1],a[2]],
                                     [a[0],a[1],z[2]],[z[0],a[1],z[2]],[z[0],z[1],z[2]],[a[0],z[1],z[2]]]
        angle=math.radians(b.get('rotation',[0,0,0])[2]);c,s=math.cos(angle),math.sin(angle);pivot=b.get('pivot',[0,0,0]);local=[]
        for x,y,z in points:
            dx,dy=x-pivot[0],y-pivot[1];x,y=c*dx-s*dy+pivot[0],s*dx+c*dy+pivot[1]
            # Calibrated wrist-local axes; the source grapple delta is applied at runtime.
            local.append([round(right[i]*x*.78/16+up[i]*(y-12)*.84/16+front[i]*(.14-z/16),7) for i in range(3)])
        parts.append(dict(name=b['name'],points=local,material=mats.index(b['material']),maxStage=b['maxStage'],attachment=b['attachment']))
    (repo/'src/main/resources/assets/blacklight/models/shield_block.json').write_text(json.dumps(dict(revision=4,faces=faces,materials=mats,parts=parts,right=right,up=up,front=front),separators=(',',':')),encoding='utf-8')
    for stage in [0,1,2]:
        visible=[b for b in boxes if b['maxStage']>=stage];common.export(f'shield_damage_{stage}',visible,dest)
    print('Shield cuboids',len(parts),'stages',[sum(b['maxStage']>=s for b in boxes) for s in range(4)])

if __name__=='__main__':export()
