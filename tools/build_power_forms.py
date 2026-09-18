"""Original joint-local biological weapons. No extracted game meshes or textures."""
import json, math
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'src/main/resources/assets/blacklight/models'
def tube(rows, sides=10, color=(40,34,35)):
    faces=[]
    rings=[[(cx+rx*math.cos(a*math.tau/sides), y, cz+rz*math.sin(a*math.tau/sides)) for a in range(sides)] for y,rx,rz,cx,cz in rows]
    def face(a,b,c,shade=1):
        faces.append({'p':[[round(v,5) for v in p] for p in (a,b,c)],'c':[round(v*shade) for v in color]})
    for i in range(len(rows)-1):
        for j in range(sides):
            k=(j+1)%sides;shade=.83+.17*(j%3)
            face(rings[i][j],rings[i+1][j],rings[i+1][k],shade)
            face(rings[i][j],rings[i+1][k],rings[i][k],shade)
    for j in range(sides):
        k=(j+1)%sides
        face((rows[0][3],rows[0][0],rows[0][4]),rings[0][k],rings[0][j])
        face((rows[-1][3],rows[-1][0],rows[-1][4]),rings[-1][j],rings[-1][k])
    return faces

def build(form):
    hammer=form=='hammerfists'
    arm=tube([(-.04,.135,.14,0,0),(.06,.19 if hammer else .15,.16,0,0),(.21,.20 if hammer else .16,.18,0,.01),(.40,.15,.14,0,0)])
    if hammer:
        hand=tube([(-.025,.15,.15,0,0),(.15,.24,.22,0,0),(.31,.32,.29,0,.015),(.58,.35,.31,0,.03),(.75,.27,.25,0,.02)],12)
        # Overlapping plates around a closed, heavy fist, with red connective tissue seams.
        for j in range(7):
            a=j*math.tau/7;cx=math.cos(a)*.265;cz=math.sin(a)*.245
            hand+=tube([(.29,.052,.05,cx,cz),(.40,.087,.08,cx,cz),(.62,.075,.08,cx,cz),(.72,.034,.03,cx,cz)],6,(57,49,47))
    elif form=='blade':
        hand=tube([(-.03,.145,.14,0,0),(.19,.19,.16,.01,0),(.38,.15,.13,.02,0)])
        # Thin diamond cross-section, broad cutting belly, tapered point; no box-sword handle.
        hand+=tube([(.24,.16,.07,.03,0),(.40,.255,.065,.10,0),(.66,.255,.050,.17,0),(.93,.13,.032,.19,0),(1.13,.004,.003,.12,0)],4,(94,86,78))
        hand+=tube([(.28,.08,.074,.015,0),(.55,.115,.058,.03,0),(.82,.075,.043,.06,0),(1.04,.003,.004,.08,0)],4,(35,30,30))
    else:
        hand=tube([(-.025,.14,.13,0,0),(.14,.17,.145,0,0),(.32,.13,.11,0,0),(.45,.10,.09,.035,0)])
        hand+=tube([(.38,.085,.07,.015,0),(.53,.105,.07,.035,0),(.68,.09,.05,.09,0),(.78,.012,.014,.21,0)],5,(79,66,58))
        for j in range(4):
            a=j*math.tau/4
            hand+=tube([(.17,.022,.022,math.cos(a)*.14,math.sin(a)*.12),(.34,.032,.028,math.cos(a+.7)*.14,math.sin(a+.7)*.12),(.56,.018,.015,math.cos(a+1.5)*.12,math.sin(a+1.5)*.09)],5,(77,30,29))
    for j in range(4):
        a=j*math.tau/4
        arm+=tube([(.025,.018,.019,math.cos(a)*.14,math.sin(a)*.14),(.20,.027,.025,math.cos(a+.5)*.18,math.sin(a+.5)*.16),(.39,.014,.015,math.cos(a+.9)*.13,math.sin(a+.9)*.13)],5,(100,35,32))
    joints={'Arm_R':arm,'Hand_R':hand}
    if hammer:
        for joint,faces in list(joints.items()):
            joints[joint.replace('_R','_L')]=[{'p':[[-v[0],v[1],v[2]] for v in reversed(f['p'])],'c':f['c']} for f in faces]
    (OUT/f'form_{form}.json').write_text(json.dumps({'joints':joints},separators=(',',':'))+'\n')
for name in ['hammerfists','blade','whipfist']:build(name)
