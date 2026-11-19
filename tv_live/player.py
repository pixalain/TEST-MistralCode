"""Lecture des flux : VLC embarqué via python-vlc, avec repli sur un lecteur externe.

Seuls les flux accessibles en clair sont supportés ; aucun DRM n'est traité.
"""
from __future__ import annotations

import shutil
import subprocess
from typing import Callable, Optional


def find_external_player() -> Optional[str]:
    for name in ("vlc", "mpv"):
        path = shutil.which(name)
        if path:
            return path
    return None


class VlcPlayer:
    """Lecteur embarqué basé sur libvlc (nécessite VLC installé ou libvlc embarqué)."""

    def __init__(self) -> None:
        import vlc  # import tardif : échoue proprement si libvlc est absent

        self._instance = vlc.Instance("--no-video-title-show")
        self._player = self._instance.media_player_new()
        self._current: Optional[str] = None

    def available(self) -> bool:
        return True

    def attach(self, get_handle: Callable[[], int]) -> None:
        import sys

        handle = int(get_handle())
        if sys.platform == "win32":
            self._player.set_hwnd(handle)
        elif sys.platform == "darwin":
            self._player.set_nsobject(handle)
        else:
            self._player.set_xwindow(handle)

    def play(self, url: str) -> None:
        media = self._instance.media_new(url)
        self._player.set_media(media)
        self._player.play()
        self._current = url

    def stop(self) -> None:
        self._player.stop()
        self._current = None

    def set_volume(self, volume: int) -> None:
        self._player.audio_set_volume(max(0, min(100, int(volume))))

    def is_playing(self) -> bool:
        return bool(self._player.is_playing())

    def current(self) -> Optional[str]:
        return self._current


class ExternalPlayer:
    """Repli : ouvre le flux dans le lecteur vidéo externe (VLC / mpv)."""

    def __init__(self, executable: Optional[str] = None) -> None:
        self._exe = executable or find_external_player()
        self._proc: Optional[subprocess.Popen] = None
        self._current: Optional[str] = None

    def available(self) -> bool:
        return self._exe is not None

    def attach(self, get_handle: Callable[[], int]) -> None:
        return None

    def play(self, url: str) -> None:
        self.stop()
        if not self._exe:
            raise RuntimeError("Aucun lecteur externe trouvé (installez VLC ou mpv).")
        self._proc = subprocess.Popen([self._exe, url])
        self._current = url

    def stop(self) -> None:
        if self._proc and self._proc.poll() is None:
            self._proc.terminate()
        self._proc = None
        self._current = None

    def set_volume(self, volume: int) -> None:
        return None

    def is_playing(self) -> bool:
        return bool(self._proc and self._proc.poll() is None)

    def current(self) -> Optional[str]:
        return self._current


def create_player(prefer_embedded: bool = True):
    """Retourne un lecteur embarqué si libvlc est disponible, sinon un lecteur externe."""
    if prefer_embedded:
        try:
            return VlcPlayer()
        except Exception:
            pass
    return ExternalPlayer()
