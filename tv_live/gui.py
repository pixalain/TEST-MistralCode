"""Interface graphique Tkinter de TV Live & Replay."""
from __future__ import annotations

import os
import queue
import subprocess
import sys
import threading
import tkinter as tk
from pathlib import Path
from tkinter import filedialog, messagebox, simpledialog, ttk

from . import config as config_mod
from . import player as player_mod
from . import sources as sources_mod
from .downloader import (
    CANCELLED,
    DONE,
    DownloadManager,
    PAUSED,
    QUEUED,
    RUNNING,
)
from .m3u import Channel, group_channels, search_channels

APP_TITLE = "TV Live & Replay"

STATUS_LABELS = {
    QUEUED: "En attente",
    RUNNING: "En cours",
    PAUSED: "En pause",
    DONE: "Terminé",
    "error": "Erreur",
    CANCELLED: "Annulé",
}


def is_playlist_url(url: str) -> bool:
    return url.split("?", 1)[0].lower().endswith((".m3u", ".m3u8"))


class TvApp:
    def __init__(self, root: tk.Tk, extra_playlists: list[str] | None = None):
        self.root = root
        self.cfg = config_mod.load()
        self.channels: list[Channel] = []
        self._iid_to_channel: dict[str, Channel] = {}
        self.manager = DownloadManager()
        self.player = player_mod.create_player()
        self.current: Channel | None = None
        self._load_queue: "queue.Queue[tuple]" = queue.Queue()

        root.title(APP_TITLE)
        try:
            root.geometry(str(self.cfg.get("window_size", "1100x650")))
        except tk.TclError:
            pass
        self._build_menu()
        self._build_ui()
        self._load_initial_sources(extra_playlists or [])
        self._poll_load_queue()
        self._poll_download_events()
        root.protocol("WM_DELETE_WINDOW", self._on_close)

    # ------------------------------------------------------------------ UI
    def _build_menu(self) -> None:
        menubar = tk.Menu(self.root)
        menu_fichier = tk.Menu(menubar, tearoff=0)
        menu_fichier.add_command(
            label="Ouvrir une playlist locale (.m3u)...", command=self._add_playlist_file
        )
        menu_fichier.add_command(
            label="Ajouter une playlist par URL...", command=self._add_playlist_url
        )
        menu_fichier.add_command(
            label="Télécharger un flux (URL)...", command=self._download_url_dialog
        )
        menu_fichier.add_separator()
        menu_fichier.add_command(label="Quitter", command=self._on_close)
        menubar.add_cascade(label="Fichier", menu=menu_fichier)

        self.menu_sources = tk.Menu(menubar, tearoff=0)
        self._rebuild_sources_menu()
        menubar.add_cascade(label="Sources", menu=self.menu_sources)

        menu_prefs = tk.Menu(menubar, tearoff=0)
        menu_prefs.add_command(
            label="Choisir le dossier de téléchargement...",
            command=self._choose_download_dir,
        )
        menubar.add_cascade(label="Préférences", menu=menu_prefs)

        menu_aide = tk.Menu(menubar, tearoff=0)
        menu_aide.add_command(label="À propos", command=self._about)
        menubar.add_cascade(label="Aide", menu=menu_aide)
        self.root.config(menu=menubar)

    def _rebuild_sources_menu(self) -> None:
        self.menu_sources.delete(0, "end")
        catalog = sources_mod.load_builtin_catalog()
        for pl in catalog["playlists"]:
            self.menu_sources.add_command(
                label=f"Charger : {pl.name}",
                command=lambda u=pl.url, n=pl.name: self._load_playlist_async(u, n),
            )
        self.menu_sources.add_separator()
        for src in self.cfg.get("sources", []):
            try:
                s = sources_mod.Source.from_dict(src)
            except ValueError:
                continue
            label = s.name or s.path
            self.menu_sources.add_command(
                label=f"Recharger : {label}",
                command=lambda s=s: self._load_source_async(s),
            )
        self.menu_sources.add_separator()
        self.menu_sources.add_command(
            label="Recharger les chaînes intégrées", command=self._load_builtin_streams
        )

    def _build_ui(self) -> None:
        paned = ttk.Panedwindow(self.root, orient=tk.HORIZONTAL)
        paned.pack(fill=tk.BOTH, expand=True)

        left = ttk.Frame(paned, width=340)
        paned.add(left, weight=0)
        self.search_var = tk.StringVar()
        self.search_var.trace_add("write", lambda *_: self._populate_tree())
        search = ttk.Entry(left, textvariable=self.search_var)
        search.pack(fill=tk.X, padx=6, pady=6)
        self.tree = ttk.Treeview(left, show="tree")
        scroll = ttk.Scrollbar(left, orient=tk.VERTICAL, command=self.tree.yview)
        self.tree.configure(yscrollcommand=scroll.set)
        self.tree.pack(side=tk.LEFT, fill=tk.BOTH, expand=True, padx=(6, 0), pady=(0, 6))
        scroll.pack(side=tk.RIGHT, fill=tk.Y, pady=(0, 6))
        self.tree.bind("<Double-1>", self._on_tree_double)

        right = ttk.Frame(paned)
        paned.add(right, weight=1)
        self.notebook = ttk.Notebook(right)
        self.notebook.pack(fill=tk.BOTH, expand=True, padx=6, pady=6)

        self._build_player_tab()
        self._build_downloads_tab()

        self.status_var = tk.StringVar(value="Prêt.")
        ttk.Label(self.root, textvariable=self.status_var, relief=tk.SUNKEN, anchor=tk.W).pack(
            fill=tk.X, side=tk.BOTTOM
        )

    def _build_player_tab(self) -> None:
        tab = ttk.Frame(self.notebook)
        self.notebook.add(tab, text="Lecture")
        self.video_frame = tk.Frame(tab, bg="black")
        self.video_frame.pack(fill=tk.BOTH, expand=True, padx=6, pady=6)
        self.video_hint = ttk.Label(
            tab,
            text="Double-cliquez sur une chaîne à gauche pour la lire ici.",
            anchor=tk.CENTER,
        )
        controls = ttk.Frame(tab)
        controls.pack(fill=tk.X, padx=6, pady=(0, 6))
        self.current_var = tk.StringVar(value="Aucune chaîne sélectionnée")
        ttk.Label(controls, textvariable=self.current_var, font=("TkDefaultFont", 10, "bold")).pack(
            anchor=tk.W
        )
        btns = ttk.Frame(controls)
        btns.pack(fill=tk.X, pady=4)
        self.play_btn = ttk.Button(btns, text="▶ Lire", command=self._play_current)
        self.play_btn.pack(side=tk.LEFT)
        ttk.Button(btns, text="■ Arrêter", command=self._stop).pack(side=tk.LEFT, padx=4)
        ttk.Button(btns, text="⬇ Télécharger", command=self._download_current).pack(
            side=tk.LEFT, padx=4
        )
        ttk.Button(btns, text="Ouvrir dans le lecteur externe", command=self._open_external).pack(
            side=tk.LEFT, padx=4
        )
        ttk.Label(btns, text="Volume").pack(side=tk.LEFT, padx=(12, 2))
        self.volume = ttk.Scale(
            btns, from_=0, to=100, orient=tk.HORIZONTAL, length=120,
            command=lambda v: self.player.set_volume(int(float(v))),
        )
        self.volume.set(80)
        self.volume.pack(side=tk.LEFT)

    def _build_downloads_tab(self) -> None:
        tab = ttk.Frame(self.notebook)
        self.notebook.add(tab, text="Téléchargements")
        self.dl_dir_var = tk.StringVar(value=str(self.cfg.get("download_dir", "")))
        top = ttk.Frame(tab)
        top.pack(fill=tk.X, padx=6, pady=6)
        ttk.Label(top, text="Dossier :").pack(side=tk.LEFT)
        ttk.Label(top, textvariable=self.dl_dir_var).pack(side=tk.LEFT, padx=4)
        ttk.Button(top, text="Ouvrir le dossier", command=self._open_download_dir).pack(
            side=tk.RIGHT
        )
        cols = ("nom", "statut", "progression", "vitesse", "fichier")
        self.dl_tree = ttk.Treeview(tab, columns=cols, show="headings", height=12)
        for col, label, width in (
            ("nom", "Nom", 220),
            ("statut", "Statut", 90),
            ("progression", "Progression", 110),
            ("vitesse", "Vitesse", 90),
            ("fichier", "Fichier", 320),
        ):
            self.dl_tree.heading(col, text=label)
            self.dl_tree.column(col, width=width, anchor=tk.W)
        dl_scroll = ttk.Scrollbar(tab, orient=tk.VERTICAL, command=self.dl_tree.yview)
        self.dl_tree.configure(yscrollcommand=dl_scroll.set)
        self.dl_tree.pack(side=tk.LEFT, fill=tk.BOTH, expand=True, padx=(6, 0), pady=(0, 6))
        dl_scroll.pack(side=tk.RIGHT, fill=tk.Y, pady=(0, 6))
        btns = ttk.Frame(tab)
        btns.pack(fill=tk.X, padx=6, pady=(0, 6))
        ttk.Button(btns, text="Mettre en pause", command=lambda: self._act_on_selected("pause")).pack(
            side=tk.LEFT
        )
        ttk.Button(btns, text="Reprendre", command=lambda: self._act_on_selected("resume")).pack(
            side=tk.LEFT, padx=4
        )
        ttk.Button(btns, text="Annuler", command=lambda: self._act_on_selected("cancel")).pack(
            side=tk.LEFT, padx=4
        )
        ttk.Button(btns, text="Supprimer de la liste", command=self._remove_selected_job).pack(
            side=tk.LEFT, padx=4
        )

    # ------------------------------------------------------------- chaînes
    def _load_initial_sources(self, extra_playlists: list[str]) -> None:
        self._load_builtin_streams()
        for src in self.cfg.get("sources", []):
            try:
                self._load_source_async(sources_mod.Source.from_dict(src))
            except ValueError:
                continue
        for p in extra_playlists:
            if p.lower().startswith(("http://", "https://")):
                self._load_playlist_async(p, p)
            else:
                self._load_playlist_file_path(p)

    def _load_builtin_streams(self) -> None:
        catalog = sources_mod.load_builtin_catalog()
        self.channels.extend(catalog["streams"])
        self._populate_tree()
        self.status_var.set(f"{len(catalog['streams'])} chaînes intégrées chargées.")

    def _load_source_async(self, source: sources_mod.Source) -> None:
        threading.Thread(
            target=self._load_source_worker, args=(source,), daemon=True
        ).start()

    def _load_source_worker(self, source: sources_mod.Source) -> None:
        label = source.name or source.path
        try:
            channels = sources_mod.load_source(source)
            self._load_queue.put(("ok", label, channels))
        except Exception as exc:  # noqa: BLE001
            self._load_queue.put(("err", label, str(exc)))

    def _load_playlist_async(self, url: str, label: str) -> None:
        self.status_var.set(f"Chargement de la playlist {label} ...")
        threading.Thread(
            target=self._load_playlist_worker, args=(url, label), daemon=True
        ).start()

    def _load_playlist_worker(self, url: str, label: str) -> None:
        try:
            channels = sources_mod.load_playlist_url(url)
            self._load_queue.put(("ok", label, channels))
        except Exception as exc:  # noqa: BLE001
            self._load_queue.put(("err", label, str(exc)))

    def _load_playlist_file_path(self, path: str) -> None:
        try:
            channels = sources_mod.load_playlist_file(path)
            self._add_channels(channels, Path(path).name)
            self._remember_source("file", path, Path(path).name)
        except Exception as exc:  # noqa: BLE001
            messagebox.showerror("Erreur", f"Impossible de lire {path} :\n{exc}")

    def _add_playlist_file(self) -> None:
        path = filedialog.askopenfilename(
            title="Ouvrir une playlist M3U",
            filetypes=[("Playlists M3U", "*.m3u *.m3u8"), ("Tous les fichiers", "*.*")],
        )
        if path:
            self._load_playlist_file_path(path)

    def _add_playlist_url(self) -> None:
        url = simpledialog.askstring("Playlist par URL", "URL de la playlist M3U :", parent=self.root)
        if url and url.strip():
            url = url.strip()
            self._load_playlist_async(url, url)
            self._remember_source("url", url, url)

    def _remember_source(self, kind: str, path: str, name: str) -> None:
        sources = [s for s in self.cfg.get("sources", []) if s.get("path") != path]
        sources.append({"kind": kind, "path": path, "name": name})
        self.cfg["sources"] = sources
        config_mod.save(self.cfg)
        self._rebuild_sources_menu()

    def _add_channels(self, channels: list[Channel], label: str) -> None:
        known = {c.url for c in self.channels}
        added = [c for c in channels if c.url not in known]
        self.channels.extend(added)
        self._populate_tree()
        self.status_var.set(f"{len(added)} chaînes ajoutées depuis {label} "
                            f"({len(self.channels)} au total).")

    def _populate_tree(self) -> None:
        self.tree.delete(*self.tree.get_children())
        self._iid_to_channel.clear()
        filtered = search_channels(self.channels, self.search_var.get())
        for group, chans in group_channels(filtered).items():
            gid = f"g:{group}"
            self.tree.insert("", tk.END, iid=gid, text=f"{group} ({len(chans)})", open=True)
            for i, ch in enumerate(chans):
                iid = f"c:{group}:{i}:{ch.url}"
                self._iid_to_channel[iid] = ch
                self.tree.insert(gid, tk.END, iid=iid, text=ch.name)

    def _on_tree_double(self, _event) -> None:
        sel = self.tree.selection()
        if not sel:
            return
        ch = self._iid_to_channel.get(sel[0])
        if ch is None:
            return
        if is_playlist_url(ch.url):
            self._load_playlist_async(ch.url, ch.name)
        else:
            self._play(ch)

    # ------------------------------------------------------------- lecture
    def _play_current(self) -> None:
        if self.current:
            self._play(self.current)

    def _play(self, ch: Channel) -> None:
        self.current = ch
        self.current_var.set(ch.name)
        self.cfg["last_channel_url"] = ch.url
        try:
            if getattr(self.player, "available", lambda: False)() and not isinstance(
                self.player, player_mod.ExternalPlayer
            ):
                self.root.update_idletasks()
                self.video_hint.place_forget()
                self.player.attach(lambda: self.video_frame.winfo_id())
            self.player.play(ch.url)
            self.status_var.set(f"Lecture : {ch.name}")
        except Exception as exc:  # noqa: BLE001
            messagebox.showerror("Erreur de lecture", f"Impossible de lire {ch.name} :\n{exc}")

    def _stop(self) -> None:
        self.player.stop()
        self.status_var.set("Arrêté.")

    def _open_external(self) -> None:
        if not self.current:
            return
        exe = player_mod.find_external_player()
        if not exe:
            messagebox.showinfo(
                "Lecteur externe", "Installez VLC ou mpv pour utiliser cette fonction."
            )
            return
        subprocess.Popen([exe, self.current.url])
        self.status_var.set(f"Ouvert dans {os.path.basename(exe)} : {self.current.name}")

    # ------------------------------------------------------- téléchargements
    def _download_current(self) -> None:
        if self.current:
            self._add_download(self.current.url, self.current.name)

    def _download_url_dialog(self) -> None:
        url = simpledialog.askstring("Télécharger un flux", "URL du flux ou du replay :", parent=self.root)
        if not url or not url.strip():
            return
        name = simpledialog.askstring("Télécharger un flux", "Nom du fichier (optionnel) :", parent=self.root)
        self._add_download(url.strip(), name or "")

    def _add_download(self, url: str, name: str) -> None:
        job = self.manager.add(url, str(self.cfg.get("download_dir", "")), name)
        self.dl_tree.insert(
            "", tk.END, iid=job.id, values=(job.name or url, STATUS_LABELS[job.status], "0%", "", job.dest_path)
        )
        self.notebook.select(1)
        self.status_var.set(f"Téléchargement ajouté : {job.name or url}")

    def _act_on_selected(self, action: str) -> None:
        sel = self.dl_tree.selection()
        if not sel:
            return
        job_id = sel[0]
        if action == "pause":
            self.manager.pause(job_id)
        elif action == "resume":
            self.manager.resume(job_id)
        elif action == "cancel":
            self.manager.cancel(job_id)

    def _remove_selected_job(self) -> None:
        sel = self.dl_tree.selection()
        if not sel:
            return
        job = self.manager.find(sel[0])
        if job and job.status == RUNNING:
            self.manager.cancel(job.id)
        self.dl_tree.delete(sel[0])

    def _poll_download_events(self) -> None:
        try:
            while True:
                kind, job = self.manager.events().get_nowait()
                self._refresh_job_row(job)
        except queue.Empty:
            pass
        self.root.after(200, self._poll_download_events)

    def _refresh_job_row(self, job) -> None:
        if not self.dl_tree.exists(job.id):
            self.dl_tree.insert(
                "", tk.END, iid=job.id,
                values=(job.name or job.url, "", "", "", job.dest_path),
            )
        pct = f"{int(job.progress * 100)}%"
        values = (
            job.name or job.url,
            STATUS_LABELS.get(job.status, job.status),
            pct,
            job.speed or job.elapsed,
            job.error or job.dest_path,
        )
        self.dl_tree.item(job.id, values=values)

    # ------------------------------------------------------------- divers
    def _poll_load_queue(self) -> None:
        try:
            while True:
                status, label, payload = self._load_queue.get_nowait()
                if status == "ok":
                    self._add_channels(payload, label)
                else:
                    self.status_var.set(f"Erreur de chargement ({label}) : {payload}")
                    messagebox.showerror("Erreur de chargement", f"{label}\n\n{payload}")
        except queue.Empty:
            pass
        self.root.after(300, self._poll_load_queue)

    def _choose_download_dir(self) -> None:
        d = filedialog.askdirectory(initialdir=str(self.cfg.get("download_dir", "")))
        if d:
            self.cfg["download_dir"] = d
            self.dl_dir_var.set(d)
            config_mod.save(self.cfg)

    def _open_download_dir(self) -> None:
        d = str(self.cfg.get("download_dir", ""))
        os.makedirs(d, exist_ok=True)
        if sys.platform == "win32":
            os.startfile(d)  # type: ignore[attr-defined]
        else:
            subprocess.Popen(["xdg-open", d])

    def _about(self) -> None:
        messagebox.showinfo(
            "À propos",
            f"{APP_TITLE}\n\n"
            "TV en direct et téléchargement de replays.\n\n"
            "L'application ne lit et ne télécharge que des flux accessibles en clair "
            "(HLS non chiffré, playlists M3U publiques, flux officiels ouverts). "
            "Elle ne contourne aucun DRM, géo-blocage ou paywall.",
        )

    def _on_close(self) -> None:
        try:
            self.cfg["window_size"] = self.root.geometry()
            config_mod.save(self.cfg)
        except Exception:  # noqa: BLE001
            pass
        self.manager.shutdown()
        self.root.destroy()


def run(extra_playlists: list[str] | None = None, url: str | None = None) -> int:
    root = tk.Tk()
    app = TvApp(root, extra_playlists=extra_playlists)
    if url:
        app._add_download(url, url) if is_playlist_url(url) else app._play(Channel(url, url))
    root.mainloop()
    return 0
