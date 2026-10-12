"""Original articulated Minecraft infection growths, in host-part coordinates.

Each centre/size is relative to the selected vanilla part's rest bounds, so
these cuboids follow its animation rather than floating at entity positions.
No extracted mesh, texture or photo reference is read.
"""
from pathlib import Path
from copy import deepcopy
import json,zlib,struct,binascii
R=Path(__file__).resolve().parents[1];A=R/'src/main/resources/assets/blacklight'
profiles={}
def box(c,s,material='chitin'):return {'center':c,'size':s,'material':material}
def profile(name,body,head,limb,tail=None):
    profiles[name]={'body':body,'head':head,'limb':limb,'tail':tail or []}
def ridge(axis='z',count=4):
    return [box([.5,(i+.5)/count,1.10],[.26,.68/count,.28]) for i in range(count)] if axis=='z' else [box([.5,-.12,(i+.5)/count],[.30,.30,.7/count]) for i in range(count)]
def ribs():
    return [box([x,y,1.02],[.26,.09,.16],'sinew') for x in (.23,.77) for y in (.27,.5,.73)]
def face():
    return [box([.5,.15,-.08],[.92,.2,.20]),box([-.02,.62,.12],[.18,.52,.70]),box([1.02,.62,.12],[.18,.52,.70]),
            box([.5,.92,-.15],[.86,.16,.42],'sinew'),box([.25,1.06,-.26],[.12,.28,.16],'bone'),box([.75,1.06,-.26],[.12,.28,.16],'bone')]
def claws():
    return [box([.5,.7,.5],[1.18,.43,1.17]),box([.5,.53,-.08],[.42,.55,.19],'sinew')]+[
        box([x,1.02,-.12],[.21,.20,.46],'bone') for x in (.18,.5,.82)]
def sheath():return [box([.5,.30,.96],[1.08,.36,.20]),box([.5,.7,-.04],[.35,.6,.12],'sinew')]

profile('bovine',ridge()+ribs()+[box([.5,.22,1.23],[.84,.40,.4]),box([.5,.22,1.47],[.55,.26,.17])],
        face()+[box([x,.03,.4],[.18,.38,.24],'bone') for x in (-.10,1.10)],claws())
profile('porcine',ridge(count=3)+ribs()+[box([.20,.40,1.18],[.32,.42,.4],'sinew'),box([.16,.40,1.43],[.21,.26,.15])],
        face()+[box([.5,.67,-.26],[.66,.32,.34],'sinew')],claws())
profile('woolly',[box([.5,.18,1.18],[1.22,.34,.65]),box([.5,.76,1.13],[1.18,.28,.55])]+ridge()+ribs(),
        face()+[box([.12,.08,.25],[.24,.25,.45]),box([.88,.08,.25],[.24,.25,.45])],sheath()+claws()[-3:])
profile('canine',ridge(count=5)+[box([x,.22,1.02],[.22,.55,.27],'sinew') for x in (.15,.85)],
        face()+[box([.5,.79,-.40],[.53,.24,.55]),box([.5,.74,-.7],[.45,.12,.16],'sinew')],claws(),
        [box([.5,.45,.5],[1.16,.55,1.16]),box([.5,1.02,.5],[.55,.22,.55],'bone')])
profiles['feline']=deepcopy(profiles['canine']);profiles['feline']['body']=ridge(count=3)+ribs()[::2]
profile('equine',ridge(count=6)+ribs(),face()+[box([.5,.0,.6],[.24,.38,.3],'bone')],sheath()+[box([.5,1.02,.3],[1.15,.16,1.28])])
profile('small_quadruped',ridge(count=3),face()[:4],sheath()+claws()[-3:])
profile('humanoid',[box([.5,.35,.1],[1.15,.68,1.08]),box([.5,.42,-.50],[.38,.52,.14],'sinew')]+ridge()+ribs(),
        face()+[box([.18,.12,.32],[.25,.33,.48],'sinew')],claws()+[box([.5,.15,.5],[1.25,.22,1.22])])
profile('construct',[box([.5,.25,.5],[1.18,.44,1.15]),box([.5,.68,-.12],[.52,.37,.22],'sinew')]+ridge(count=3),
        face()[:4]+[box([.5,.7,-.17],[.22,.30,.22],'sinew')],claws())
profile('creeper',ridge(count=5)+[box([.5,y,-.09],[.55,.16,.18],'sinew') for y in (.22,.47,.72)],
        [box([.5,.0,.5],[1.15,.2,1.06]),box([.08,.6,.3],[.24,.82,1.10]),box([.92,.6,.3],[.24,.82,1.10]),box([.5,.82,-.08],[.35,.32,.17],'sinew')],
        [box([.5,.56,.5],[1.17,.84,1.16])]+claws()[-3:])
profile('arthropod',ridge('y',5),face(),sheath())
# Explicit spider abdomen plates and a forked block jaw, not random needle cones.
profiles['arthropod']['body']=ridge('y',5)+[box([.5,.04,.5],[1.08,.30,1.12]),box([.2,-.14,.52],[.15,.16,.50],'sinew'),box([.8,-.14,.52],[.15,.16,.50],'sinew')]
profiles['arthropod']['head']=face()[:3]+[box([x,.95,-.18],[.18,.48,.23],'bone') for x in (.2,.8)]
profile('winged',ridge(count=3)+[box([.5,.5,-.1],[.64,.6,.20],'sinew')],face()[:4],
        [box([.5,.2,.5],[1.12,.18,1.10]),box([.5,.65,.5],[1.07,.12,1.07]),box([.5,.43,.4],[.2,.55,.25],'sinew')])
profile('aquatic',[box([x,.5,.5],[.18,.76,.86]) for x in (-.04,1.04)]+ridge('y',3),face()[:4],sheath(),
        [box([.5,.5,.5],[1.12,1.1,.75]),box([.5,.5,1.06],[.4,.6,.23],'sinew')])
profile('shelled',[box([.5,-.08,.5],[.8,.28,.9]),box([.5,-.26,.5],[.45,.12,.65])]+[box([x,-.02,.5],[.12,.13,.66],'sinew') for x in (.13,.87)],face()[:4],sheath())
profile('amphibian',ridge('y',3)+[box([.5,.15,.5],[1.09,.28,1.04])],face()[:4],claws())
profile('soft',[box([.5,.15,.5],[.9,.35,.85]),box([.25,.55,-.05],[.2,.45,.17],'sinew'),box([.75,.55,-.05],[.2,.45,.17],'sinew')],face()[:3],sheath())
profiles['generic']=deepcopy(profiles['humanoid'])
species={}
groups={'bovine':'cow mooshroom goat ravager hoglin zoglin','porcine':'pig','woolly':'sheep','canine':'wolf fox','feline':'cat ocelot','equine':'horse donkey mule llama trader_llama camel','small_quadruped':'rabbit polar_bear panda',
 'humanoid':'zombie husk drowned zombie_villager skeleton stray bogged wither_skeleton villager wandering_trader pillager vindicator evoker illusioner witch piglin piglin_brute zombified_piglin enderman warden',
 'construct':'iron_golem snow_golem','creeper':'creeper','arthropod':'spider cave_spider silverfish endermite',
 'winged':'chicken parrot bee bat phantom vex allay ghast','aquatic':'cod salmon tropical_fish pufferfish squid glow_squid dolphin guardian elder_guardian',
 'shelled':'turtle armadillo','amphibian':'frog axolotl sniffer strider','soft':'slime magma_cube'}
for key,names in groups.items():
 for name in names.split():species['minecraft:'+name]=key
data={'format':1,'authoring':'Original Minecraft cuboids and pixel atlas; articulated infection morphs, not imported game assets.','species':species,'profiles':profiles,'materials':{'chitin':0,'sinew':1,'bone':2}}
(A/'models/infected_morphology.json').write_text(json.dumps(data,indent=2),encoding='utf8')
SIZE=64;pixels=bytearray([36,29,30,255]*(SIZE*SIZE))
def put(x,y,c):
 i=(y*SIZE+x)*4;pixels[i:i+4]=bytes([*c,255])
for tile,base in enumerate(((46,38,40),(115,39,37),(135,123,107))):
 for y in range(16):
  for x in range(16):
   shade=(4 if (x//3+y//4)%3==0 else 0)- (10 if min(x,y,15-x,15-y)==0 else 0)
   c=[max(0,b+shade) for b in base]
   if tile==0 and x in (5,6) and 3<y<13:c=[74,34,35] if y%4 else [98,42,38]
   if tile==1 and (x-y//4)%5==0:c=[65,25,27]
   if tile==2 and x<3:c=[81,70,66]
   put(tile*16+x,y,c)
def chunk(t,d):return struct.pack('>I',len(d))+t+d+struct.pack('>I',binascii.crc32(t+d)&0xffffffff)
raw=b''.join(b'\0'+pixels[y*SIZE*4:(y+1)*SIZE*4] for y in range(SIZE))
png=b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>2I5B',SIZE,SIZE,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(raw,9))+chunk(b'IEND',b'')
(A/'textures/entity/infected_morphology.png').write_bytes(png)
print('Authored',len(profiles),'morph profiles for',len(species),'explicit vanilla species; anatomical fallbacks for other hosts')
