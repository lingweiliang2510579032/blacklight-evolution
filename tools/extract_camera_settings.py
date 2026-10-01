"""Export a private Prototype chase-camera profile from local startup_tod.p3d.

Schema verified against prototypeenginef.dll: registration 0x103462a0,
SmoothingState enum table 0x1107cd40 (preferred image base 0x10000000).
Angular setters convert degrees to radians; exported values retain degrees.
No original game file or generated profile belongs in the public repository.
"""
import argparse,hashlib,json,math,struct,zlib
from pathlib import Path
from extract_utility_settings import objects

STATES=['Ground','Ground_Running','Ground_Parkour','Jump_Vertical','Jump_Leap',
        'Jump_Roof','Climb','WallRun','WallRun_Parkour','Unstable','Grapple','Target']
FIELDS=['filterWidth','changeRate','elevation','elevationChangeRate','elevationSlowingDistance',
        'cameraDistance','cameraDistanceChangeRate','cameraDistanceSlowingDistance',
        'maxLookAtYaw','minLookAtYawCharacterSpeed','maxLookAtYawCharacterSpeed',
        'maxLookAtYawChangeRate','maxLookAtYawSlowingDistance','maxLookAtPitchUp',
        'minLookAtPitchUpCharacterSpeed','maxLookAtPitchUpCharacterSpeed','maxLookAtPitchDown',
        'minLookAtPitchDownCharacterSpeed','maxLookAtPitchDownCharacterSpeed',
        'maxLookAtPitchChangeRate','maxLookAtPitchSlowingDistance']

def extract(path):
    raw=path.read_bytes()
    if path.suffix=='.rz': raw=zlib.decompress(raw[16:])
    matches=[(offset,payload) for offset,name,payload in objects(raw)
             if b'proto::chasecamera::SmoothingDataWrapper(1):ChaseCamSmoothingData' in name]
    if len(matches)!=1: raise ValueError('Expected exactly one chase-camera schema')
    offset,payload=matches[0]
    if len(payload)!=1080 or payload[16:20]!=b'META' or struct.unpack_from('<I',payload,20)[0]!=12:
        raise ValueError('Unsupported chase-camera record layout')
    profiles={}
    for i,state in enumerate(STATES):
        start=24+i*88
        if struct.unpack_from('<I',payload,start)[0]!=i: raise ValueError('Unexpected state order')
        values=struct.unpack_from('<21f',payload,start+4)
        if not all(math.isfinite(v) for v in values): raise ValueError('Nonfinite camera setting')
        profiles[state]=dict(zip(FIELDS,values))
    return {'schema':1,'sourceSha256':hashlib.sha256(raw).hexdigest(),
            'sourceObjectOffset':offset,'profiles':profiles}

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source',type=Path,required=True)
    parser.add_argument('--output',type=Path,required=True)
    args=parser.parse_args();data=extract(args.source)
    args.output.parent.mkdir(parents=True,exist_ok=True)
    args.output.write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')
    print('Exported',len(data['profiles']),'private chase-camera profiles; source',data['sourceSha256'])
