"""Tests for the v6-bridge configuration module."""

import os
import tempfile
from pathlib import Path

import pytest

from bridge.config import (
    build_node_config,
    discover_databases,
    parse_csv_env,
    get_env,
)


def test_parse_csv_env_empty(monkeypatch):
    monkeypatch.delenv("TEST_VAR", raising=False)
    assert parse_csv_env("TEST_VAR") == []


def test_parse_csv_env_single(monkeypatch):
    monkeypatch.setenv("TEST_VAR", "foo")
    assert parse_csv_env("TEST_VAR") == ["foo"]


def test_parse_csv_env_multiple(monkeypatch):
    monkeypatch.setenv("TEST_VAR", "foo, bar ,baz")
    assert parse_csv_env("TEST_VAR") == ["foo", "bar", "baz"]


def test_get_env_required_missing(monkeypatch):
    monkeypatch.delenv("MISSING_VAR", raising=False)
    with pytest.raises(EnvironmentError):
        get_env("MISSING_VAR", required=True)


def test_get_env_default(monkeypatch):
    monkeypatch.delenv("MISSING_VAR", raising=False)
    assert get_env("MISSING_VAR", default="fallback") == "fallback"


def test_discover_databases_empty_dir():
    with tempfile.TemporaryDirectory() as tmpdir:
        result = discover_databases(tmpdir, [])
        assert result == []


def test_discover_databases_with_authorized_project():
    with tempfile.TemporaryDirectory() as tmpdir:
        # Create a project with a parquet file
        project_dir = Path(tmpdir) / "shared-test"
        project_dir.mkdir()
        (project_dir / "table.parquet").write_bytes(b"fake parquet")

        # Only authorized projects are discovered
        result = discover_databases(tmpdir, ["shared-test"])
        assert len(result) == 1
        assert result[0]["label"] == "shared-test"
        assert result[0]["type"] == "parquet"


def test_discover_databases_unauthorized_project_excluded():
    with tempfile.TemporaryDirectory() as tmpdir:
        # Create two projects
        for name in ["shared-allowed", "shared-denied"]:
            project_dir = Path(tmpdir) / name
            project_dir.mkdir()
            (project_dir / "table.parquet").write_bytes(b"fake parquet")

        result = discover_databases(tmpdir, ["shared-allowed"])
        assert len(result) == 1
        assert result[0]["label"] == "shared-allowed"


def test_discover_databases_nonexistent_dir():
    result = discover_databases("/nonexistent/path", [])
    assert result == []


def test_build_node_config_minimal(monkeypatch, tmp_path):
    monkeypatch.setenv("V6_SERVER_URL", "https://v6.example.org")
    monkeypatch.setenv("V6_API_KEY", "test-api-key")
    monkeypatch.setenv("V6_DATA_DIR", str(tmp_path))

    config = build_node_config()

    assert config["server_url"] == "https://v6.example.org"
    assert config["api_key"] == "test-api-key"
    assert config["databases"] == []
    assert config["encryption"]["enabled"] is False


def test_build_node_config_full(monkeypatch, tmp_path):
    # Create a project with data
    project = tmp_path / "shared-myproject"
    project.mkdir()
    (project / "data.parquet").write_bytes(b"fake")

    monkeypatch.setenv("V6_SERVER_URL", "https://v6.example.org")
    monkeypatch.setenv("V6_API_KEY", "test-key")
    monkeypatch.setenv("V6_DATA_DIR", str(tmp_path))
    monkeypatch.setenv("V6_COLLABORATION_ID", "42")
    monkeypatch.setenv("V6_ENCRYPTION_KEY_PATH", "/mnt/key.pem")
    monkeypatch.setenv("V6_AUTHORIZED_PROJECTS", "shared-myproject")
    monkeypatch.setenv("V6_ALLOWED_ALGORITHMS", "harbor.v6.ai/algo/.*")

    config = build_node_config()

    assert config["collaboration_id"] == 42
    assert config["encryption"]["enabled"] is True
    assert config["encryption"]["private_key"] == "/mnt/key.pem"
    assert len(config["databases"]) == 1
    assert config["databases"][0]["label"] == "shared-myproject"
    assert "allowed_algorithms" in config["policies"]


def test_build_node_config_missing_required(monkeypatch):
    monkeypatch.delenv("V6_SERVER_URL", raising=False)
    monkeypatch.delenv("V6_API_KEY", raising=False)

    with pytest.raises(EnvironmentError):
        build_node_config()
