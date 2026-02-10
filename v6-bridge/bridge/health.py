"""
Health check HTTP endpoint for the v6-bridge container.

Armadillo polls this endpoint to monitor bridge status.
"""

import logging
from threading import Thread
from flask import Flask, jsonify

log = logging.getLogger(__name__)

# Shared state updated by the main bridge process
_status = {
    "running": False,
    "connected_to_server": False,
    "server_url": None,
    "active_tasks": 0,
    "queued_tasks": 0,
    "authorized_projects": [],
    "database_count": 0,
}


def update_status(**kwargs):
    """Update health status fields."""
    _status.update(kwargs)


def create_health_app() -> Flask:
    """Create a Flask app serving the health check endpoint."""
    app = Flask("v6-bridge-health")

    @app.route("/health")
    def health():
        return jsonify({
            "status": "UP" if _status["running"] else "STARTING",
            **_status,
        })

    @app.route("/health/ready")
    def ready():
        if _status["running"] and _status["connected_to_server"]:
            return jsonify({"status": "READY"}), 200
        return jsonify({"status": "NOT_READY"}), 503

    return app


def start_health_server(port: int = 8081):
    """Start the health check server in a daemon thread."""
    app = create_health_app()

    def run():
        log.info(f"Health check server starting on port {port}")
        app.run(host="0.0.0.0", port=port, use_reloader=False)

    thread = Thread(target=run, daemon=True)
    thread.start()
    log.info(f"Health check server started on port {port}")
    return thread
