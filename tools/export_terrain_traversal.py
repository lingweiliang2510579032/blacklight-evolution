"""Export two private traversal clips; source assets and generated JSON stay local.

Uses the existing local numpy_retarget engine. Does not reconstruct source engine
collision, obstacle thresholds, random variants or movement state arbitration.
"""
import argparse
import hashlib
import json
import math
import os
from pathlib import Path
import sys

os.environ.setdefault("OPENBLAS_NUM_THREADS", "1")
os.environ.setdefault("OMP_NUM_THREADS", "1")
parser = argparse.ArgumentParser()
parser.add_argument("--decoded", type=Path, required=True)
parser.add_argument("--retarget", type=Path, required=True)
parser.add_argument("--output", type=Path, required=True)
args = parser.parse_args()
sys.path.insert(0, str(args.retarget))
from numpy_retarget import Retarget, matrix, np

# Source FIG prototype_sprint owner 84540: frames 0..25 in 0.833333 s.
# Source FIG prototype_wall owner 83276: full alex_wall_run_top in 0.6 s.
specs = [
    ("basic_parkour_small", "alex_loco_parkour_small_basic", 25, 25 / 30, 84540),
    ("basic_wall_top", "alex_wall_run_top", 12, .6, 83276),
]
args.output.mkdir(parents=True, exist_ok=True)
for name, source, end, seconds, owner in specs:
    path = args.decoded / "alex" / (source + ".json")
    raw = path.read_bytes()
    rt = Retarget(json.loads(raw), skeleton="alex")
    frames, audit = rt.generate([dict(start=0, end=end, seconds=seconds, blend=0)])
    target = rt.target
    times = [i / 60 for i in range(len(frames))]
    origin = rt.staged_world(0)["Motion_Root"][:3, 3]
    roots, coords = [], []
    for i, frame in enumerate(frames):
        root = target.rest["Root"] @ matrix(frame["Root"])
        delta = (rt.staged_world(times[i])["Motion_Root"][:3, 3] - origin) * rt.scale
        root[:3, 3] -= delta
        root[2, 3] -= (origin[2] - rt.source.bind["Motion_Root"][2, 3]) * rt.scale
        roots.append(root)
        coord = np.eye(4)
        coord[:3, 3] = delta
        coords.append(coord)
    tracks = []
    for bone in target.names:
        matrices = roots if bone == "Root" else [target.rest[bone] @ matrix(f[bone]) for f in frames]
        tracks.append(dict(name=bone, time=times, transform=[m.reshape(-1).tolist() for m in matrices]))
    tracks.append(dict(name="Coord", time=times, transform=[m.reshape(-1).tolist() for m in coords]))
    result = dict(format="MATRIX", animation=tracks,
                  blacklight=dict(durationTicks=math.ceil((times[-1] + .05) * 20 - .00001), transitionSeconds=.05),
                  privateNotice="Original-derived, local only. Gameplay never applies diagnostic Coord.",
                  source=dict(animation=source, sha256=hashlib.sha256(raw).hexdigest(), owner=owner))
    (args.output / (name + ".json")).write_text(json.dumps(result, separators=(",", ":"), allow_nan=False), encoding="utf-8")
    print(name, "seconds", times[-1], "foot error", audit["footEndpointMaxError"])
