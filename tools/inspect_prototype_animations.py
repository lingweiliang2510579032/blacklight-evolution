"""Read local Prototype 1 PC P3D/RZ animation metadata without exporting assets.

Usage: python tools/inspect_prototype_animations.py GAME_DIRECTORY --out REPORT.json
Only the report path is written. The game directory is never modified. This
inventory is not an animation converter and does not establish playback order.
"""
import argparse
import json
import math
from pathlib import Path
import struct
import zlib

MAX_BYTES = 128 * 1024 * 1024
PACKAGES = [Path('art/alex/alex.p3d.rz')] + [
    Path(f'art/packages/powers/alex_{name}/alex_{name}.p3d.rz')
    for name in ('blades', 'claws', 'hammerfist', 'whipfist', 'musclemass')
]


def read_payload(path):
    if path.stat().st_size > MAX_BYTES:
        raise ValueError('Resource exceeds the metadata inspection size limit')
    data = path.read_bytes()
    if data[:2] == b'RZ':
        if len(data) < 16:
            raise ValueError('Truncated RZ header')
        expected = struct.unpack_from('<I', data, 8)[0]
        if expected > MAX_BYTES:
            raise ValueError('Declared RZ size exceeds the inspection limit')
        decoder = zlib.decompressobj()
        data = decoder.decompress(data[16:], expected + 1)
        if len(data) != expected or not decoder.eof:
            raise ValueError('RZ output does not match its declared size')
    if len(data) < 12 or data[:4] != b'P3D\xff':
        raise ValueError('Expected a little-endian PC Pure3D resource')
    if struct.unpack_from('<II', data, 4) != (12, len(data)):
        raise ValueError('P3D envelope does not match the resource length')
    return data


def read_string(payload, offset):
    if offset >= len(payload):
        raise ValueError('Missing P3D string length')
    end = offset + 1 + payload[offset]
    if end > len(payload):
        raise ValueError('P3D string exceeds its payload')
    return payload[offset + 1:end].split(b'\0', 1)[0].decode('utf8'), end


def inspect(data):
    animations, skeletons = [], []

    def walk(offset, end, depth=0):
        if depth > 64:
            raise ValueError('Unexpected P3D nesting depth')
        while offset < end:
            if offset + 12 > end:
                raise ValueError('Truncated P3D chunk header')
            kind, header, total = struct.unpack_from('<III', data, offset)
            if not 12 <= header <= total or offset + total > end:
                raise ValueError('P3D chunk exceeds its parent')
            payload = data[offset + 12:offset + header]
            if kind == 0x00121000:
                name, pos = read_string(payload, 4)
                tag = payload[pos:pos + 4].decode('ascii').rstrip('\0')
                frames, fps, flags = struct.unpack_from('<ffI', payload, pos + 4)
                if not (math.isfinite(frames) and math.isfinite(fps) and frames >= 0 and fps > 0):
                    raise ValueError(f'Invalid animation timing: {name}')
                animations.append({'name': name, 'type': tag, 'frames': frames,
                                   'fps': fps, 'resource_seconds': frames / fps})
            elif kind == 0x00023000:
                name, pos = read_string(payload, 0)
                version, count, partitions, limbs = struct.unpack_from('<4I', payload, pos)
                skeletons.append({'name': name, 'joint_count': count})
            walk(offset + header, offset + total, depth + 1)
            offset += total

    walk(12, len(data))
    return {'skeletal_animation_count': sum(a['type'] == 'PTRN' for a in animations),
            'skeletons': skeletons, 'animations': animations}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('game_directory', type=Path)
    parser.add_argument('--out', type=Path, required=True)
    args = parser.parse_args()
    game = args.game_directory.resolve()
    output = args.out.resolve()
    if output.is_relative_to(game):
        parser.error('Place the report outside the game directory; inspection is read-only')
    report = {'notice': 'Metadata only. Resource length is not FIG action duration. No game assets or motion curves are exported.',
              'packages': []}
    for relative in PACKAGES:
        path = game / relative
        if not path.is_file():
            report['packages'].append({'file': relative.as_posix(), 'missing': True})
            continue
        record = {'file': relative.as_posix(), **inspect(read_payload(path))}
        report['packages'].append(record)
        print(f"{relative.name}: {record['skeletal_animation_count']} skeletal animations")
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf8')


if __name__ == '__main__':
    main()
