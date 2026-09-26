"""Original joint-bound armour, shield and muscular arms, with editable loft sections."""
import json
from build_power_forms import tube,muscles,CORE,ARMOR,RIDGE,TENDON,OUT

def save(name,joints):
 (OUT/(name+'.json')).write_text(json.dumps({'designRevision':19,'joints':joints},separators=(',',':'))+'\n',encoding='utf8')
def mirror(faces):return [{'p':[[-v[0],v[1],v[2]] for v in reversed(f['p'])],'c':f['c']} for f in faces]
arm=tube([(-.02,.14,.14,0,0),(.07,.21,.19,0,0),(.20,.245,.205,0,0),(.34,.17,.16,0,0),(.40,.14,.13,0,0)],14,TENDON)
arm+=muscles([(.03,.16),(.12,.21),(.25,.20),(.38,.14)],.025,8)
hand=tube([(-.02,.14,.13,0,0),(.12,.20,.18,0,0),(.30,.18,.16,0,0),(.43,.145,.12,0,0),(.52,.18,.145,0,0),(.65,.17,.14,0,0),(.70,.12,.10,0,0)],14,CORE)
hand+=muscles([(.01,.135),(.12,.19),(.29,.16),(.44,.135)],.022,7)
for i in range(4):
 x=(i-1.5)*.075;hand+=tube([(.48,.04,.075,x,.07),(.59,.048,.10,x,.08),(.67,.038,.08,x,.07)],8,RIDGE,True)
save('form_muscle_mass',{'Arm_R':arm,'Arm_L':mirror(arm),'Hand_R':hand,'Hand_L':mirror(hand)})
shield=tube([(-.23,.04,.06,-.30,-.08),(-.10,.30,.12,-.30,-.08),(.15,.43,.15,-.30,-.08),(.5,.40,.15,-.30,-.08),(.8,.26,.11,-.30,-.08),(1.0,.035,.045,-.30,-.08)],12,ARMOR)
shield+=tube([(-.12,.03,.035,-.30,-.24),(.25,.05,.035,-.30,-.24),(.6,.04,.03,-.30,-.24),(.9,.01,.015,-.30,-.16)],8,TENDON)
save('utility_shield',{'Hand_L':shield})
chest=tube([(-.11,.25,.17,0,0),(.02,.33,.20,0,0),(.20,.32,.20,0,0),(.30,.22,.16,0,0)],12,ARMOR,True)
for y in [-.08,.03,.14]:chest+=tube([(y,.28,.045,0,-.19),(y+.07,.31,.06,0,-.20),(y+.11,.27,.035,0,-.19)],10,RIDGE)
torso=tube([(-.13,.235,.15,0,0),(.02,.26,.17,0,0),(.24,.25,.17,0,0)],12,CORE,True)
helmet=tube([(-.015,.255,.255,0,0),(.16,.275,.265,0,0),(.38,.265,.26,0,0),(.51,.23,.23,0,0)],12,ARMOR,True)
helmet+=tube([(.24,.19,.01,0,-.272),(.27,.20,.012,0,-.272)],8,TENDON)
upper=tube([(-.025,.15,.15,0,0),(.12,.17,.16,0,0),(.32,.155,.15,0,0),(.40,.14,.14,0,0)],12,CORE,True)
lower=tube([(-.03,.14,.14,0,0),(.16,.165,.16,0,0),(.34,.15,.15,0,0),(.44,.14,.145,0,-.03)],12,ARMOR,True)
save('utility_armor',{'Torso':torso,'Chest':chest,'Head':helmet,'Thigh_R':upper,'Thigh_L':mirror(upper),'Leg_R':lower,'Leg_L':mirror(lower)})
print('Muscle mass, shield and armour models exported')
