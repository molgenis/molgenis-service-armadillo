"""
V6-Bridge: Armadillo-to-Vantage6 node bridge.

This is the entry point for the v6-bridge container. It:
1. Reads configuration from environment variables (set by Armadillo)
2. Translates them into a vantage6 node configuration
3. Starts a health check HTTP server
4. Initializes and runs a vantage6 node

The bridge allows Armadillo to act as a vantage6 node, participating
in vantage6 federated analyses with its hosted Parquet data.
"""

import logging
import os
import sys
import signal
import yaml
import tempfile

from pathlib import Path

logging.basicConfig(
    level=os.environ.get("V6_LOG_LEVEL", "INFO"),
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)
log = logging.getLogger("v6-bridge")


def main():
    log.info("V6-Bridge starting...")

    # Import after logging is configured
    from bridge.config import build_node_config
    from bridge.health import start_health_server, update_status

    # Start health check server first
    health_port = int(os.environ.get("V6_HEALTH_PORT", "8081"))
    start_health_server(port=health_port)

    # Build vantage6 node configuration from environment variables
    try:
        node_config = build_node_config()
    except EnvironmentError as e:
        log.error(f"Configuration error: {e}")
        sys.exit(1)

    update_status(
        server_url=node_config.get("server_url"),
        database_count=len(node_config.get("databases", [])),
        authorized_projects=[
            db["label"] for db in node_config.get("databases", [])
        ],
    )

    # Write config to a temporary YAML file for vantage6 to read
    config_dir = Path(tempfile.mkdtemp(prefix="v6-bridge-"))
    config_file = config_dir / "node_config.yaml"
    with open(config_file, "w") as f:
        yaml.dump(node_config, f, default_flow_style=False)

    log.info(f"Node config written to {config_file}")

    # Initialize vantage6 node
    try:
        from vantage6.cli.context.node import NodeContext

        ctx = NodeContext.from_external_config_file(
            str(config_file),
            "armadillo-v6-bridge",
            system_folders=False,
        )

        from vantage6.node import Node

        node = Node(ctx)
        update_status(running=True, connected_to_server=True)
        log.info("Vantage6 node initialized and connected")

    except Exception as e:
        log.error(f"Failed to initialize vantage6 node: {e}", exc_info=True)
        update_status(running=False, connected_to_server=False)
        sys.exit(1)

    # Handle shutdown signals gracefully
    def shutdown(signum, frame):
        log.info(f"Received signal {signum}, shutting down...")
        update_status(running=False)
        try:
            node.cleanup()
        except Exception:
            pass
        sys.exit(0)

    signal.signal(signal.SIGTERM, shutdown)
    signal.signal(signal.SIGINT, shutdown)

    # Run the node's main loop
    try:
        log.info("Starting node main loop")
        node.run()
    except KeyboardInterrupt:
        log.info("Interrupted, shutting down...")
    except Exception as e:
        log.error(f"Node crashed: {e}", exc_info=True)
        update_status(running=False, connected_to_server=False)
    finally:
        try:
            node.cleanup()
        except Exception:
            pass


if __name__ == "__main__":
    main()
