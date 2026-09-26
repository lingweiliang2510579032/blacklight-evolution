"""Original R14 biomass weapons, authored as editable joint-local surfaces.
Run with Python to export runtime meshes; render_power_forms.py opens them in Blender.
"""
import json, math
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'src/main/resources/assets/blacklight/models'
CORE=(31,27,29); ARMOR=(47,43,44); RIDGE=(69,64,62)
EDGE=(128,126,117); TENDON=(101,35,30)

def tri(faces,a,b,c,color):
    u=[b[i]-a[i] for i in range(3)];v=[c[i]-a[i] for i in range(3)]
    n=[u[1]*v[2]-u[2]*v[1],u[2]*v[0]-u[0]*v[2],u[0]*v[1]-u[1]*v[0]]
    if sum(x*x for x in n)<1e-13:return
    faces.append({'p':[[round(v,5) for v in p] for p in (a,b,c)],'c':list(color)})

def tube(rows,sides=10,color=CORE,square=False):
    faces=[];rings=[]
    for y,rx,rz,cx,cz in rows:
        ring=[]
        for j in range(sides):
            a=j*math.tau/sides;x,z=math.cos(a),math.sin(a)
            if square:x=math.copysign(abs(x)**.5,x);z=math.copysign(abs(z)**.5,z)
            ring.append((cx+rx*x,y,cz+rz*z))
        rings.append(ring)
    for i in range(len(rows)-1):
        for j in range(sides):
            k=(j+1)%sides
            c=tuple(round(v*(.93+.07*math.cos(j*math.tau/sides))) for v in color)
            tri(faces,rings[i][j],rings[i+1][j],rings[i+1][k],c)
            tri(faces,rings[i][j],rings[i+1][k],rings[i][k],c)
    for j in range(sides):
        k=(j+1)%sides
        tri(faces,(rows[0][3],rows[0][0],rows[0][4]),rings[0][k],rings[0][j],color)
        tri(faces,(rows[-1][3],rows[-1][0],rows[-1][4]),rings[-1][j],rings[-1][k],color)
    return faces

def blade(rows):
    # Asymmetric lenticular section: black spine, broad keratin face, narrow pale edge.
    faces=[];rings=[]
    for y,back,edge,thick in rows:
        middle=back+(edge-back)*.4
        rings.append([(back,y,0),(middle,y,-thick),(edge-.018,y,-thick*.18),
                      (edge,y,0),(edge-.018,y,thick*.18),(middle,y,thick)])
    colors=[ARMOR,RIDGE,EDGE,EDGE,RIDGE,ARMOR]
    for a,b in zip(rings,rings[1:]):
        for j in range(6):
            k=(j+1)%6;tri(faces,a[j],b[j],b[k],colors[j]);tri(faces,a[j],b[k],a[k],colors[j])
    for ring in [rings[0],list(reversed(rings[-1]))]:
        for j in range(1,5):tri(faces,ring[0],ring[j],ring[j+1],ARMOR)
    return faces

def muscles(rows,radius,count=5):
    faces=[]
    for j in range(count):
        a=j*math.tau/count
        strand=[]
        for i,(y,width) in enumerate(rows):
            t=i/max(1,len(rows)-1);bend=a+.7*t
            strand.append((y,radius*(.5+.5*math.sin(math.pi*t)),radius*.65,
                           math.cos(bend)*width,math.sin(bend)*width*.86))
        faces+=tube(strand,6,TENDON if j%2==0 else RIDGE)
    return faces

def build(form):
    hammer=form=='hammerfists'
    arm=tube([(-.045,.135,.14,0,0),(.025,.19,.18,0,0),(.14,.215 if hammer else .195,.18,.018,0),(.29,.175,.16,0,0),(.38,.14,.135,0,0)],12)
    arm+=muscles([(.005,.16),(.10,.197),(.21,.19),(.34,.135)],.021)
    if hammer:
        # Blocky impact mass grows from a narrow wrist, with four huge knuckle plates.
        hand=tube([(-.04,.145,.145,0,0),(.10,.23,.205,0,0),(.27,.30,.27,0,0),(.43,.42,.33,0,.015),(.65,.425,.34,0,.015),(.81,.355,.28,0,0)],12,CORE,True)
        for i in range(4):
            x=(i-1.5)*.205
            hand+=tube([(.39,.068,.19,x,.095),(.50,.108,.275,x,.10),(.75,.112,.265,x,.10),(.84,.080,.195,x,.08)],8,RIDGE,True)
            hand+=tube([(.23,.012,.018,x*.7,.255),(.47,.02,.022,x,.358),(.72,.012,.012,x,.37)],6,TENDON)
        # Offset thumb and overlapping dorsal plates avoid a round mace/sausage outline.
        hand+=tube([(.27,.09,.12,.29,-.04),(.43,.145,.15,.36,-.12),(.60,.11,.14,.34,-.18),(.70,.055,.07,.24,-.22)],8,ARMOR,True)
        for i in range(3):
            y=.16+i*.105
            hand+=tube([(y,.15+i*.042,.055,0,-.18-i*.045),(y+.08,.23+i*.045,.065,0,-.19-i*.045),(y+.14,.20+i*.048,.025,0,-.20-i*.043)],8,ARMOR)
        hand+=muscles([(.01,.15),(.15,.24),(.31,.33),(.50,.40)],.025,6)
    elif form=='blade':
        hand=tube([(-.035,.15,.145,0,0),(.12,.205,.175,0,0),(.28,.235,.18,.03,0),(.43,.20,.12,.04,0)],12)
        hand+=blade([(.24,-.13,.20,.11),(.43,-.16,.40,.10),(.68,-.14,.52,.085),(.93,-.08,.50,.06),(1.17,.01,.37,.045),(1.39,.12,.21,.018),(1.46,.165,.18,.005)])
        # A raised black spine and reddish fibres merge the blade into the forearm.
        hand+=tube([(.20,.065,.09,-.09,0),(.49,.085,.115,-.09,0),(.80,.061,.085,-.04,0),(1.08,.045,.05,.055,0),(1.35,.008,.015,.16,0)],7,CORE)
        hand+=muscles([(.02,.15),(.14,.20),(.28,.23),(.41,.18)],.023)
        for z in [-1,1]:
            hand+=tube([(.31,.015,.012,.10,z*.115),(.55,.018,.016,.16,z*.105),(.83,.012,.012,.20,z*.079),(1.10,.005,.007,.23,z*.045)],6,TENDON)
    else:
        hand=tube([(-.04,.145,.14,0,0),(.13,.205,.175,0,0),(.29,.20,.155,.01,0),(.44,.145,.11,.02,0),(.55,.11,.08,.02,0)],12)
        hand+=muscles([(.01,.15),(.17,.20),(.34,.18),(.52,.115)],.035,6)
        # Compact hooked cutting tip with a wide shoulder and a return barb.
        hand+=blade([(.43,-.08,.15,.095),(.61,-.10,.27,.10),(.82,-.07,.35,.075),(.98,.04,.30,.043),(1.04,.19,.26,.013)])
        hand+=blade([(.66,.17,.32,.06),(.57,.25,.38,.045),(.45,.34,.39,.02),(.37,.38,.395,.004)])
        hand+=tube([(.15,.065,.05,-.10,-.13),(.34,.06,.05,-.08,-.17),(.55,.035,.03,-.035,-.12),(.77,.01,.01,.02,-.07)],7,ARMOR)
    joints={'Arm_R':arm,'Hand_R':hand}
    if hammer:
        for joint,faces in list(joints.items()):
            joints[joint.replace('_R','_L')]=[{'p':[[-v[0],v[1],v[2]] for v in reversed(f['p'])],'c':f['c']} for f in faces]
    (OUT/f'form_{form}.json').write_text(json.dumps({'designRevision':14,'joints':joints},separators=(',',':'))+'\n')
    print(form, sum(map(len,joints.values())), 'triangles')
if __name__=='__main__':
    # Keep the historical command usable without restoring the rejected R14 meshes.
    from build_reference_weapons import hammer, blade as reference_blade, whip
    for author in (hammer,reference_blade,whip):author()
