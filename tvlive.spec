# -*- mode: python ; coding: utf-8 -*-
"""Spec PyInstaller : produit dist/TVLive.exe (fenêtré, onefile)."""
import os
import sys

block_cipher = None

datas = []

# ffmpeg embarqué (binaire statique fourni par imageio-ffmpeg)
try:
    import imageio_ffmpeg

    datas.append((imageio_ffmpeg.get_ffmpeg_exe(), "."))
except Exception:
    pass

# libvlc embarqué si VLC est installé sur la machine de build
if sys.platform == "win32":
    candidates = [
        os.environ.get("VLCDIR", ""),
        r"C:\Program Files\VideoLAN\VLC",
        r"C:\Program Files (x86)\VideoLAN\VLC",
    ]
    for vlc_dir in candidates:
        if vlc_dir and os.path.isdir(vlc_dir):
            datas.append((os.path.join(vlc_dir, "libvlc.dll"), "."))
            datas.append((os.path.join(vlc_dir, "libvlccore.dll"), "."))
            plugins = os.path.join(vlc_dir, "plugins")
            if os.path.isdir(plugins):
                datas.append((plugins, "plugins"))
            break

a = Analysis(
    ["main.py"],
    pathex=[],
    binaries=[],
    datas=datas,
    hiddenimports=["vlc", "imageio_ffmpeg", "requests"],
    hookspath=[],
    hooksconfig={},
    runtime_hooks=[],
    excludes=[],
    cipher=block_cipher,
    noarchive=False,
)
pyz = PYZ(a.pure, a.zipped_data, cipher=block_cipher)

exe = EXE(
    pyz,
    a.scripts,
    a.binaries,
    a.zipfiles,
    a.datas,
    [],
    name="TVLive",
    debug=False,
    bootloader_ignore_signals=False,
    strip=False,
    upx=False,
    console=False,
    icon=None,
)
