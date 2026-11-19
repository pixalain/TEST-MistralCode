"""Sources de chaînes : catalogue intégré + playlists utilisateur (fichier local ou URL)."""
from __future__ import annotations

import json
import os
from dataclasses import dataclass
from pathlib import Path
from typing import Any

import requests

from .m3u import Channel, parse_m3u

USER_AGENT = "TVLive/1.0"
DEFAULT_TIMEOUT = 20


@dataclass
class Source:
    """Une source utilisateur : playlist locale ('file') ou distante ('url')."""

    kind: str
    path: str
    name: str = ""

    def to_dict(self) -> dict[str, str]:
        return {"kind": self.kind, "path": self.path, "name": self.name}

    @classmethod
    def from_dict(cls, d: dict[str, Any]) -> "Source":
        kind = d.get("kind")
        if kind not in ("file", "url"):
            raise ValueError(f"kind de source invalide: {kind!r}")
        path = str(d.get("path", "")).strip()
        if not path:
            raise ValueError("chemin de source vide")
        return cls(kind=kind, path=path, name=str(d.get("name", "")))


def builtin_catalog_path() -> Path:
    return Path(__file__).resolve().parent / "sources_builtin.json"


def load_builtin_catalog(path: Path | None = None) -> dict[str, list[Channel]]:
    """Charge le catalogue intégré : {'streams': [...], 'playlists': [...]}."""
    path = path or builtin_catalog_path()
    data = json.loads(path.read_text(encoding="utf-8"))
    streams = [
        Channel(
            name=str(e["name"]),
            url=str(e["url"]),
            group=str(e.get("group", "Chaînes intégrées")),
            logo=str(e.get("logo", "")),
        )
        for e in data.get("streams", [])
    ]
    playlists = [
        Channel(
            name=str(e["name"]),
            url=str(e["url"]),
            group=str(e.get("group", "Playlists publiques")),
        )
        for e in data.get("playlists", [])
    ]
    return {"streams": streams, "playlists": playlists}


def fetch_text(url: str, timeout: int = DEFAULT_TIMEOUT) -> str:
    resp = requests.get(url, headers={"User-Agent": USER_AGENT}, timeout=timeout)
    resp.raise_for_status()
    if not resp.encoding or resp.encoding.lower() == "iso-8859-1":
        resp.encoding = resp.apparent_encoding or "utf-8"
    return resp.text


def load_playlist_file(path: str | os.PathLike) -> list[Channel]:
    text = Path(path).read_text(encoding="utf-8", errors="replace")
    return parse_m3u(text)


def load_playlist_url(url: str, timeout: int = DEFAULT_TIMEOUT) -> list[Channel]:
    return parse_m3u(fetch_text(url, timeout=timeout))


def load_source(source: Source) -> list[Channel]:
    if source.kind == "file":
        return load_playlist_file(source.path)
    return load_playlist_url(source.path)
