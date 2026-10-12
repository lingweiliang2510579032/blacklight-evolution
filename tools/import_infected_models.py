"""Import the approved original infected meshes as a compact runtime skeleton atlas.

No Blender or Pillow dependency. Source mesh JSON and MTL files are exported from
the editable model library; this command never changes those approved sources.

Usage: python tools/import_infected_models.py --ordinary <infected-all-species>
       --bosses <infected-bosses-v1>

Runtime coordinates are blocks, Y up, front -Z. Vertices and pivots are absolute
rest coordinates. Each bone contains packed xyz vertices and i0,i1,i2,rgb faces.
Parent matrices multiply T(pivot) R T(-pivot), without changing those coordinates.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import math
from collections import defaultdict
from pathlib import Path

ALIASES = {"Chest": "body", "Head": "head", "front_left": "front_1",
           "front_right": "front_-1", "hind_left": "hind_1", "hind_right": "hind_-1"}
BOSS_IDS = {"warden", "iron_golem", "wither", "ender_dragon"}
# Species whose editable detail previews deliberately share another species'
# scale. These are fixed adult reference scales, not gameplay attribute scales.
# Full bounds include infection horns/wings; fitting those bounds to the vanilla
# collision box would make the intact torso much too small.
SOURCE_SCALE = {"giant": 6.0, "cave_spider": 0.5, "elder_guardian": 2.35,
                "ghast": 4.0, "allay": 0.5, "vex": 0.7,
                "chicken": 0.6, "parrot": 0.75, "turtle": 0.65,
                "horse": 0.75, "mule": 0.75, "donkey": 0.7,
                "skeleton_horse": 0.75, "zombie_horse": 0.75,
                "tropical_fish": 0.55, "cod": 0.75, "wither_skeleton": 1.2}


def read(path):
    return json.loads(path.read_text(encoding="utf-8"))


def coordinates(v):
    return [round(v[0] / 16, 6), round(v[2] / 16, 6), round(v[1] / 16, 6)]


def bounds(points):
    points = list(points)
    return {"min": [round(min(p[i] for p in points), 6) for i in range(3)],
            "max": [round(max(p[i] for p in points), 6) for i in range(3)]}


def center(bound):
    return [(a + b) / 2 for a, b in zip(bound["min"], bound["max"])]


def palette(path):
    colors = {}
    name = None
    for line in path.read_text(encoding="utf-8").splitlines():
        if line.startswith("newmtl "):
            name = line[7:]
        elif line.startswith("Kd "):
            r, g, b = [min(255, max(0, round(float(v) * 255))) for v in line[3:].split()]
            colors[name] = r << 16 | g << 8 | b
    return colors


def normalized_bone(part):
    name = part["bone"]
    if name in ALIASES:
        return ALIASES[name]
    # Legacy zombie bindings use anatomical names from the previous authoring
    # rig. In these source coordinates positive X is the author's R side.
    if name in ("Arm_R", "Arm_L", "Hand_R", "Hand_L", "Leg_R", "Leg_L"):
        side = 1 if sum(v[0] for v in part["vertices"]) >= 0 else -1
        return ("leg_" if name.startswith("Leg") else "arm_") + str(side)
    return name


def primary_parts(parts):
    base = [p for p in parts if p["name"].startswith("Base /")]
    # Discard thin pixel decals when finding structural joint sockets.
    main = [p for p in base if not any(s in p["name"].lower()
            for s in ("pixel", "patch", "eye", "trim", "stripe", "band", "nostril", "ear", "nose", "beak", "crest"))]
    return main or base or parts


def ordinary_rig(key, groups):
    rig = {"root": {"pivot": [0., 0., 0.], "parent": None}}
    body_parts = primary_parts(groups.get("body", []))
    fallback = [p for ps in groups.values() for p in ps]
    bb = bounds(v for p in (body_parts or fallback) for v in p["vertices"])
    # Everything is still Z-up source pixels here.
    bc = center(bb)
    rig["body"] = {"pivot": [0., 0., bb["min"][2]], "parent": "root"}
    for name, parts in groups.items():
        if name == "body":
            continue
        source = primary_parts(parts)
        b = bounds(v for p in source for v in p["vertices"])
        lo, hi = b["min"], b["max"]
        p = center(b)
        parent = "body"
        if name == "head":
            # A neck attaches at the rear face for quadrupeds, the bottom for
            # bipeds. Do not let antlers and ear tips move the pivot.
            main_head = [q for q in source if any(s in q["name"].lower()
                         for s in ("head", "skull"))]
            if main_head:
                hb = bounds(v for q in main_head[:1] for v in q["vertices"])
                p = center(hb)
                quadruped = any(n.startswith(("front_", "hind_")) for n in groups)
                if quadruped or key in {"spider", "cave_spider", "phantom", "bee", "dolphin", "cod", "salmon", "tropical_fish"}:
                    p[1] = hb["max"][1]
                else:
                    p[2] = hb["min"][2]
            if "neck" in groups:
                parent = "neck"
        elif name == "neck":
            p[2] = lo[2]
            p[1] = hi[1]
        elif name == "arms":
            p = [0., 0., bb["max"][2] - 1]
        elif name.startswith(("front_", "hind_")) or (name.startswith("leg_") and name.count("_") == 1):
            leg_boxes = [q for q in source if any(w in q["name"].lower() for w in ("leg", "front_", "hind_"))]
            if leg_boxes:
                b = bounds(v for q in leg_boxes[:1] for v in q["vertices"])
                p = center(b)
                p[2] = b["max"][2]
            else:
                p[2] = hi[2]
        elif name.startswith("arm_"):
            if key in {"squid", "glow_squid"}:
                a = int(name[4:]) * math.tau / 8
                p = [math.cos(a) * 4, math.sin(a) * 4, 13]
            elif key == "snow_golem":
                p = [(-4 if name.endswith("-1") else 4), 0, 18]
            else:
                arm_boxes = [q for q in source if "arm" in q["name"].lower()]
                if arm_boxes:
                    b = bounds(v for q in arm_boxes[:1] for v in q["vertices"])
                    p = center(b)
                    p[2] = b["max"][2]
                else:
                    p[2] = bb["max"][2] - 1
        elif name.startswith("leg_"):
            _, side, index = name.split("_")
            side, index = int(side), int(index)
            if key in {"spider", "cave_spider"}:
                p = [side * 3, -4 + index * 3, 7]
            elif key == "bee":
                p = [side * 3, -3 + index * 3, 5]
            elif key == "creeper":
                p = [side, index, 6]
            else:
                widths = [3, 4, 5, 4, 2]
                p = [side * widths[index] / 2, -5 + index * 2.7, 3]
                parent = "segment_" + str(index)
        elif name.startswith("wing_"):
            side = -1 if name.endswith("-1") else 1
            roots = {"bat": (2, 0, 13), "phantom": (3, -3, 11),
                     "bee": (2, 0, 12), "allay": (1.5, 1.5, 11), "vex": (1.5, 1.5, 11),
                     "chicken": (3, -2, 13), "parrot": (3, -2, 13)}
            x, y, z = roots[key]
            p = [x * side, y, z]
        elif name.startswith("paddle_"):
            _, side, front = name.split("_")
            p = [int(side) * 6, int(front), 2]
            parent = ("front_" if int(front) < 0 else "hind_") + side
        elif name.startswith("tentacle_"):
            i = int(name[9:])
            p = [(i % 3 - 1) * 5, (i // 3 - 1) * 5, 18]
        elif name.startswith("fin_"):
            # First vertex of these author-created fins is the body socket.
            p = parts[0]["vertices"][0]
        elif name == "tail":
            # Tail authors place the first cross-section/vertex at the rump.
            first = source[0]
            if len(first["vertices"]) <= 4:
                p = first["vertices"][0]
            else:
                p[1] = lo[1]
        elif name.startswith("rod_") or name.startswith("wind_"):
            p = [0., 0., p[2]]
        elif name == "abdomen":
            p[1] = lo[1]
        rig[name] = {"pivot": list(p), "parent": parent}
    return rig


def ordered_rig(rig):
    result, done = [], set()
    while len(done) < len(rig):
        changed = False
        for name, bone in rig.items():
            if name not in done and (bone["parent"] is None or bone["parent"] in done):
                result.append((name, bone))
                done.add(name)
                changed = True
        if not changed:
            raise ValueError("Cyclic or missing parent: " + str(set(rig) - done))
    return result


def convert(key, parts, colors, boss_manifest=None):
    groups = defaultdict(list)
    for p in parts:
        if p["faces"]:
            groups[p["bone"] if boss_manifest else normalized_bone(p)].append(p)
    if boss_manifest:
        rig = {n: {"pivot": b["p"], "parent": b["parent"]} for n, b in boss_manifest["rig"].items()}
        unknown = set(groups) - set(rig)
        if unknown:
            raise ValueError(f"Boss {key} has unbound groups {unknown}")
    else:
        rig = ordinary_rig(key, groups)
    data = {"format": 1, "species": "minecraft:" + key, "boss": bool(boss_manifest),
            "referenceScale": SOURCE_SCALE.get(key, 1.),
            "bounds": bounds(coordinates(v) for p in parts for v in p["vertices"]),
            "baseBounds": bounds(coordinates(v) for p in primary_parts(parts) for v in p["vertices"]),
            "bones": []}
    for name, bone in ordered_rig(rig):
        vertices, faces, vertex_index = [], [], {}
        for p in groups.get(name, []):
            local_indices = []
            for vertex in p["vertices"]:
                xyz = tuple(coordinates(vertex))
                if not all(math.isfinite(v) for v in xyz):
                    raise ValueError("Non-finite vertex in " + key)
                if xyz not in vertex_index:
                    vertex_index[xyz] = len(vertices) // 3
                    vertices.extend(xyz)
                local_indices.append(vertex_index[xyz])
            if len(p["faces"]) != len(p["materials"]):
                raise ValueError("Face/material count mismatch in " + key)
            for polygon, material in zip(p["faces"], p["materials"]):
                # Axis swapping changes handedness. Reverse the winding once.
                polygon = [local_indices[i] for i in reversed(polygon)]
                for i in range(1, len(polygon) - 1):
                    ids = [polygon[0], polygon[i], polygon[i + 1]]
                    if len(set(ids)) < 3:
                        continue
                    faces.extend(ids + [colors[material]])
        data["bones"].append({"name": name, "parent": bone["parent"],
                              "pivot": coordinates(bone["pivot"]), "vertices": vertices, "triangles": faces})
    floor = data["bounds"]["min"][1]
    for bone in data["bones"]:
        bone["pivot"][1] = round(bone["pivot"][1] - floor, 6)
        for i in range(1, len(bone["vertices"]), 3):
            bone["vertices"][i] = round(bone["vertices"][i] - floor, 6)
    for field in ("bounds", "baseBounds"):
        data[field]["min"][1] = round(data[field]["min"][1] - floor, 6)
        data[field]["max"][1] = round(data[field]["max"][1] - floor, 6)
    return data


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--ordinary", type=Path, required=True)
    parser.add_argument("--bosses", type=Path, required=True)
    parser.add_argument("--output", type=Path, default=Path(__file__).resolve().parents[1] / "src/main/resources/assets/blacklight/infected_models")
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    coverage = read(args.ordinary / "coverage.json")
    bosses = read(args.bosses / "manifest.json")["models"]
    if set(bosses) != BOSS_IDS:
        raise ValueError("Expected all four approved giant boss models")
    colors = {False: palette(args.ordinary / "materials.mtl"), True: palette(args.bosses / "materials.mtl")}
    manifest = {"format": 1, "coordinates": "blocks, Y-up, front=-Z, absolute rest vertices/pivots",
                "triangleLayout": "flat groups of i0, i1, i2, packed RGB", "models": []}
    total_triangles = 0
    for key in sorted(set(coverage) | set(bosses)):
        is_boss = key in bosses
        folder = args.bosses if is_boss else args.ordinary
        source = folder / (key + "-geometry.json")
        model = convert(key, read(source), colors[is_boss], bosses.get(key))
        output = args.output / (key + ".json")
        output.write_text(json.dumps(model, separators=(",", ":"), allow_nan=False) + "\n", encoding="utf-8")
        triangles = sum(len(b["triangles"]) // 4 for b in model["bones"])
        total_triangles += triangles
        manifest["models"].append({"id": "minecraft:" + key,
            "file": "blacklight:infected_models/" + key + ".json", "boss": is_boss,
            "bones": len(model["bones"]), "triangles": triangles,
            "bounds": model["bounds"], "baseBounds": model["baseBounds"],
            "referenceScale": model["referenceScale"],
            "sourceSha256": hashlib.sha256(source.read_bytes()).hexdigest()})
    if len(manifest["models"]) != 82:
        raise ValueError("Expected 82 distinct species after overriding two ordinary models")
    (args.output / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    print(f"Imported {len(manifest['models'])} species, {len(bosses)} giant bosses, {total_triangles:,} triangles -> {args.output}")


if __name__ == "__main__":
    main()
