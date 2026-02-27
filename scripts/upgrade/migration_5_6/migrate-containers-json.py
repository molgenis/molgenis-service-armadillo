#!/usr/bin/env python3
"""
Migrate Armadillo 5 to 6: profiles.json -> containers.json.

Usage:
    python migrate-containers-json.py <data-dir>
    python migrate-containers-json.py /usr/share/armadillo/data
"""

import json
import sys
from pathlib import Path

DEFAULT_DATA_DIR = Path('/usr/share/armadillo/data')

GREEN = '\033[32m'
RED = '\033[31m'
CYAN = '\033[36m'
BOLD = '\033[1m'
RESET = '\033[0m'


def info(msg):
    print(f"  {GREEN}✔{RESET} {msg}")


def warn(msg):
    print(f"  {CYAN}ℹ{RESET} {msg}")


def error(msg):
    print(f"  {RED}✖{RESET} {msg}")


# --- containers.json migration ---

def infer_type(container: dict) -> str:
    """Infer container type based on DataSHIELD-specific fields."""
    whitelist = container.get('packageWhitelist', [])
    blacklist = container.get('functionBlacklist', [])
    options = container.get('options') or container.get('datashieldROptions', {})

    if whitelist or blacklist or options:
        return 'ds'
    return 'vanilla'


def migrate_container(name: str, container: dict) -> dict:
    """Migrate a single container to the new schema."""
    container_type = container.get('type') or infer_type(container)

    new = {
        'type': container_type,
        'name': container.get('name', name),
        'image': container.get('image'),
        'host': container.get('host', 'localhost'),
        'port': container.get('port'),
        'imageSize': container.get('imageSize'),
        'installDate': container.get('InstallDate') or container.get('installDate'),
        'lastImageId': container.get('lastImageId'),
        'dockerArgs': container.get('dockerArgs', []),
        'dockerOptions': container.get('dockerOptions', {}),
        'autoUpdate': container.get('autoUpdate', False),
        'updateSchedule': container.get('updateSchedule'),
        'versionId': container.get('versionId'),
        'creationDate': container.get('CreationDate') or container.get('creationDate'),
    }

    if container_type == 'ds':
        new['packageWhitelist'] = container.get('packageWhitelist', [])
        new['functionBlacklist'] = container.get('functionBlacklist', [])
        new['datashieldROptions'] = container.get('options') or container.get('datashieldROptions', {})

    return new


def migrate_json(data: dict) -> dict:
    """Migrate the entire profiles.json structure."""
    source = data.get('profiles') or data.get('containers') or {}
    new_containers = {}
    for name, container in source.items():
        new_containers[name] = migrate_container(name, container)
    return {'containers': new_containers}


def migrate_containers_json(data_dir: Path):
    """Migrate profiles.json to containers.json."""
    system_dir = data_dir / 'system'
    containers_path = system_dir / 'containers.json'
    profiles_path = system_dir / 'profiles.json'

    if containers_path.exists():
        warn(f"containers.json already exists at {BOLD}{containers_path}{RESET}, skipping.")
        return

    if not profiles_path.exists():
        error(f"Neither containers.json nor profiles.json found in {BOLD}{system_dir}{RESET}")
        sys.exit(1)

    info(f"Found {BOLD}{profiles_path}{RESET}")

    with open(profiles_path, 'r') as f:
        data = json.load(f)

    migrated = migrate_json(data)
    n = len(migrated.get('containers', {}))

    with open(containers_path, 'w') as f:
        json.dump(migrated, f)

    info(f"Migrated {BOLD}{n}{RESET} container(s) to {BOLD}{containers_path}{RESET}")


# --- main ---

def main():
    data_dir = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_DATA_DIR

    print()
    migrate_containers_json(data_dir)
    print()
    warn("Please verify that your containers work correctly in Armadillo 6.")
    warn(f"Once verified, remove the old files:")
    warn(f"  {BOLD}rm {data_dir / 'system' / 'profiles.json'}{RESET}")
    print()


if __name__ == '__main__':
    main()
