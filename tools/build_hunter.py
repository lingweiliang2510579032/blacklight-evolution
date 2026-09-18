"""Original Blockbench/GeckoLib full-body hunter rig. No third-party skeleton or model."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/"src/main/resources/assets/blacklight"
bones=[]
def bone(name,parent,pivot,rotation=None):
 b={"name":name,"pivot":pivot,"cubes":[]}
 if parent:b["parent"]=parent
 if rotation:b["rotation"]=rotation
 bones.append(b);return b
UV={"hide":([630,680],[100,100]),"plate":([280,330],[75,75]),"red":([130,145],[30,30]),"edge":([485,138],[60,20]),"dark":([310,770],[45,45])}
def cube(b,o,s,mat="hide",rot=None,piv=None):
 uv,sz=UV[mat];c={"origin":o,"size":s,"uv":{f:{"uv":uv,"uv_size":sz} for f in ("north","south","east","west","up","down")}}
 if rot:c.update(rotation=rot,pivot=piv or [o[i]+s[i]/2 for i in range(3)])
 b["cubes"].append(c)
root=bone("root",None,[0,0,0]);hips=bone("hips","root",[0,15,0]);cube(hips,[-4,13,-3],[8,6,7]);cube(hips,[-3,15,-3.4],[6,3,1],"red")
t=bone("torso","hips",[0,19,0],[12,0,0]);cube(t,[-6,18,-4],[12,10,9]);cube(t,[-8,24,-4.5],[16,8,10],"plate");cube(t,[-5,30,-3],[10,4,8],"plate")
for side in [-1,1]:
 for i in range(4):
  cube(t,[side*4-2.5,20+i*2,-5.1],[5,1.4,1.4],"plate",[0,0,side*(12+i*3)])
  cube(t,[side*2.7-.65,20+i*2,-5.35],[1.3,1.1,.5],"red")
 for i in range(3):
  cube(t,[side*(7+i*.3)-1.2,28+i*1.1,3],[2.4,5-i*.6,2],"plate",[-25,0,side*-22])
for i in range(5):cube(t,[-.85,21+i*2.4,5.4],[1.7,1.7,2.2],"edge")
head=bone("head","torso",[0,31,-3]);cube(head,[-3.2,30,-9],[6.4,5.5,7],"plate",[12,0,0]);cube(head,[-2.5,30.7,-10],[5,2.1,2],"dark")
for side in [-1,1]:
 cube(head,[side*1.5-.85,33,-9.9],[1.7,.55,.6],"red",[0,0,side*-15])
 for i in range(3):cube(head,[side*(.5+i*.7)-.2,30.8,-10.4],[.4,1.3,.8],"edge",[-15,0,side*8])
jaw=bone("jaw","head",[0,30,-3]);cube(jaw,[-2.4,28.6,-9.4],[4.8,1.5,5.6],"plate");cube(jaw,[-1.5,29.6,-9.7],[3,.8,.8],"red")
for side,name in [(-1,"right"),(1,"left")]:
 heavy=name=="right";x=side*(9 if heavy else 8);w=7 if heavy else 5.4
 arm=bone(name+"_arm","torso",[x,29,0],[0,0,side*9]);cube(arm,[x-w/2,20,-3],[w,10,7],"hide");cube(arm,[x-w/2-.5,26,-3.5],[w+1,5,8],"plate",[0,0,side*12]);cube(arm,[x-w/2+.8,21,-3.4],[1.2,7,1],"red")
 hand=bone(name+"_forearm",name+"_arm",[x,20,0],[-12,0,side*-5]);cube(hand,[x-w/2-.5,10,-3.8],[w+1,11,8],"hide");cube(hand,[x-w/2-1,13,-4.3],[w+2,6,8.6],"plate");cube(hand,[x-.7,10,-4.6],[1.4,9,.7],"red")
 for i in range(4 if heavy else 3):
  cx=x+(i-(1.5 if heavy else 1))*1.65
  cube(hand,[cx-.65,5.3,-3.8],[1.3,6,1.8],"plate",[12,0,(i-1.5)*4])
  cube(hand,[cx-.45,3.1,-5],[.9,3.5,1.1],"edge",[28,0,0])
 cube(hand,[x-side*(w/2+1)-.8,7,-1],[1.6,5,2],"plate",[0,0,side*-35])
 leg=bone(name+"_thigh","hips",[side*3,15,0]);cube(leg,[side*3-2.7,7,-2.2],[5.4,9,6],"hide",[-8,0,0]);cube(leg,[side*3-2.1,8,-3],[4.2,4,2],"plate")
 shin=bone(name+"_shin",name+"_thigh",[side*3,8,0]);cube(shin,[side*3-1.9,1,-.8],[3.8,7.5,4],"plate",[8,0,0]);cube(shin,[side*3-2.5,0,-5],[5,2,8],"hide")
 for i in range(3):cube(shin,[side*3-1.8+i*1.4,0,-6.6],[1,1.2,2.5],"edge")
geo={"format_version":"1.12.0","minecraft:geometry":[{"description":{"identifier":"geometry.blacklight.hunter","texture_width":1254,"texture_height":1254,"visible_bounds_width":5,"visible_bounds_height":4,"visible_bounds_offset":[0,1.5,0]},"bones":bones}]}
(ROOT/"geo/blacklight_hunter.geo.json").write_text(json.dumps(geo,indent=2))
def frames(values):return {str(t):v for t,v in values}
anims={}
anims["idle"]={"loop":True,"animation_length":2,"bones":{"torso":{"rotation":frames([(0,[0,0,-2]),(1,[-2,0,2]),(2,[0,0,-2])])},"jaw":{"rotation":frames([(0,[0,0,0]),(1,[8,0,0]),(2,[0,0,0])])}}}
run={"hips":{"position":frames([(0,[0,0,0]),(.2,[0,1,0]),(.4,[0,0,0]),(.6,[0,1,0]),(.8,[0,0,0])])},"torso":{"rotation":frames([(0,[9,8,0]),(.4,[9,-8,0]),(.8,[9,8,0])])}}
for side,sign in [("right",1),("left",-1)]:
 run[side+"_thigh"]={"rotation":frames([(0,[sign*30,0,0]),(.4,[-sign*30,0,0]),(.8,[sign*30,0,0])])}
 run[side+"_shin"]={"rotation":frames([(0,[max(0,-sign*28),0,0]),(.4,[max(0,sign*28),0,0]),(.8,[max(0,-sign*28),0,0])])}
 run[side+"_arm"]={"rotation":frames([(0,[-sign*20,0,0]),(.4,[sign*20,0,0]),(.8,[-sign*20,0,0])])}
anims["run"]={"loop":True,"animation_length":.8,"bones":run}
anims["swipe"]={"loop":False,"animation_length":1.2,"bones":{"torso":{"rotation":frames([(0,[0,0,0]),(.35,[-8,-30,0]),(.5,[14,30,8]),(.8,[8,20,4]),(1.2,[0,0,0])])},"right_arm":{"rotation":frames([(0,[0,0,0]),(.35,[-100,-30,-45]),(.5,[-30,45,30]),(.8,[15,10,0]),(1.2,[0,0,0])])},"right_forearm":{"rotation":frames([(0,[0,0,0]),(.35,[-45,0,0]),(.5,[0,0,0]),(1.2,[0,0,0])])}}}
anims["consume"]={"loop":True,"animation_length":.4,"bones":{"torso":{"rotation":frames([(0,[-12,0,-4]),(.2,[-16,0,4]),(.4,[-12,0,-4])])},"left_arm":{"rotation":[-25,0,-30]},"right_arm":{"rotation":[-25,0,30]},"jaw":{"rotation":[20,0,0]}}}
(ROOT/"animations/blacklight_hunter.animation.json").write_text(json.dumps({"format_version":"1.8.0","animations":anims},indent=2))
print(len(bones),"bones",sum(len(b["cubes"]) for b in bones),"cubes")
