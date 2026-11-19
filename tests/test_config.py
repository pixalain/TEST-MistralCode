import json

from tv_live import config as config_mod


def test_defaults_and_roundtrip(tmp_path, monkeypatch):
    monkeypatch.setenv("XDG_CONFIG_HOME", str(tmp_path))
    cfg = config_mod.load()
    assert cfg["download_dir"].endswith("TVLive")
    assert cfg["sources"] == []

    cfg["sources"] = [{"kind": "url", "path": "https://x/y.m3u8", "name": "X"}]
    cfg["window_size"] = "800x600"
    config_mod.save(cfg)

    again = config_mod.load()
    assert again["sources"][0]["path"] == "https://x/y.m3u8"
    assert again["window_size"] == "800x600"


def test_corrupt_file_falls_back_to_defaults(tmp_path, monkeypatch):
    monkeypatch.setenv("XDG_CONFIG_HOME", str(tmp_path))
    config_mod.config_dir().mkdir(parents=True)
    config_mod.config_path().write_text("{pas du json", encoding="utf-8")
    cfg = config_mod.load()
    assert cfg["sources"] == []
    assert cfg["download_dir"]
