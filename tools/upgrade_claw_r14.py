"""Enlarge the authored claws without moving shoulder/elbow pivots or mirroring hands.
Run after build_claw_blender.py. Idempotent on an R14 asset.
"""
from pathlib import Path
import json
p=Path(__file__).resolve().parents[1]/'src/main/resources/assets/blacklight/models/claw_blender.json'
data=json.loads(p.read_text())
if data.get('designRevision',0)<14:
    for joint,faces in data['joints'].items():
        width=1.22 if joint.startswith('Hand') else 1.18
        for face in faces:
            for v in face['p']:
                v[0]=round(v[0]*width,6);v[2]=round(v[2]*width,6)
                if joint.startswith('Hand') and v[1]>.42:v[1]=round(.42+(v[1]-.42)*1.28,6)
    data['designRevision']=14
    p.write_text(json.dumps(data,separators=(',',':'))+'\n')
print('Claws revision',data.get('designRevision'),sum(map(len,data['joints'].values())),'triangles')
