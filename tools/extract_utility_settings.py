"""Read utility tuning from a locally unpacked Prototype 1 alex_tod.p3d.

No game files are distributed with this tool. Schema order was cross-checked
against prototypeenginef.dll metadata (ShieldPower at VA 0x1008bac0 and
TransformationDescription near VA 0x10087c6c, preferred base 0x10000000).
This is a narrow schema reader, not a generic TOD decoder. Fail on other schemas.
"""
import argparse
import hashlib
import json
from pathlib import Path
import struct


def objects(data, offset=0):
    kind, header, size = struct.unpack_from('<III', data, offset)
    if not 12 <= header <= size or offset + size > len(data):
        raise ValueError(f'Invalid chunk at {offset:#x}')
    if kind == 0x07f00000:
        yield offset, data[offset + 12:offset + header], data[offset + header:offset + size]
    child = offset + header
    while child < offset + size:
        yield from objects(data, child)
        child += struct.unpack_from('<I', data, child + 8)[0]
    if child != offset + size:
        raise ValueError('Misaligned chunk boundary')


def extract(path):
    data = path.read_bytes()
    result = {'sourceSha256': hashlib.sha256(data).hexdigest(), 'objects': {}}
    for offset, name, payload in objects(data):
        if b'ShieldPower(1):ShieldPower' in name:
            marker = b'L_Wrist_Grapple'
            start = payload.index(marker) + len(marker)
            values = struct.unpack_from('<7f', payload, start)
            fields = ('deployedScale', 'deploySpeed', 'retractSpeed', 'health',
                      'healingTime', 'undamagedTimeBeforeHealing', 'autoHealFromBrokenTime')
            assert payload[start + 28:start + 32] == struct.pack('<I', 19)
            result['objects']['shield'] = {'chunkOffset': hex(offset), 'fields': dict(zip(fields, values))}
        elif b'TransformationDescription(1):AlexArmourTransformationDescription' in name:
            marker = b'\x07\0\0\0powered'
            start = payload.index(marker) + len(marker)
            values = struct.unpack_from('<3f', payload, start)
            assert struct.unpack_from('<I', payload, start + 12)[0] == 6
            result['objects']['armor'] = {'chunkOffset': hex(offset), 'fields': dict(zip(
                ('damageMultiplier', 'healthDamageScale', 'damageAttackerAmount'), values))}
    if set(result['objects']) != {'shield', 'armor'}:
        raise ValueError('Required utility objects missing or ambiguous source schema')
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('source', type=Path)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    result = extract(args.source)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(result, indent=2))
