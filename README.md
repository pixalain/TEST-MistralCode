# TV Live & Replay

Application **Windows** (Python + Tkinter, packagée en `.exe` avec PyInstaller) pour :

- **regarder la TV en direct** (flux HLS/DASH en clair, embarqués via VLC/libvlc, avec repli sur un lecteur externe) ;
- **télécharger des replays / flux** (file d'attente, progression, pause / reprise / annulation, sortie MP4 via ffmpeg) ;
- charger des **playlists IPTV (M3U)** locales ou par URL, plus un petit catalogue intégré de chaînes du service public et d'infos en flux ouvert.

> **Avertissement légal** : l'application ne lit et ne télécharge que des **flux accessibles en clair** (HLS non chiffré, playlists publiques, flux officiels ouverts). Elle ne contourne **aucun DRM** (Widevine, PlayReady...), aucun géo-blocage ni paywall : un flux protégé fera simplement échouer en lecture/téléchargement. Certaines chaînes du catalogue intégré appliquent un géo-blocage et ne seront lisibles que depuis leur région. Utilisez l'application conformément aux conditions d'utilisation des diffuseurs.

## Installation (développement)

```bash
pip install -r requirements-dev.txt
```

Pour la lecture embarquée, installez aussi [VLC](https://www.videolan.org/) sur la machine (sinon l'app ouvre le flux dans le lecteur externe).

## Lancement

```bash
python main.py                          # interface graphique
python -m tv_live --playlist ma_liste.m3u8
python -m tv_live --url https://.../flux.m3u8
```

## Compilation en application Windows (`TVLive.exe`)

Sur une machine Windows avec Python 3.10+ :

```bat
build.bat
```

L'exécutable est généré dans `dist\TVLive.exe`. Le script embarque automatiquement :

- **ffmpeg** (binaire statique via `imageio-ffmpeg`) pour les téléchargements ;
- **libvlc** si VLC est installé sur la machine de build (sinon : bouton « Ouvrir dans le lecteur externe »).

## Utilisation

- Double-clic sur une chaîne → lecture dans la zone vidéo (ou le lecteur externe).
- Bouton **⬇ Télécharger** → ajoute le flux courant à l'onglet *Téléchargements* (dossier `Videos\TVLive` par défaut, modifiable dans *Préférences*).
- Menu **Fichier** → ouvrir une playlist locale `.m3u`/`.m3u8`, ajouter une playlist par URL, télécharger un flux direct par URL.
- Menu **Sources** → charger les playlists publiques intégrées ou recharger vos playlists enregistrées.
- Champ de recherche en haut de la liste des chaînes.

## Structure du projet

```
main.py                  point d'entrée (PyInstaller)
tv_live/
  m3u.py                 parseur de playlists M3U/M3U8
  sources.py             catalogue intégré + playlists fichier/URL
  sources_builtin.json   chaînes intégrées (flux ouverts uniquement)
  config.py              configuration persistée (%APPDATA%\TVLive\config.json)
  player.py              lecteur embarqué VLC (python-vlc) + lecteur externe
  downloader.py          file de téléchargements (ffmpeg), progression, pause/reprise
  gui.py                 interface Tkinter
  app.py / __main__.py   CLI
tests/                   tests unitaires (pytest)
tvlive.spec              spec PyInstaller
build.bat                compilation Windows
```

## Tests

```bash
python -m pytest tests/ -q
```
