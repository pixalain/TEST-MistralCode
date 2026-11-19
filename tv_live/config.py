"""Configuration utilisateur persistée en JSON (dossier de config de l'OS)."""
from __future__ import annotations

import json
import os
import sys
from pathlib import Path
from typing import Any

APP_NAME = "TVLive"

DEFAULTS: dict[str, Any] = {
    "download_dir": None,
    "window_size": "1100x650",
    "sources": [],
    "last_channel_url": "",
}


def config_dir() -> Path:
    if sys.platform == "win32":
        base = os.environ.get("APPDATA") or str(Path.home() / "AppData" / "Roaming")
    else:
        base = os.environ.get("XDG_CONFIG_HOME") or str(Path.home() / ".config")
    return Path(base) / APP_NAME


def config_path() -> Path:
    return config_dir() / "config.json"


def default_download_dir() -> Path:
    if sys.platform == "win32":
        base = Path(os.environ.get("USERPROFILE") or Path.home())
        return base / "Videos" / APP_NAME
    return Path.home() / "Videos" / APP_NAME


def load() -> dict[str, Any]:
    cfg = dict(DEFAULTS)
    cfg["download_dir"] = str(default_download_dir())
    try:
        data = json.loads(config_path().read_text(encoding="utf-8"))
        if isinstance(data, dict):
            cfg.update(data)
    except (OSError, json.JSONDecodeError):
        pass
    if not cfg.get("download_dir"):
        cfg["download_dir"] = str(default_download_dir())
    return cfg


def save(cfg: dict[str, Any]) -> None:
    config_dir().mkdir(parents=True, exist_ok=True)
    config_path().write_text(
        json.dumps(cfg, ensure_ascii=False, indent=2), encoding="utf-8"
    )
