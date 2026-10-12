"""Original compact pixel atlas: flesh fibres, keratin, lesions and muted horn."""
from pathlib import Path
import json,math,random,struct,zlib
r=Path(__file__).resolve().parents[1]/'src/main/resources/assets/blacklight'
palette={'hide':(60,31,32),'plate':(32,30,31),'red':(111,37,32),'edge':(94,79,68),'dark':(23,21,23)}
def png(path,w,h,pixel):
 raw=bytearray()
 for y in range(h):
  raw.append(0)
  for x in range(w):raw.extend(max(0,min(255,int(v))) for v in pixel(x,y))
 def chunk(t,d):return struct.pack('>I',len(d))+t+d+struct.pack('>I',zlib.crc32(t+d)&0xffffffff)
 path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>2I5B',w,h,8,2,0,0,0))+chunk(b'IDAT',zlib.compress(raw))+chunk(b'IEND',b''))
rng=random.Random(1909)
def pixel(x,y):
 base=list(palette.values())[x//64];u=x%64
 fibre=math.sin(u*.45+math.sin(y*.16)*1.8);fine=math.sin(u*1.55+y*.07);noise=rng.uniform(-6,6)
 shade=(fibre*5+fine*2+noise);return [c+shade for c in base]
png(r/'textures/entity/infected_species.png',320,64,pixel)
for kind in ['stalker','spitter','brute','hive']:
 p=r/f'geo/blacklight_{kind}.geo.json';d=json.loads(p.read_text(encoding='utf8'));geo=d['minecraft:geometry'][0];geo['description']['texture_width']=320;geo['description']['texture_height']=64
 old={(630,680):0,(280,330):1,(130,145):2,(485,138):3,(310,770):4}
 for bone in geo['bones']:
  for cube in bone.get('cubes',[]):
   for face in cube['uv'].values():
    old_uv=tuple(face['uv']);slot=old.get(old_uv,int(face['uv'][0]//64));face['uv']=[slot*64+8,8];face['uv_size']=[40,40]
 p.write_text(json.dumps(d,indent=2),encoding='utf8')
# Broad, irregular tissue knots replace the striped placeholder block pattern.
for name in ['hive_flesh','hive_carapace','hive_sac']:
 base={'hive_flesh':(77,29,31),'hive_carapace':(31,27,29),'hive_sac':(106,42,32)}[name]
 knots=[(rng.uniform(0,32),rng.uniform(0,32),rng.uniform(3,8)) for _ in range(8)]
 def block(x,y):
  d=min(math.hypot(min(abs(x-a),32-abs(x-a)),min(abs(y-b),32-abs(y-b)))/s for a,b,s in knots)
  fold=math.cos(min(2,d)*3)*8;grain=rng.uniform(-6,6);vein=-10 if abs(math.sin(x*.28+math.sin(y*.3)*2))<.15 else 0
  return [v+fold+grain+vein for v in base]
 png(r/f'textures/block/{name}.png',32,32,block)
print('Species atlas and tissue textures exported')
