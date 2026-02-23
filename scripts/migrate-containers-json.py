#!/usr/bin/env python3
"""
Migrate containers.json from old schema to new schema.

Usage:
    python migrate-containers-json.py <input.json> [output.json]
    python migrate-containers-json.py data/system/containers.json --in-place
"""

import json
import sys
import shutil
from pathlib import Path


def migrate_container(name: str, container: dict) -> dict:
    """Migrate a single container to the new schema."""
    new = {
        'type': container.get('type', 'ds'),
        'name': container.get('name', name),
        'image': container.get('image'),
        'host': container.get('host', 'localhost'),
        'port': container.get('port'),
        'imageSize': container.get('imageSize'),
        'installDate': container.get('InstallDate') or container.get('installDate'),
        'lastImageId': container.get('lastImageId'),
        'dockerArgs': container.get('dockerArgs', []),
        'dockerOptions': container.get('dockerOptions', {}),
        'packageWhitelist': container.get('packageWhitelist', []),
        'functionBlacklist': container.get('functionBlacklist', []),
        'datashieldROptions': container.get('options') or container.get('datashieldROptions', {}),
        'autoUpdate': container.get('autoUpdate', False),
        'updateSchedule': container.get('updateSchedule'),
        'versionId': container.get('versionId'),
        'creationDate': container.get('CreationDate') or container.get('creationDate'),
    }
    return new


def migrate(data: dict) -> dict:
    """Migrate the entire containers.json structure."""
    new_containers = {}
    for name, container in data.get('containers', {}).items():
        new_containers[name] = migrate_container(name, container)
    return {'containers': new_containers}


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(1)

    in_place = '--in-place' in sys.argv
    args = [a for a in sys.argv[1:] if not a.startswith('--')]
    input_path = Path(args[0])

    with open(input_path, 'r') as f:
        data = json.load(f)

    migrated = migrate(data)
    output_json = json.dumps(migrated)

    if in_place:
        shutil.copy(input_path, input_path.with_suffix('.json.bak'))
        with open(input_path, 'w') as f:
            f.write(output_json)
        print(f"Migrated {input_path} (backup: {input_path.with_suffix('.json.bak')})")
    elif len(args) > 1:
        with open(args[1], 'w') as f:
            f.write(output_json)
    else:
        print(output_json)


if __name__ == '__main__':
    main()