"""Parseur de playlists IPTV (M3U / M3U8)."""
from __future__ import annotations

import re
from dataclasses import dataclass
from typing import Iterable

_EXTINF_RE = re.compile(r"^#EXTINF:([^,]*)(?:,(.*))?$")
_ATTR_RE = re.compile(r'([a-zA-Z0-9_-]+)="([^"]*)"')


@dataclass
class Channel:
    name: str
    url: str
    group: str = ""
    logo: str = ""
    tvg_id: str = ""
    radio: bool = False


def _parse_attrs(raw: str) -> dict[str, str]:
    return dict(_ATTR_RE.findall(raw or ""))


def parse_extinf(line: str) -> tuple[dict[str, str], str]:
    """Retourne (attributs, nom) d'une ligne #EXTINF, ou ({}, "") si invalide."""
    m = _EXTINF_RE.match(line.strip())
    if not m:
        return {}, ""
    return _parse_attrs(m.group(1)), (m.group(2) or "").strip()


def parse_m3u(text: str) -> list[Channel]:
    """Parse une playlist M3U/M3U8 en liste de chaînes."""
    channels: list[Channel] = []
    pending_attrs: dict[str, str] = {}
    pending_name = ""
    for raw in text.splitlines():
        line = raw.strip()
        if not line:
            continue
        if line.startswith("#EXTINF"):
            pending_attrs, pending_name = parse_extinf(line)
        elif line.startswith("#"):
            continue
        else:
            channels.append(
                Channel(
                    name=pending_name or line,
                    url=line,
                    group=pending_attrs.get("group-title", ""),
                    logo=pending_attrs.get("tvg-logo", ""),
                    tvg_id=pending_attrs.get("tvg-id", ""),
                    radio=pending_attrs.get("radio", "").lower() == "true",
                )
            )
            pending_attrs = {}
            pending_name = ""
    return channels


def group_channels(channels: Iterable[Channel]) -> dict[str, list[Channel]]:
    """Regroupe les chaînes par group-title (trié, groupe 'Divers' sinon)."""
    groups: dict[str, list[Channel]] = {}
    for ch in channels:
        groups.setdefault(ch.group or "Divers", []).append(ch)
    return dict(sorted(groups.items(), key=lambda kv: kv[0].lower()))


def search_channels(channels: Iterable[Channel], query: str) -> list[Channel]:
    q = query.strip().lower()
    if not q:
        return list(channels)
    return [
        c
        for c in channels
        if q in c.name.lower() or q in c.group.lower() or q in c.url.lower()
    ]


def to_m3u(channels: Iterable[Channel]) -> str:
    """Sérialise des chaînes en playlist M3U."""
    lines = ["#EXTM3U"]
    for c in channels:
        attrs = []
        if c.tvg_id:
            attrs.append(f'tvg-id="{c.tvg_id}"')
        if c.logo:
            attrs.append(f'tvg-logo="{c.logo}"')
        if c.group:
            attrs.append(f'group-title="{c.group}"')
        if c.radio:
            attrs.append('radio="true"')
        attr_str = (" " + " ".join(attrs)) if attrs else ""
        lines.append(f"#EXTINF:-1{attr_str},{c.name}")
        lines.append(c.url)
    return "\n".join(lines) + "\n"
