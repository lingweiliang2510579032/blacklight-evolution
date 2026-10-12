"""Original GeckoLib silhouettes and articulated rigs for the hive ecosystem."""
import json,math
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources/assets/blacklight'
UV={'hide':([630,680],[100,100]),'plate':([280,330],[75,75]),'red':([130,145],[30,30]),'edge':([485,138],[60,20]),'dark':([310,770],[45,45])}
bones=[]
def bone(name,parent,pivot):
 b={'name':name,'pivot':pivot,'cubes':[]}
 if parent:b['parent']=parent
 bones.append(b);return b
def cube(b,o,s,mat='hide',rot=None):
 uv,size=UV[mat];c={'origin':o,'size':s,'uv':{f:{'uv':uv,'uv_size':size} for f in ['north','south','east','west','up','down']}}
 if rot:c.update(rotation=rot,pivot=[o[i]+s[i]/2 for i in range(3)])
 b['cubes'].append(c)
def frames(vals):return {str(t):v for t,v in vals}
def export(name,hive=False):
 geo={'format_version':'1.12.0','minecraft:geometry':[{'description':{'identifier':'geometry.blacklight.'+name,'texture_width':1254,'texture_height':1254,'visible_bounds_width':8 if hive else 5,'visible_bounds_height':6,'visible_bounds_offset':[0,2,0]},'bones':bones}]}
 (ROOT/'geo'/f'blacklight_{name}.geo.json').write_text(json.dumps(geo,indent=2))
 anims={'idle':{'loop':True,'animation_length':2,'bones':{'torso':{'scale':frames([(0,[1,1,1]),(1,[1.025,1.03,1.025]),(2,[1,1,1])])},'head':{'rotation':frames([(0,[0,-4,0]),(1,[-4,4,0]),(2,[0,-4,0])])}}}}
 if hive:
  anims['idle']['bones']={'torso':{'scale':frames([(0,[1,1,1]),(1,[1.05,1.025,1.05]),(2,[1,1,1])])}}
  for i in range(6):anims['idle']['bones'][f'petal{i}']={'rotation':frames([(0,[0,0,0]),(1,[5,0,0]),(2,[0,0,0])])}
 else:
  run={'torso':{'rotation':frames([(0,[4,5,0]),(.3,[4,-5,0]),(.6,[4,5,0])])}}
  for side,sign in [('right',1),('left',-1)]:
   for part,amount in [('leg',30),('arm',-22)]:
    run[f'{side}_{part}']={'rotation':frames([(0,[amount*sign,0,0]),(.3,[-amount*sign,0,0]),(.6,[amount*sign,0,0])])}
  anims['run']={'loop':True,'animation_length':.6,'bones':run}
  attack={'torso':{'rotation':frames([(0,[0,0,0]),(.5,[-12,-15,0]),(.7,[24,15,0]),(1.2,[0,0,0])])},'head':{'rotation':frames([(0,[0,0,0]),(.5,[-18,0,0]),(.7,[12,0,0]),(1.2,[0,0,0])])}}
  for side,sign in [('right',1),('left',-1)]:attack[side+'_arm']={'rotation':frames([(0,[0,0,0]),(.5,[-90,0,sign*-20]),(.7,[-10,0,sign*12]),(1.2,[0,0,0])])}
  if name=='spitter':attack['head']['scale']=frames([(0,[1,1,1]),(.5,[1.15,1.12,1.1]),(.7,[.9,.9,.9]),(1.2,[1,1,1])])
  anims['attack']={'loop':False,'animation_length':1.2,'bones':attack}
  anims['consume']={'loop':True,'animation_length':.6,'bones':{'torso':{'rotation':frames([(0,[-8,0,-6]),(.3,[8,0,6]),(.6,[-8,0,-6])])},'head':{'rotation':[15,0,0]}}}
 (ROOT/'animations'/f'blacklight_{name}.animation.json').write_text(json.dumps({'format_version':'1.8.0','animations':anims},indent=2))
 print(name,len(bones),sum(len(b['cubes']) for b in bones),'cubes')
for kind in ['stalker','spitter','brute']:
 bones=[];root=bone('root',None,[0,0,0]);low=kind=='stalker';heavy=kind=='brute'
 h=12 if low else 26 if heavy else 22;w=8 if heavy else 5
 torso=bone('torso','root',[0,h,0]);cube(torso,[-w,h-5,-4],[w*2,13 if heavy else 10,12 if low else 8]);
 for i in range(4):
  cube(torso,[-w-.6,h-4+i*2.5,-4.8],[w*2+1.2,1.7,2],'plate',[0,0,(-1)**i*7]);cube(torso,[-.6,h-4+i*2.5,-5.1],[1.2,1.1,.6],'red')
 for side in [-1,1]:
  for i in range(3):cube(torso,[side*w-1,h+i*2,3],[2,6-i,2],'plate',[-25,0,side*-25])
 head=bone('head','torso',[0,h+5,-3]);cube(head,[-3,h+3,-10],[6,6,7],'plate');cube(head,[-2,h+3,-11],[4,2,2],'red')
 for side in [-1,1]:
  cube(head,[side*2-.9,h+6,-10.5],[1.8,.6,.8],'red');cube(head,[side*3-.6,h,-9],[1.2,5,2],'edge',[10,0,side*22])
 if kind=='spitter':
  for i in range(3):
   cube(torso,[-4.5,h-3+i*3,4],[9,5,6],'red',[15,0,0]);cube(torso,[-5,h-2+i*3,8],[10,1.5,2],'plate',[15,0,0])
  cube(head,[-2,h+3,-15],[4,3,5],'dark');cube(head,[-2.5,h+3,-15.5],[5,.7,1],'red')
 for side,name in [(-1,'right'),(1,'left')]:
  x=side*(w+2);arm=bone(name+'_arm','torso',[x,h+3,0]);armw=8 if heavy else 3
  cube(arm,[x-armw/2,(3 if low else h-12),-2],[armw,13 if low else 16,6],'hide',[0,0,side*9])
  cube(arm,[x-armw/2-1,(3 if low else h-11),-3],[armw+2,8,8],'plate');cube(arm,[x-.5,(4 if low else h-10),-3.6],[1,8,1],'red')
  for finger in range(3):cube(arm,[x-2+finger*1.8,(0 if low else h-17),-4],[1,5,2],'edge',[20,0,0])
  leg=bone(name+'_leg','root',[side*3,h-3,3]);cube(leg,[side*3-2,2,1],[4,h-4,5],'hide',[-12,0,0]);cube(leg,[side*3-2.5,0,-3],[5,3,9],'plate')
 if low:
  tail=bone('tail','torso',[0,h,7]);
  for i in range(5):cube(tail,[-1.7+i*.18,h-1-i*.4,7+i*3],[3.4-i*.36,3,4],'plate',[-8,0,0])
 export(kind)
bones=[];bone('root',None,[0,0,0]);torso=bone('torso','root',[0,8,0]);cube(torso,[-18,2,-18],[36,24,36],'dark');cube(torso,[-13,12,-13],[26,32,26],'red');cube(torso,[-9,35,-9],[18,15,18],'red')
for i in range(6):
 a=i*math.tau/6;petal=bone(f'petal{i}','torso',[math.cos(a)*12,10,math.sin(a)*12]);yaw=-math.degrees(a)
 x,z=math.cos(a)*17,math.sin(a)*17
 cube(petal,[x-5,6,z-5],[10,29,10],'plate',[0,yaw,15]);cube(petal,[x-3,30,z-3],[6,18,6],'edge',[0,yaw,23])
 root=bone(f'root{i}','root',[0,0,0])
 for j in range(5):
  d=18+j*4;cube(root,[math.cos(a)*d-3,1,math.sin(a)*d-3],[6-j*.7,6-j,8],'hide',[0,-math.degrees(a)+90,0]);cube(root,[math.cos(a)*d-1,5-j*.7,math.sin(a)*d-1],[2,1.3,5],'red',[0,-math.degrees(a)+90,0])
export('hive',True)
