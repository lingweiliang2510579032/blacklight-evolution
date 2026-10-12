"""Extract reviewed Input-only hooks; never copy animation tracks into the repository.

Usage: python tools/extract_source_combo_timing.py PRIVATE_FIG_ROOT OUTPUT_JSON
Negative event sentinels and mixed-condition events are deliberately unsupported.
"""
import json
import math
import sys
from pathlib import Path

# These owners are also used by the existing local animation exporter. They are
# explicit mappings, not a guessed order of siblings in the source tree.
OWNERS = {
    "muscle_cross": ("bare", 26664, 28248),
    "combo_kick1": ("bare", 40340, 42024),
    "combo_knee": ("bare", 59508, 61344),
    "combo_spin": ("bare", 64968, 66652),
    "combo_axe_handle": ("bare", 85580, 87356),
    "claw_auto1": ("claws", 16640, 18076),
    "claw_auto2": ("claws", 20940, 22312),
    "blade_cut": ("blades", 12256, 13748),
    "blade_combo_c": ("blades", 21048, 22424),
    "blade_combo_c2": ("blades", 25320, 26696),
    "hammer_hook": ("hammerfist", 25392, 27072),
}

# Release hooks for these moves contain an unresolved negative sentinel. Use
# the explicit Input hook on recovery instead, without interpreting that sentinel.
# Tuple: block, windup owner, release animation owner, recovery event owner.
RECOVERY_OWNERS = {
    "blade_return": ("blades", 16648, 18024, 19900),
    "hammer_jab": ("hammerfist", 21048, 22640, 24448),
    "hammer_combo_c": ("hammerfist", 30128, 31808, 33336),
    "whip_lash": ("whipfist", 12796, 16616, 17512),
    "whip_return": ("whipfist", 18844, 22812, 23708),
}


def input_event(node, move):
    events = [t for t in node["tracks"] if t["kind"] == "onEvent"
              and t.get("event_names") == ["Input"] and t.get("time_hook", -1) >= 0
              and t.get("time_begin") == 0 and t.get("unknown_event_bits") == 0]
    if len(events) != 1:
        raise ValueError(f"Unreviewed Input hook: {move}")
    return events[0]


def windup_time(node, move):
    animations = [t for t in node["tracks"] if t["kind"] == "animation"]
    if len(animations) != 1 or animations[0]["time_begin"] != 0 or animations[0]["time_end"] <= 0:
        raise ValueError(f"Unreviewed windup layout: {move}")
    # Whip A's reviewed exporter retains .13 seconds instead of rounding to 60 Hz.
    if move == "whip_lash":
        return round(animations[0]["time_end"], 6)
    return round(animations[0]["time_end"] * 60) / 60


def release_duration(node, move):
    tracks = sorted((t for t in node["tracks"] if t["kind"] == "animation"), key=lambda t: t["time_begin"])
    if not tracks:
        raise ValueError(f"Missing release animation: {move}")
    end = 0
    for track in tracks:
        begin, finish = track["time_begin"], track["time_end"]
        if abs(begin - end) > .0001:
            raise ValueError(f"Unreviewed release gap/overlap: {move}")
        if finish < 0:
            # This is an animation end marker, NOT an onEvent hook. Only the two
            # reviewed whip default poses use this path, matching build_whip.py.
            if move not in ("whip_lash", "whip_return") or track["fps"] != 30 or track["end_frame"] <= track["start_frame"]:
                raise ValueError(f"Unreviewed release end marker: {move}")
            finish = begin + (track["end_frame"] - track["start_frame"]) / 30
        if finish <= begin:
            raise ValueError(f"Invalid release interval: {move}")
        end = finish
    return round(end * 60) / 60


def extract(root):
    tables = {}
    for block in ("bare", "claws", "blades", "hammerfist", "whipfist"):
        relative = "fig-r25-basic" if block == "bare" else "fig-r22"
        table = {}
        def visit(node):
            if node["offset"] in table:
                raise ValueError("Ambiguous source owner")
            table[node["offset"]] = node
            for child in node["child_owners"]:
                visit(child)
        for doc in json.loads((root / relative / f"prototype_{block}-owners.json").read_text(encoding="utf8")):
            visit(doc["root"])
        tables[block] = table
    rows = {}
    for move, (block, windup, release) in OWNERS.items():
        nodes = tables[block]
        event = input_event(nodes[release], move)
        # The existing exporter samples on a 60 Hz timeline. Match its windup
        # clock; keep the source hook as a float until conversion to server ticks.
        windup_seconds = windup_time(nodes[windup], move)
        hook = event["time_hook"]
        if not math.isfinite(hook) or hook > 5:
            raise ValueError(f"Invalid hook: {move}")
        rows[move] = dict(releaseTime=windup_seconds, inputHookSeconds=hook,
                          sourceBlock="prototype_" + block, windupOwner=windup,
                          releaseOwner=release, eventOffset=event["offset"], eventPhase="release")
    for move, (block, windup, release, recovery) in RECOVERY_OWNERS.items():
        nodes = tables[block]
        event = input_event(nodes[recovery], move)
        duration = release_duration(nodes[release], move)
        rows[move] = dict(releaseTime=windup_time(nodes[windup], move),
                          inputHookSeconds=duration + event["time_hook"],
                          sourceBlock="prototype_" + block, windupOwner=windup,
                          releaseOwner=release, recoveryOwner=recovery, releaseDuration=duration,
                          recoveryInputHookSeconds=event["time_hook"],
                          eventOffset=event["offset"], eventPhase="recovery")
    return dict(schema=1, moves=rows)


if __name__ == "__main__":
    result = extract(Path(sys.argv[1]))
    output = Path(sys.argv[2])
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(result, indent=2) + "\n", encoding="utf8")
    print(f"Extracted {len(result['moves'])} reviewed Input-only hooks")
