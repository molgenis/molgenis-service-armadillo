"""
Configuration bridge: translates Armadillo environment variables
into a vantage6 node configuration dictionary.
"""

import os
import logging
from pathlib import Path
from glob import glob

log = logging.getLogger(__name__)

# Vantage6 node configuration keys mapped from env vars
ENV_MAPPINGS = {
    "V6_SERVER_URL": "server_url",
    "V6_API_KEY": "api_key",
    "V6_COLLABORATION_ID": "collaboration_id",
    "V6_ENCRYPTION_KEY_PATH": "encryption_key_path",
    "V6_DATA_DIR": "data_dir",
    "V6_ALLOWED_ALGORITHMS": "allowed_algorithms",
    "V6_ALLOWED_ALGORITHM_STORES": "allowed_algorithm_stores",
    "V6_AUTHORIZED_PROJECTS": "authorized_projects",
}


def get_env(key: str, default: str = None, required: bool = False) -> str | None:
    """Get environment variable with optional default and required check."""
    value = os.environ.get(key, default)
    if required and value is None:
        raise EnvironmentError(f"Required environment variable {key} is not set")
    return value


def parse_csv_env(key: str) -> list[str]:
    """Parse a comma-separated environment variable into a list."""
    value = os.environ.get(key, "")
    if not value:
        return []
    return [item.strip() for item in value.split(",") if item.strip()]


def discover_databases(data_dir: str, authorized_projects: list[str]) -> list[dict]:
    """
    Discover Armadillo Parquet files and register them as vantage6 databases.

    Only includes projects listed in authorized_projects. If the list is empty,
    no databases are registered (secure by default).

    Args:
        data_dir: Path to Armadillo's data root directory
        authorized_projects: List of project names allowed for this collaboration

    Returns:
        List of vantage6 database configuration dictionaries
    """
    databases = []
    data_path = Path(data_dir)

    if not data_path.is_dir():
        log.warning(f"Data directory does not exist: {data_dir}")
        return databases

    for project_dir in sorted(data_path.iterdir()):
        if not project_dir.is_dir():
            continue

        project_name = project_dir.name

        # Only include authorized projects
        if authorized_projects and project_name not in authorized_projects:
            log.debug(f"Skipping unauthorized project: {project_name}")
            continue

        # Find all Parquet files in this project
        parquet_files = sorted(glob(str(project_dir / "**" / "*.parquet"), recursive=True))

        if parquet_files:
            # Register the project directory as a database
            databases.append({
                "label": project_name,
                "uri": str(project_dir),
                "type": "parquet",
                "is_dir": True,
                "is_file": False,
            })
            log.info(
                f"Registered project '{project_name}' with "
                f"{len(parquet_files)} Parquet file(s)"
            )

    log.info(f"Discovered {len(databases)} authorized database(s)")
    return databases


def build_node_config() -> dict:
    """
    Build a vantage6 node configuration dictionary from environment variables.

    Returns:
        Dictionary compatible with vantage6's node configuration format
    """
    server_url = get_env("V6_SERVER_URL", required=True)
    api_key = get_env("V6_API_KEY", required=True)
    data_dir = get_env("V6_DATA_DIR", default="/mnt/armadillo-data")

    collaboration_id = get_env("V6_COLLABORATION_ID")
    encryption_key_path = get_env("V6_ENCRYPTION_KEY_PATH")
    authorized_projects = parse_csv_env("V6_AUTHORIZED_PROJECTS")
    allowed_algorithms = parse_csv_env("V6_ALLOWED_ALGORITHMS")
    allowed_algorithm_stores = parse_csv_env("V6_ALLOWED_ALGORITHM_STORES")

    # Discover databases from the Armadillo data directory
    databases = discover_databases(data_dir, authorized_projects)

    config = {
        "server_url": server_url,
        "port": 443,
        "api_path": "/api",
        "api_key": api_key,
        "databases": databases,
        "logging": {
            "level": os.environ.get("V6_LOG_LEVEL", "INFO"),
        },
    }

    if collaboration_id:
        config["collaboration_id"] = int(collaboration_id)

    # Encryption configuration
    if encryption_key_path:
        config["encryption"] = {
            "enabled": True,
            "private_key": encryption_key_path,
        }
    else:
        config["encryption"] = {"enabled": False}

    # Policies for algorithm whitelisting
    policies = {}
    if allowed_algorithms:
        policies["allowed_algorithms"] = allowed_algorithms
    if allowed_algorithm_stores:
        policies["allowed_algorithm_stores"] = allowed_algorithm_stores
    if policies:
        config["policies"] = policies

    log.info(f"Built node config for server: {server_url}")
    log.info(f"  Databases: {len(databases)}")
    log.info(f"  Encryption: {'enabled' if encryption_key_path else 'disabled'}")
    log.info(f"  Authorized projects: {authorized_projects or 'none (all blocked)'}")

    return config
