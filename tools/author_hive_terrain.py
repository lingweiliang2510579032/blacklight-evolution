#!/usr/bin/env python3
"""Author original Minecraft pixel tiles and block resources for the Hive terrain set.

Requires Pillow. No source image, game extraction or external texture is read or altered.
Run from any directory; --preview PATH optionally saves a nearest-neighbour material sheet.
"""
from __future__ import annotations

import argparse
import json
import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src/main/resources"
ASSETS = RES / "assets/blacklight"
NEW = ("infected_soil", "infected_stone", "tissue_fiber", "hive_vein",
       "hive_membrane", "hive_bone", "nutrient_node")
EXISTING = ("hive_flesh", "hive_carapace", "hive_sac")
SIZE = 32


def noise(x: int, y: int, seed: int = 0) -> int:
    value = ((x % SIZE) * 374761393 + (y % SIZE) * 668265263 + seed * 69069) & 0xFFFFFFFF
    value = ((value ^ (value >> 13)) * 1274126177) & 0xFFFFFFFF
    return (value ^ (value >> 16)) & 255


def tint(color: tuple[int, int, int], amount: int) -> tuple[int, int, int]:
    return tuple(max(0, min(255, c + amount)) for c in color)


def base(color: tuple[int, int, int], seed: int, grain: int = 10) -> Image.Image:
    image = Image.new("RGB", (SIZE, SIZE))
    for y in range(SIZE):
        for x in range(SIZE):
            offset = (noise(x // 2, y // 2, seed) % (grain * 2 + 1)) - grain
            offset += noise(x, y, seed + 91) % 5 - 2
            image.putpixel((x, y), tint(color, offset))
    return image


def wrapped_line(draw: ImageDraw.ImageDraw, points: list[tuple[int, int]], color: tuple[int, int, int], width: int):
    for dx in (-32, 0, 32):
        for dy in (-32, 0, 32):
            draw.line([(x + dx, y + dy) for x, y in points], fill=color, width=width)


def soil() -> Image.Image:
    image = base((65, 47, 39), 11, 13)
    draw = ImageDraw.Draw(image)
    for x, y, w, h in ((2, 4, 6, 3), (14, 1, 4, 3), (23, 12, 7, 4),
                       (11, 21, 6, 4), (0, 28, 5, 3), (26, 27, 4, 3)):
        draw.rectangle((x, y, x+w, y+h), fill=(48, 45, 40))
        draw.line((x+1, y, x+w-1, y), fill=(91, 78, 63))
        draw.point((x+2, y+1), fill=(118, 95, 70))
    roots = [[(-3, 7), (5, 9), (10, 8), (16, 13), (22, 13), (29, 7), (35, 7)],
             [(10, -3), (11, 5), (10, 8), (6, 18), (7, 24), (10, 29), (10, 35)],
             [(16, 13), (19, 21), (26, 23), (29, 31)]]
    for path in roots:
        wrapped_line(draw, path, (22, 20, 19), 4)
        wrapped_line(draw, path, (75, 24, 25), 2)
        wrapped_line(draw, [(x, y-1) for x, y in path], (109, 43, 33), 1)
    return image


def stone() -> Image.Image:
    image = base((57, 55, 55), 23, 12)
    draw = ImageDraw.Draw(image)
    paths = [[(-3, 5), (6, 5), (9, 9), (19, 7), (26, 5), (35, 5)],
             [(6, -4), (6, 5), (3, 13), (6, 21), (6, 28), (6, 36)],
             [(9, 9), (12, 17), (20, 19), (23, 24), (32, 25)],
             [(-2, 24), (6, 21), (12, 17)], [(20, 19), (25, 12), (26, 5)]]
    for path in paths:
        wrapped_line(draw, [(x-1, y-1) for x, y in path], (83, 76, 72), 2)
        wrapped_line(draw, path, (24, 23, 25), 3)
        wrapped_line(draw, path, (83, 29, 31), 1)
    for x, y in ((15, 3), (29, 18), (12, 26), (0, 16)):
        draw.rectangle((x, y, x+2, y+1), fill=(97, 85, 71))
        draw.point((x+1, y), fill=(137, 124, 97))
    return image


def fiber() -> Image.Image:
    image = base((56, 26, 29), 37, 8)
    draw = ImageDraw.Draw(image)
    for start in range(-32, 65, 8):
        path = [(start, -1), (start+3, 6), (start+2, 13), (start+6, 21), (start+8, 31)]
        draw.line(path, fill=(23, 20, 23), width=5)
        draw.line([(x+2, y) for x, y in path], fill=(90, 43, 39), width=4)
        draw.line([(x+3, y) for x, y in path], fill=(117, 61, 48), width=1)
        draw.line([(x+1, y) for x, y in path], fill=(71, 30, 32), width=1)
    for x, y in ((5, 9), (20, 21), (28, 2)):
        draw.line((x, y, x+3, y+2), fill=(153, 122, 94), width=1)
        draw.line((x, y+2, x+2, y+3), fill=(103, 77, 60), width=1)
    return image


def vein(frame: int) -> Image.Image:
    image = base((35, 29, 32), 53, 7)
    draw = ImageDraw.Draw(image)
    pulse = int(15 * (1-math.cos(frame * math.tau / 8)) / 2)
    paths = [[(15, -3), (15, 5), (18, 12), (15, 19), (15, 35)],
             [(-3, 10), (6, 10), (10, 15), (16, 17)],
             [(15, 25), (23, 20), (27, 10), (35, 10)]]
    for path in paths:
        wrapped_line(draw, path, (15, 17, 20), 9)
        wrapped_line(draw, [(x-1, y) for x, y in path], (72, 30, 33), 7)
        wrapped_line(draw, path, (95+pulse, 34+pulse//3, 35+pulse//4), 4)
        wrapped_line(draw, [(x-1, y) for x, y in path], (126+pulse, 55+pulse//3, 44), 1)
    for y in (3, 11, 20, 28):
        draw.line((12, y, 19, y+2), fill=(55, 28, 32), width=1)
    return image


def membrane() -> Image.Image:
    image = base((47, 28, 33), 67, 6)
    draw = ImageDraw.Draw(image)
    for row in range(-1, 4):
        for column in range(-1, 4):
            x = column*12 + (6 if row % 2 else 0)
            y = row*12
            draw.polygon([(x+2,y+1),(x+9,y+2),(x+12,y+7),(x+8,y+12),(x+2,y+10),(x,y+5)],
                         fill=(72, 42, 43), outline=(99, 61, 51))
            draw.polygon([(x+3,y+3),(x+8,y+4),(x+9,y+7),(x+6,y+9),(x+3,y+7)], fill=(27, 22, 27))
            draw.line((x+2,y+2,x+7,y+3), fill=(130, 91, 69))
            draw.point((x+8,y+8), fill=(86, 34, 36))
    return image


def bone() -> Image.Image:
    image = base((142, 132, 111), 79, 14)
    draw = ImageDraw.Draw(image)
    for x in (2, 10, 21, 28):
        path = [(x, -3), (x-1, 8), (x+1, 15), (x-1, 24), (x, 35)]
        wrapped_line(draw, path, (83, 78, 70), 3)
        wrapped_line(draw, [(px+2, py) for px, py in path], (191, 177, 143), 1)
        wrapped_line(draw, path, (108, 97, 81), 1)
    draw.polygon([(9,15),(13,12),(16,14),(18,20),(15,24),(11,22)], fill=(46, 31, 31))
    draw.line((12,15,15,17,14,22), fill=(107, 37, 33), width=2)
    for x, y in ((4,5),(17,4),(25,20),(6,27),(24,9)):
        draw.rectangle((x,y,x+1,y+1), fill=(66, 61, 54))
        draw.point((x+2,y), fill=(185, 170, 140))
    return image


def nutrient() -> Image.Image:
    image = base((27, 26, 28), 97, 7)
    draw = ImageDraw.Draw(image)
    draw.polygon([(9,3),(22,3),(28,9),(29,21),(22,28),(10,29),(3,22),(3,10)], fill=(70,38,35))
    draw.polygon([(10,5),(21,5),(26,10),(26,21),(21,26),(10,26),(5,21),(5,10)], fill=(114,47,32))
    draw.polygon([(11,8),(20,8),(23,11),(23,20),(19,24),(12,24),(8,19),(8,12)], fill=(51,24,28))
    draw.polygon([(14,10),(19,11),(21,16),(18,22),(13,21),(11,16)], fill=(155,66,35))
    draw.line((15,11,16,15,14,19), fill=(212,118,58), width=2)
    for path in [[(0,15),(6,15),(12,17)],[(31,16),(25,16),(21,17)],
                 [(15,0),(15,6),(16,10)],[(16,31),(16,26),(16,22)]]:
        draw.line(path, fill=(15,18,22), width=5)
        draw.line(path, fill=(101,35,31), width=2)
        draw.point(path[1], fill=(180,80,41))
    for x,y,dx,dy in ((3,3,1,1),(28,3,-1,1),(3,28,1,-1),(28,28,-1,-1)):
        draw.line((x,y,x+dx*4,y+dy*2,x+dx*5,y+dy*5), fill=(86,82,70), width=3)
        draw.line((x,y,x+dx*4,y+dy*2), fill=(166,153,117), width=1)
    return image


def write_json(path: Path, data: dict):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def resources():
    for name in NEW:
        write_json(ASSETS / "blockstates" / f"{name}.json",
                   {"variants": {"": {"model": f"blacklight:block/{name}"}}})
        write_json(ASSETS / "models/block" / f"{name}.json",
                   {"parent": "minecraft:block/cube_all", "textures": {"all": f"blacklight:block/{name}"}})
    for name in NEW + EXISTING:
        write_json(ASSETS / "models/item" / f"{name}.json", {"parent": f"blacklight:block/{name}"})
        write_json(RES / "data/blacklight/loot_tables/blocks" / f"{name}.json", {
            "type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0,
                "conditions": [{"condition": "minecraft:survives_explosion"}],
                "entries": [{"type": "minecraft:item", "name": f"blacklight:{name}"}]}]})
    tools = {"shovel": ["infected_soil"], "pickaxe": ["infected_stone", "hive_bone", "hive_carapace"],
             "hoe": ["tissue_fiber", "hive_vein", "hive_membrane", "hive_flesh", "hive_sac", "nutrient_node"]}
    for tool, names in tools.items():
        path = RES / "data/minecraft/tags/block/mineable" / f"{tool}.json"
        existing = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"replace": False, "values": []}
        values = list(dict.fromkeys(existing.get("values", []) + [f"blacklight:{name}" for name in names]))
        write_json(path, {**existing, "replace": False, "values": values})


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--preview", type=Path)
    args = parser.parse_args()
    textures = ASSETS / "textures/block"
    textures.mkdir(parents=True, exist_ok=True)
    images = {"infected_soil": soil(), "infected_stone": stone(), "tissue_fiber": fiber(),
              "hive_vein": vein(0), "hive_membrane": membrane(), "hive_bone": bone(), "nutrient_node": nutrient()}
    for name, image in images.items():
        if name == "hive_vein":
            strip = Image.new("RGB", (SIZE, SIZE*8))
            for frame in range(8):
                strip.paste(vein(frame), (0, frame*SIZE))
            strip.save(textures / f"{name}.png")
            write_json(textures / f"{name}.png.mcmeta", {"animation": {"frametime": 5, "interpolate": False}})
        else:
            image.save(textures / f"{name}.png")
    resources()
    if args.preview:
        sheet = Image.new("RGB", (1000, 540), (22, 23, 26))
        draw = ImageDraw.Draw(sheet)
        font = ImageFont.truetype("C:/Windows/Fonts/consola.ttf", 18)
        for i, (name, image) in enumerate(images.items()):
            x, y = 30+(i % 4)*245, 25+(i // 4)*260
            sheet.paste(image.resize((192, 192), Image.Resampling.NEAREST), (x, y))
            draw.text((x, y+205), name, font=font, fill=(219, 211, 196))
            draw.text((x, y+229), "32px | original pixel tile", font=ImageFont.truetype("C:/Windows/Fonts/consola.ttf", 12), fill=(132,134,140))
        args.preview.parent.mkdir(parents=True, exist_ok=True)
        sheet.save(args.preview)
    print(f"Authored {len(NEW)} original textures, {len(NEW)+len(EXISTING)} block items/loot tables and 3 tool tags.")


if __name__ == "__main__":
    main()
