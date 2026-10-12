"""Author the independent player's functional bind hierarchy from block proportions.

No mod JAR or external skeleton is read. Names retain the existing Blacklight motion
contract; matrices are generated here from dimensions and elementary rotations.
"""
from pathlib import Path
import json
import math


def transform(x=0.0, y=0.0, z=0.0, rx=0.0):
    c, s = math.cos(rx), math.sin(rx)
    return [1, 0, 0, x, 0, c, -s, y, 0, s, c, z, 0, 0, 0, 1]


def bone(name, matrix, *children):
    return dict(name=name, transform=[round(v, 8) for v in matrix], children=list(children))


def arm(side, x):
    return bone("Shoulder_" + side, transform(y=.4),
                bone("Arm_" + side, transform(x=x, y=-.1, rx=math.pi),
                     bone("Hand_" + side, transform(y=.3, rx=-.11),
                          bone("Tool_" + side, transform(y=.27, rx=math.pi + .02))),
                     bone("Elbow_" + side, transform(y=.3, rx=-math.pi / 2))))


def leg(side, x):
    return bone("Thigh_" + side, transform(x=x, rx=math.pi),
                bone("Leg_" + side, transform(y=.375)),
                bone("Knee_" + side, transform(y=.375, rx=math.pi / 2)))


def main():
    root = bone("Root", transform(z=.75, rx=math.pi / 2),
                leg("R", .125), leg("L", -.125),
                bone("Torso", transform(y=.05),
                     bone("Chest", transform(y=.3),
                          bone("Head", transform(y=.4)), arm("R", .375), arm("L", -.375))))
    names = []

    def visit(node):
        names.append(node["name"])
        for child in node["children"]:
            visit(child)

    visit(root)
    result = dict(joints=names, hierarchy=[root],
                  sourceNotice="Project-authored functional Blacklight player hierarchy. GPL-3.0-only. Generated without reading an external mod or skeleton.")
    destination = Path(__file__).resolve().parents[1] / "src/main/resources/assets/blacklight/animation/player_bind.json"
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    print(f"Authored {len(names)} Blacklight bones: {destination}")


if __name__ == "__main__":
    main()
