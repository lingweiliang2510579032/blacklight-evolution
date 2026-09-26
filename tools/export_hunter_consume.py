"""Export the omitted receiver scale channel into a private motion directory.

Inputs are locally decoded P3D JSON and the existing retargeted actor clip.
This exports animation data; it does not run tests or launch Minecraft.
Never commit the output or redistribute the original-derived motion library.
"""
import argparse
import hashlib
import json
from pathlib import Path

parser = argparse.ArgumentParser()
parser.add_argument("receiver", type=Path)
parser.add_argument("motion_directory", type=Path)
args = parser.parse_args()
raw = args.receiver.read_bytes()
source = json.loads(raw)
if source["name"] != "alex_brawl_consume_2_rcv" or source["frame_rate"] != 30:
    raise ValueError("Expected the active Brawler consume-2 receiver at 30 fps")
channels = [c for g in source["groups"] if g["name"] == "Jaw"
            for c in g["channels"] if c["semantic"] == "SCL"]
if len(channels) != 1:
    raise ValueError("Expected exactly one Jaw scale channel")
channel = channels[0]
if len(channel["times"]) != len(channel["values"]):
    raise ValueError("Mismatched scale keys")
actor = json.loads((args.motion_directory / "consume_large_actor.json").read_text())
payload = dict(schema=1, source=source["name"], sourceSha256=hashlib.sha256(raw).hexdigest(),
               actorClipSeconds=max(t for track in actor["animation"] for t in track["time"]),
               jawScale=[[t, *v] for t, v in zip(channel["times"], channel["values"])],
               privateNotice="Original-derived animation channel. Private local use only.")
output = args.motion_directory / "hunter-consume.data"
output.write_text(json.dumps(payload, separators=(",", ":"), allow_nan=False), encoding="utf8")
print(f"Exported {len(payload['jawScale'])} Jaw scale keys to {output}")
