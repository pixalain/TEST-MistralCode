"""Téléchargement des replays : file d'attente, progression, pause / reprise / annulation.

Seuls les flux accessibles en clair sont supportés (HLS non chiffré, fichiers directs).
Aucun DRM n'est contourné : un flux chiffré fera simplement échouer ffmpeg.
"""
from __future__ import annotations

import os
import queue
import re
import shutil
import subprocess
import threading
import time
import uuid
from dataclasses import dataclass, field
from typing import Callable, Optional

_ILLEGAL = re.compile(r'[<>:"/\\|?*\x00-\x1f]')
_VALID_EXTS = (".mp4", ".mkv", ".ts", ".webm", ".mp3", ".aac", ".m4a", ".mov", ".avi")

QUEUED, RUNNING, PAUSED, DONE, ERROR, CANCELLED = (
    "queued",
    "running",
    "paused",
    "done",
    "error",
    "cancelled",
)


def sanitize_filename(name: str, default: str = "video") -> str:
    name = _ILLEGAL.sub("_", name).strip().strip(".")
    name = re.sub(r"\s+", " ", name)
    return name[:180] or default


def extension_for_url(url: str, default: str = ".mp4") -> str:
    path = url.split("?", 1)[0].split("#", 1)[0]
    ext = os.path.splitext(path)[1].lower()
    if ext in ("", ".m3u", ".m3u8"):
        return ".mp4"
    if ext in _VALID_EXTS:
        return ext
    return default


def find_ffmpeg() -> Optional[str]:
    exe = shutil.which("ffmpeg")
    if exe:
        return exe
    try:
        import imageio_ffmpeg

        return imageio_ffmpeg.get_ffmpeg_exe()
    except Exception:
        return None


def probe_duration(url: str) -> Optional[float]:
    """Durée du média via ffprobe, si disponible (None sinon : progression indéterminée)."""
    exe = shutil.which("ffprobe")
    if not exe:
        return None
    try:
        out = subprocess.run(
            [
                exe,
                "-v",
                "error",
                "-show_entries",
                "format=duration",
                "-of",
                "default=noprint_wrappers=1:nokey=1",
                url,
            ],
            capture_output=True,
            text=True,
            timeout=30,
        )
        return float(out.stdout.strip())
    except Exception:
        return None


@dataclass
class DownloadJob:
    url: str
    dest_dir: str
    name: str = ""
    id: str = field(default_factory=lambda: uuid.uuid4().hex[:12])
    status: str = QUEUED
    progress: float = 0.0
    speed: str = ""
    elapsed: str = ""
    error: str = ""
    dest_path: str = ""

    def target_path(self) -> str:
        base = sanitize_filename(self.name or self.url) + extension_for_url(self.url)
        return os.path.join(self.dest_dir, base)


Event = tuple[str, DownloadJob]


def _fmt_duration(seconds: float) -> str:
    m, s = divmod(int(seconds), 60)
    h, m = divmod(m, 60)
    return f"{h:02d}:{m:02d}:{s:02d}" if h else f"{m:02d}:{s:02d}"


class DownloadManager:
    """Gestionnaire de téléchargements en arrière-plan (un worker, file FIFO)."""

    def __init__(
        self,
        ffmpeg: Optional[str] = None,
        runner: Optional[Callable[[DownloadJob], None]] = None,
    ) -> None:
        self._ffmpeg = ffmpeg if ffmpeg is not None else find_ffmpeg()
        self._runner = runner or self._ffmpeg_runner
        self._jobs: list[DownloadJob] = []
        self._lock = threading.Lock()
        self._events: "queue.Queue[Event]" = queue.Queue()
        self._worker: Optional[threading.Thread] = None
        self._stop = threading.Event()

    def add(self, url: str, dest_dir: str, name: str = "") -> DownloadJob:
        job = DownloadJob(url=url, dest_dir=dest_dir, name=name)
        job.dest_path = job.target_path()
        with self._lock:
            self._jobs.append(job)
        self._ensure_worker()
        self._events.put(("updated", job))
        return job

    def jobs(self) -> list[DownloadJob]:
        with self._lock:
            return list(self._jobs)

    def find(self, job_id: str) -> Optional[DownloadJob]:
        with self._lock:
            for j in self._jobs:
                if j.id == job_id:
                    return j
        return None

    def cancel(self, job_id: str) -> None:
        job = self.find(job_id)
        if job and job.status not in (DONE, CANCELLED):
            job.status = CANCELLED
            self._events.put(("updated", job))

    def pause(self, job_id: str) -> None:
        job = self.find(job_id)
        if job and job.status in (QUEUED, RUNNING):
            job.status = PAUSED
            self._events.put(("updated", job))

    def resume(self, job_id: str) -> None:
        job = self.find(job_id)
        if job and job.status in (PAUSED, ERROR, CANCELLED):
            job.status = QUEUED
            job.error = ""
            if job.url.split("?", 1)[0].lower().endswith((".m3u8", ".m3u")):
                job.progress = 0.0
            self._ensure_worker()
            self._events.put(("updated", job))

    def events(self) -> "queue.Queue[Event]":
        return self._events

    def shutdown(self) -> None:
        self._stop.set()
        for job in self.jobs():
            if job.status == RUNNING:
                job.status = CANCELLED

    def _ensure_worker(self) -> None:
        with self._lock:
            if self._worker and self._worker.is_alive():
                return
            self._stop.clear()
            self._worker = threading.Thread(target=self._work_loop, daemon=True)
            self._worker.start()

    def _work_loop(self) -> None:
        while not self._stop.is_set():
            job = None
            with self._lock:
                for j in self._jobs:
                    if j.status == QUEUED:
                        j.status = RUNNING
                        job = j
                        break
            if job is None:
                time.sleep(0.2)
                continue
            self._events.put(("updated", job))
            try:
                self._runner(job)
                if job.status == RUNNING:
                    job.status = DONE
                    job.progress = 1.0
            except Exception as exc:  # noqa: BLE001
                if job.status != CANCELLED:
                    job.status = ERROR
                    job.error = str(exc)
            self._events.put(("updated", job))
            self._events.put(("finished", job))

    def _ffmpeg_runner(self, job: DownloadJob) -> None:
        if not self._ffmpeg:
            raise RuntimeError(
                "ffmpeg introuvable : installez ffmpeg ou le paquet imageio-ffmpeg."
            )
        os.makedirs(job.dest_dir, exist_ok=True)
        duration = probe_duration(job.url)
        cmd = [
            self._ffmpeg,
            "-y",
            "-hide_banner",
            "-loglevel",
            "error",
            "-progress",
            "pipe:1",
            "-nostdin",
            "-i",
            job.url,
            "-c",
            "copy",
            "-bsf:a",
            "aac_adtstoasc",
            job.dest_path,
        ]
        proc = subprocess.Popen(
            cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, bufsize=1
        )
        assert proc.stdout is not None
        start = time.time()
        for line in proc.stdout:
            if job.status in (CANCELLED, PAUSED):
                proc.terminate()
                try:
                    proc.wait(timeout=5)
                except subprocess.TimeoutExpired:
                    proc.kill()
                if job.status == PAUSED:
                    return
                raise RuntimeError("téléchargement annulé")
            line = line.strip()
            if line.startswith(("out_time_ms=", "out_time_us=")):
                key, _, val = line.partition("=")
                try:
                    seconds = float(val) / (1e6 if key == "out_time_us=" else 1e3)
                except ValueError:
                    continue
                if duration:
                    job.progress = max(0.0, min(1.0, seconds / duration))
                elapsed = time.time() - start
                job.elapsed = _fmt_duration(elapsed)
                if elapsed > 0:
                    job.speed = f"{seconds / elapsed:.1f}x"
                self._events.put(("updated", job))
            elif line.startswith("speed="):
                job.speed = line.partition("=")[2] or job.speed
        proc.wait()
        if proc.returncode != 0 and job.status == RUNNING:
            err = proc.stderr.read() if proc.stderr else ""
            raise RuntimeError(
                f"ffmpeg a échoué (code {proc.returncode}) : {err.strip()[:300]}"
            )
