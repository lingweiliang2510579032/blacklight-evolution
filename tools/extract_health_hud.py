"""Extract the user's local Prototype HUD textures/layout into a private profile.
No derived files are written to public resources. The numbers below are decoded
protohud.gfx sprite placements (296 / 295 / 278), not redrawn artwork.
"""
from pathlib import Path
import argparse,struct,zlib,io,json,base64,hashlib
from PIL import Image

def chunks(data,start=0,end=None):
 end=len(data) if end is None else end
 while start<end:
  kind,header,total=struct.unpack_from('<III',data,start)
  if not 12<=header<=total or start+total>end:raise ValueError('Invalid P3D chunk')
  yield kind,data[start+12:start+header],list(chunks(data,start+header,start+total))
  start+=total

def walk(nodes):
 for node in nodes:
  yield node
  yield from walk(node[2])

def main():
 parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('source',type=Path);parser.add_argument('output',type=Path);a=parser.parse_args()
 compressed=(a.source/'fe_ingame.p3d.rz').read_bytes();raw=zlib.decompress(compressed[16:]);assert len(raw)==struct.unpack_from('<I',compressed,8)[0]
 names={'protohud__IF0.dds':'heart','protohud__I10B.dds':'junction','protohud__I109.dds':'underline','protohud__I118.dds':'pulse'}
 textures={}
 for k,p,children in walk(chunks(raw)):
  if k!=0x19000:continue
  name=p[1:1+p[0]].rstrip(b'\0').decode().replace('\\','/').split('/')[-1]
  if name not in names:continue
  payload=next(v for _,v,_ in walk(children) if v.find(b'DDS ') in (0,4))
  image=Image.open(io.BytesIO(payload[payload.index(b'DDS '):])).convert('RGBA');buffer=io.BytesIO();image.save(buffer,format='PNG')
  textures[names[name]]={'png':base64.b64encode(buffer.getvalue()).decode(),'width':image.width,'height':image.height,'source':name,'sha256':hashlib.sha256(payload).hexdigest()}
 assert len(textures)==4
 # GFX is a 1280x720 stage. Coordinates are local to healthHUD.
 profile={'schema':1,'sourceSha256':hashlib.sha256(compressed).hexdigest(),'gfxSha256':hashlib.sha256((a.source/'protohud.gfx').read_bytes()).hexdigest(),
  'width':672,'height':68,'heart':[-3.5,.85,64,64],'junction':[51,1,64,64],
  'bar':[67,24,600,10],'underline':[67,31.5,600,8],
  'fillColor':[155,0,0],'edgeColor':[255,51,0],'damageColor':[255,153,0],'alpha':.8984375,
  'textures':textures}
 a.output.parent.mkdir(parents=True,exist_ok=True);a.output.write_text(json.dumps(profile,separators=(',',':')))
 print('Exported original HUD: four DDS textures and decoded healthHUD layout ->',a.output)
if __name__=='__main__':main()
