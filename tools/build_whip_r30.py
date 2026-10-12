"""Authored continuous whip arm and a shared resting/traveling terminal blade.

Visual study: Dejan Ostojic's Prototype concept portfolio (2006-2009).
https://d-conceptart.blogspot.com/2012/07/prototype.html
No source-game geometry or texture is embedded.
"""
from build_reference_weapons import *

def tip_faces():
    # A long hooked outer cutting edge and shorter backward-pointing inner barb.
    faces=cord([(0,-.06,0,.06,.055),(.018,.12,0,.10,.07),(.025,.30,0,.065,.045)],BLACK,12,.08)
    faces+=plate([(-.018,.02,0,.048,.042),(.015,.17,0,.13,.060),(.11,.40,0,.155,.052),(.16,.60,.018,.075,.026),(.105,.77,.055,.001,.001)])
    faces+=plate([(.08,.22,-.01,.08,.038),(-.07,.30,-.005,.12,.05),(-.24,.25,.016,.076,.024),(-.29,.11,.052,.001,.001)],True)
    for side in [-1,1]:
        faces+=cord([(-.02,.015,.05*side,.018,.012),(.03,.17,.065*side,.023,.012),(.09,.40,.05*side,.017,.008),(.135,.58,.025*side,.001,.001)],FLESH,6)
        faces+=plate([(.055,.12,.045*side,.025,.013),(.13,.32,.056*side,.039,.014),(.18,.48,.037*side,.013,.007),(.16,.59,.02*side,.001,.001)])
    return faces

def build():
    m=Model();upper(m,1.14)
    m.add('Hand_R','R / continuous living forearm',cord([(0,-.05,0,.12,.115),(-.012,.045,.005,.176,.144),(-.01,.14,.01,.17,.132),(0,.26,.035,.137,.11),(0,.33,.05,.112,.103)],BLACK,14,.08))
    for i in range(8):
        a=i*math.tau/8;x,z=math.cos(a),math.sin(a)
        m.add('Hand_R',f'R / longitudinal bundle {i}',cord([(x*.10,-.035,z*.10,.013,.009),(x*.163,.065,z*.142,.018,.012),(x*.158,.17,z*.129+.015,.015,.010),(x*.108,.33,z*.095+.05,.008,.006)],FLESH if i in [2,6] else SHELL,6))
    for i in range(4):
        y=.015+i*.075
        m.add('Hand_R',f'R / overlapping dorsal rib {i}',plate([(-.06,y+.085,-.08,.008,.009),(-.09,y+.035,-.14,.095,.028),(-.10,y-.025,-.15,.056,.018),(-.16,y-.075,-.105,.001,.001)]))
    tip=tip_faces()
    mounted=[]
    for f in tip:
        g=dict(f);g['p']=[[x,y+.30,z+.05] for x,y,z in f['p']];mounted.append(g)
    m.add('Tip_R','R / retractable terminal blade',mounted)
    m.save('form_whipfist',{'tether_R':[0,.30,.05]},revision=30)
    t=Model();t.add('Hand_R','R / terminal blade',tip);t.save('whip_tip',revision=30)

if __name__=='__main__':build()
