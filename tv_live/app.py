"""Point d'entrée en ligne de commande."""
from __future__ import annotations

import argparse


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        prog="tvlive",
        description=(
            "TV en direct et téléchargement de replays. "
            "Flux accessibles en clair uniquement (aucun DRM contourné)."
        ),
    )
    parser.add_argument(
        "--playlist",
        action="append",
        default=[],
        metavar="M3U",
        help="Playlist à charger au démarrage (fichier local ou URL), option répétable.",
    )
    parser.add_argument("--url", help="Flux direct à lire au démarrage.")
    args = parser.parse_args(argv)

    from .gui import run

    return run(extra_playlists=args.playlist, url=args.url)
