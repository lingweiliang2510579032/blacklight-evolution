"""R15 original meshes: sculpted silhouettes informed by the P1 reference study.

Control curves and separate named surfaces remain editable in the Blender study.
No source-game mesh, texture, or artwork is copied into the mod.
Coordinates are Epic Fight joint-local metres (Hand is the elbow, not wrist).
"""
import json, math
from pathlib import Path
from collections import defaultdict

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT/'src/main/resources/assets/blacklight/models'
BLACK=(23,25,28); SHELL=(43,47,51); RIDGE=(67,72,75)
BEVEL=(112,120,123); EDGE=(170,178,179); FLESH=(66,24,29)

def sub(a,b): return tuple(x-y for x,y in zip(a,b))
def cross(a,b): return (a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0])
def norm(v):
    d=math.sqrt(sum(x*x for x in v))
    return tuple(x/d for x in v) if d>1e-10 else (0,1,0)

def sample(rows,steps=4):
    """Interpolating curves, including thickness, without changing endpoints."""
    result=[]
    for i in range(len(rows)-1):
        a,b,c,d=rows[max(0,i-1)],rows[i],rows[i+1],rows[min(len(rows)-1,i+2)]
        for k in range(steps):
            t=k/steps
            result.append(tuple(.5*((2*b[j])+(-a[j]+c[j])*t+(2*a[j]-5*b[j]+4*c[j]-d[j])*t*t+(-a[j]+3*b[j]-3*c[j]+d[j])*t*t*t) for j in range(len(b))))
    return result+[tuple(rows[-1])]

def surface(rings,colors,smooth=True,caps=True):
    faces=[];sides=len(rings[0])
    def add(a,b,c,color):
        n=cross(sub(b,a),sub(c,a))
        if sum(v*v for v in n)<1e-13:return
        faces.append({'p':[list(p) for p in (a,b,c)],'c':list(color)})
    for a,b in zip(rings,rings[1:]):
        for j in range(sides):
            k=(j+1)%sides;c=colors[j%len(colors)]
            add(a[j],b[j],b[k],c);add(a[j],b[k],a[k],c)
    for ring in (list(reversed(rings[0])),rings[-1]) if caps else ():
        for j in range(1,sides-1):add(ring[0],ring[j],ring[j+1],colors[0])
    if smooth:
        normals=defaultdict(lambda:[0,0,0])
        for f in faces:
            n=cross(sub(f['p'][1],f['p'][0]),sub(f['p'][2],f['p'][0]))
            for p in f['p']:
                key=tuple(round(v,6) for v in p)+(tuple(f['c']) if smooth=='crease' else ())
                for i in range(3):normals[key][i]+=n[i]
        for f in faces:f['n']=[norm(normals[tuple(round(v,6) for v in p)+(tuple(f['c']) if smooth=='crease' else ())]) for p in f['p']]
    for f in faces:
        f['p']=[[round(v,6) for v in p] for p in f['p']]
        if 'n' in f:f['n']=[[round(v,6) for v in n] for n in f['n']]
    return faces

def cord(rows,color=BLACK,sides=10,lobes=0):
    """Anatomical bundle; local rows x,y,z,width,depth. Radius varies continuously."""
    rings=[];rows=sample(rows)
    for i,(x,y,z,rx,rz) in enumerate(rows):
        rx=max(.0005,rx);rz=max(.0005,rz);t=i/max(1,len(rows)-1)
        rings.append([(x+rx*math.cos(a)*(1+lobes*math.cos(3*a+2*t)),y,
                       z+rz*math.sin(a)*(1+lobes*math.cos(3*a+2*t)))
                      for a in [j*math.tau/sides for j in range(sides)]])
    return surface(rings,[color])

def plate(rows,edge_left=False):
    """Continuous knife surface: x,y,z,half-width,thickness; crisp edge bevels."""
    rings=[]
    for x,y,z,w,h in sample(rows,5):
        w=max(w,.0008);h=max(h,.0006)
        rings.append([(x-w,y,z),(x-w*.83,y,z-h*.48),(x-w*.22,y,z-h),
                      (x+w*.78,y,z-h*.28),(x+w,y,z),
                      (x+w*.78,y,z+h*.28),(x-w*.22,y,z+h),(x-w*.83,y,z+h*.48)])
    colors=[EDGE,SHELL,SHELL,RIDGE,RIDGE,SHELL,SHELL,BEVEL] if edge_left else [RIDGE,SHELL,SHELL,BEVEL,EDGE,SHELL,SHELL,RIDGE]
    return surface(rings,colors,'crease')

class Model:
    def __init__(self):self.joints={};self.parts={}
    def add(self,joint,name,faces):
        group=self.joints.setdefault(joint,[])
        self.parts[name]={'joint':joint,'start':len(group),'count':len(faces)};group.extend(faces)
    def mirror(self):
        for name,part in list(self.parts.items()):
            if not part['joint'].endswith('_R'):continue
            fs=self.joints[part['joint']][part['start']:part['start']+part['count']]
            mirrored=[]
            for f in fs:
                n={'p':[[-p[0],p[1],p[2]] for p in reversed(f['p'])],'c':f['c']}
                if 'n' in f:n['n']=[[-p[0],p[1],p[2]] for p in reversed(f['n'])]
                mirrored.append(n)
            self.add(part['joint'].replace('_R','_L'),name.replace('R /','L /'),mirrored)
    def save(self,name,anchors=None,revision=15,mount_degrees=None):
        data={'version':2,'designRevision':revision,'source':f'reference-weapons-r{revision}.blend','joints':self.joints,'parts':self.parts}
        if anchors:data['anchors']=anchors
        if mount_degrees is not None:data['mountDegrees']=mount_degrees
        (OUT/(name+'.json')).write_text(json.dumps(data,separators=(',',':'))+'\n')
        print(name,sum(map(len,self.joints.values())),'triangles')

def upper(m,bulk=1):
    m.add('Arm_R','R / upper core',cord([(0,-.04,0,.12,.13),(.015,.05,0,.155*bulk,.15),(.01,.18,0,.14,.125),(0,.34,0,.115,.105)],lobes=.09))
    # Lengthwise crests split around the elbow; open spaces remain between arches.
    for i in range(5):
        a=i*math.tau/5+.4;x,z=math.cos(a),math.sin(a);shift=(i%3)*.018
        m.add('Arm_R',f'R / shoulder crest {i}',cord([(x*.08,-.04,z*.08,.006,.006),(x*.15,.035+shift,z*.15,.027,.018),(x*.18,.11+shift,z*.16,.025,.016),(x*.09,.24+shift,z*.10,.003,.003)],SHELL))
    for i in range(7):
        a=i*math.tau/7
        rows=[]
        for j in range(6):
            t=j/5;angle=a+.8*math.sin(t*2+i*.4);radius=.128+.021*math.sin(t*math.pi)
            rows.append((math.cos(angle)*radius,-.015+t*.34,math.sin(angle)*radius,.003+.004*math.sin(t*math.pi),.004))
        m.add('Arm_R',f'R / longitudinal tissue {i}',cord(rows,FLESH if i==3 else RIDGE,5))

def forearm(m,scale=1):
    m.add('Hand_R','R / forearm core',cord([(0,-.045,0,.11,.105),(-.015,.08,.02,.14*scale,.115),(-.005,.20,.035,.10,.085),(0,.31,.06,.11,.075)],lobes=.12))
    for i,x in enumerate([-.135,-.04,.07,.15]):
        offset=[.02,-.015,.03,0][i]
        m.add('Hand_R',f'R / lifted shell {i}',plate([(x*.32,-.025,-.045,.006,.008),(x*.75,.055+offset,-.13,.029,.022),(x*1.25,.14+offset,-.16,.033,.022),(x*.6,.26,-.045,.022,.014),(x*.4,.34,.02,.003,.004)]))
    for i in range(8):
        a=i*math.tau/8;rows=[]
        for j in range(6):
            t=j/5;angle=a+.55*math.sin(t*3+i);radius=.12+.024*math.sin(t*math.pi)
            rows.append((math.cos(angle)*radius,-.01+t*.32,math.sin(angle)*radius+.025,.005+.005*math.sin(t*math.pi),.006))
        m.add('Hand_R',f'R / interwoven tendon {i}',cord(rows,FLESH if i in (2,6) else SHELL,6))
    for i,x in enumerate([-.1,.04,.13]):
        m.add('Hand_R',f'R / recessed fibre {i}',cord([(x*.4,.01,.10,.004,.003),(x,.13,.13,.009,.006),(x*.8,.29,.10,.005,.004)],FLESH,6))

def claws():
    m=Model();upper(m,1.06);forearm(m,1.06)
    # Preserve the tested anatomical palm/knuckle frame from the authored R13 rig.
    base=json.loads((ROOT/'tools/data/claw_contact_r13.json').read_text())
    for name in ['palm','thumb root','knuckle 1','knuckle 2','knuckle 3','palm ridge 1','palm ridge 2','palm ridge 3']:
        p=base['parts']['R / '+name];faces=base['joints'][p['joint']][p['start']:p['start']+p['count']]
        normals=defaultdict(lambda:[0,0,0])
        for f in faces:
            n=cross(sub(f['p'][1],f['p'][0]),sub(f['p'][2],f['p'][0]))
            for vertex in f['p']:
                for axis in range(3):normals[tuple(vertex)][axis]+=n[axis]
        for f in faces:
            f['c']=list(RIDGE if 'ridge' in name else SHELL if 'knuckle' in name else BLACK)
            f['n']=[norm(normals[tuple(v)]) for v in f['p']]
        m.add('Hand_R','R / '+name,faces)
    for i,(x,end,fan) in enumerate([(.15,.95,.19),(0,1.08,-.035),(-.15,.995,-.205)]):
        m.add('Hand_R',f'R / talon {i+1}',plate([(x,.39,.105,.043,.021),(x+fan*.25,.54,.08,.069,.033),(x+fan*.70,.75,.09,.047,.024),(x+fan,end-.07,.22,.021,.010),(x+fan*.90,end,.36,.001,.001)]))
        m.add('Hand_R',f'R / metacarpal {i}',cord([(x*.25,.15,-.09,.008,.005),(x*.6,.28,-.04,.023,.012),(x,.43,.08,.030,.013),(x+fan*.2,.53,.09,.006,.004)],SHELL,8))
    m.add('Hand_R','R / opposed thumb',plate([(.25,.34,.11,.05,.027),(.37,.44,.14,.057,.028),(.41,.58,.25,.025,.013),(.33,.66,.37,.001,.001)]))
    m.mirror();m.save('claw_blender',{'palm_L':[.00267,.32362,.07666]})

def blade_r16():
    """R16: compact arm-root and thin, tapered blades; no detached crescent bow."""
    m=Model()
    # The upper arm retains an anatomical taper instead of the R15 bulbous cuff.
    m.add('Arm_R','R / arm tissue',cord([(0,-.025,0,.105,.11),(-.008,.06,0,.125,.118),(-.012,.18,0,.113,.099),(0,.32,0,.085,.082)],BLACK,12,.035))
    for side in [-1,1]:
        for i,x in enumerate([-.07,.005,.075]):
            m.add('Arm_R',f'R / arm lamina {side} {i}',plate([(x*.8,-.015,side*.08,.013,.010),(x,.07,side*.108,.026,.011),(x*.8,.18,side*.096,.023,.010),(x*.5,.32,side*.067,.004,.004)]))
    m.add('Hand_R','R / fused forearm tissue',cord([(0,-.035,0,.086,.083),(-.012,.055,0,.105,.089),(-.01,.16,.006,.096,.073),(.005,.28,.008,.080,.052),(.025,.42,0,.047,.026)],BLACK,12,.06))

    def knife(rows,reverse=False):
        # Explicit silhouette edges, central ridge and very narrow ground bevels.
        # rows: y, left, right, centre depth, half thickness.
        rings=[]
        for y,l,r,z,h in sample(rows,3):
            w=r-l;ridge=l+w*.39;bevel=min(.018,w*.12)
            rings.append([(l,y,z),(l+bevel,y,z-h*.32),(ridge,y,z-h),
                          (r-bevel,y,z-h*.32),(r,y,z),
                          (r-bevel,y,z+h*.32),(ridge,y,z+h),(l+bevel,y,z+h*.32)])
        return surface(rings,[BEVEL,RIDGE,SHELL,EDGE,EDGE,SHELL,RIDGE,BEVEL],'crease')

    # Maximum width is at the root. Both edges taper towards the long point;
    # a small hooked tip replaces the R15 swollen leaf-shaped distal blade.
    m.add('Hand_R','R / forward cutting blade',knife([
        (.20,-.061,.081,.005,.039),(.32,-.126,.163,0,.043),
        (.44,-.134,.161,0,.034),(.66,-.104,.109,.004,.025),
        (.85,-.079,.043,.010,.016),(1.005,-.073,-.024,.016,.007),
        (1.105,-.094,-.092,.021,.001)]))
    # The reverse blade grows from the same wrist root and follows the OUTER
    # forearm. Its narrow slot closes at the elbow and wrist: no large bow loop.
    m.add('Hand_R','R / reverse cutting blade',knife([
        (-.37,-.223,-.221,-.018,.001),(-.22,-.240,-.199,-.012,.012),
        (-.035,-.233,-.157,-.005,.020),(.13,-.202,-.099,0,.027),
        (.275,-.148,-.007,0,.032),(.38,-.083,.055,0,.030),
        (.47,-.022,.053,0,.020)],True))
    # Broad overlapping roots fuse the hard shell into the arm. Small openings
    # remain between these laminae, never held apart by thin bridge rods.
    for side in [-1,1]:
        m.add('Hand_R',f'R / elbow root {side}',plate([(-.18,-.035,side*.004,.025,.012),(-.11,.02,side*.065,.043,.017),(-.052,.14,side*.079,.037,.019),(.015,.285,side*.046,.019,.014),(.035,.44,side*.03,.001,.001)]))
        m.add('Hand_R',f'R / diagonal root {side}',plate([(.102,-.065,side*.035,.001,.001),(.081,.035,side*.074,.038,.015),(.029,.145,side*.09,.041,.017),(-.065,.26,side*.066,.036,.015),(-.112,.37,side*.031,.003,.003)]))
        # Dark-red fibres remain recessed along the root, not painted across
        # the cutting face as raised parallel wires.
        for i in range(4):
            x=-.066+i*.037
            m.add('Hand_R',f'R / root fibre {side} {i}',cord([(x*.65,-.022,side*.076,.003,.003),(x,.085,side*.087,.006,.004),(x*.72,.21,side*.064,.005,.003),(.014+i*.009,.36,side*.039,.001,.001)],FLESH if i%2==0 else RIDGE,5))
        m.add('Hand_R',f'R / blade root ridge {side}',plate([(.065,.25,side*.033,.014,.008),(.018,.41,side*.037,.018,.011),(-.004,.61,side*.027,.011,.008),(-.038,.86,side*.017,.001,.001)]))
    # Epic Fight's neutral arm presents local X across the torso. The broad
    # blade must face OUTWARD beside the arm, not lie across the front of it.
    # Rotate the full tissue/knife assembly so the attachment remains continuous.
    mount_degrees=-60
    angle=math.radians(mount_degrees);c,s=math.cos(angle),math.sin(angle)
    for faces in m.joints.values():
        for face in faces:
            face['p']=[[round(c*x+s*z,6),y,round(-s*x+c*z,6)] for x,y,z in face['p']]
            if 'n' in face:face['n']=[[round(c*x+s*z,6),y,round(-s*x+c*z,6)] for x,y,z in face['n']]
    m.save('form_blade',revision=16,mount_degrees=mount_degrees)

def blade():
    from build_blade_r17 import build
    build()

def hammer():
    m=Model();upper(m,1.12)
    m.add('Hand_R','R / tapered fist core',cord([(0,-.045,0,.115,.11),(0,.16,.02,.17,.145),(.01,.34,.015,.295,.235),(0,.52,0,.375,.285),(0,.68,0,.34,.25),(0,.75,0,.245,.20)],BLACK,14,.10))
    # Three broad interlocking digits; uneven facets, no four-knuckle boxing glove.
    for i,x in enumerate([-.255,0,.255]):
        y=.74+(i==1)*.045
        m.add('Hand_R',f'R / impact digit {i}',cord([(x*.48,.21,-.035,.065,.09),(x*.85,.39,-.025,.136,.245),(x,.58,0,.158,.30),(x*.92,y,.005,.134,.255),(x*.65,y+.035,.005,.050,.125)],SHELL,9,.08))
        m.add('Hand_R',f'R / impact ridge {i}',plate([(x*.6,.28,-.17,.025,.021),(x,.46,-.27,.075,.042),(x*.93,.65,-.285,.071,.038),(x*.75,y,-.19,.018,.011)]))
    for i,(x,z) in enumerate([(-.22,-.20),(.14,-.22),(.29,.02),(-.28,.08)]):
        m.add('Hand_R',f'R / knotted protrusion {i}',cord([(x*.7,.28,z*.7,.026,.024),(x,.44,z,.067,.05),(x*1.18,.51,z*1.2,.065,.054),(x,.60,z,.006,.006)],RIDGE,7))
    for i,x in enumerate([-.14,0,.14]):
        m.add('Hand_R',f'R / gripping seam {i}',cord([(x*.5,.05,.105,.007,.006),(x,.27,.17,.013,.009),(x*1.6,.43,.25,.011,.008),(x*1.75,.62,.26,.004,.003)],FLESH,6))
    m.mirror();m.save('form_hammerfists')

def whip():
    m=Model();upper(m);forearm(m,.9)
    m.add('Hand_R','R / hooked main blade',plate([(0,.23,.025,.06,.075),(.085,.41,.02,.17,.087),(.15,.60,.015,.19,.065),(.11,.78,.04,.12,.036),(-.015,.90,.09,.001,.001)]))
    m.add('Hand_R','R / recurved barb',plate([(.19,.59,0,.11,.054),(.31,.56,0,.08,.036),(.35,.44,.045,.029,.019),(.30,.33,.10,.001,.001)]))
    # The segmented dorsal cable flows into the blade, with overlapping swept ribs.
    m.add('Hand_R','R / whip cable',cord([(-.04,-.02,-.06,.045,.044),(-.11,.16,-.13,.064,.055),(-.10,.36,-.115,.064,.05),(-.02,.55,-.045,.040,.035),(-.015,.90,.09,.003,.003)],BLACK,10))
    for i in range(6):
        y=.07+i*.073
        m.add('Hand_R',f'R / swept barb {i}',plate([(-.075,y+.05,-.09,.021,.019),(-.17,y,-.13,.045,.027),(-.235,y-.06,-.10,.001,.001)]))
    m.save('form_whipfist',{'tether_R':[-.015,.90,.09]})

if __name__=='__main__':
    for build in (claws,blade,hammer,whip):build()
