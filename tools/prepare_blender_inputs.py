"""Prepare ignored, local-only Blender inputs from an installed Epic Fight JAR.

Usage: python tools/prepare_blender_inputs.py EPIC_FIGHT_JAR ADDON_SOURCE_DIR
The external reference model and exporter are not bundled with this repository.
"""
import argparse
import shutil
import zipfile
from pathlib import Path

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('epic_fight_jar', type=Path)
parser.add_argument('addon_source_dir', type=Path)
args = parser.parse_args()
if not (args.addon_source_dir / 'compat.py').is_file():
    parser.error('addon_source_dir must contain the exporter compat.py module')
root = Path(__file__).resolve().parents[1]
work = root / 'art-work'
work.mkdir(exist_ok=True)
with zipfile.ZipFile(args.epic_fight_jar) as dependency:
    model = dependency.read('assets/epicfight/animmodels/entity/biped.json')
(work / 'epic-biped.json').write_bytes(model)
shutil.copytree(args.addon_source_dir, work / 'epic-blender-addon', dirs_exist_ok=True,
                ignore=shutil.ignore_patterns('.git', '__pycache__', '*.pyc'))
print('Prepared local-only inputs in', work)
print('Do not commit art-work: it contains external reference assets and exporter code.')
