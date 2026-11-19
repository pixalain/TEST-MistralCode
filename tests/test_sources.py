import pytest

from tv_live import sources as sources_mod
from tv_live.sources import Source, load_builtin_catalog, load_playlist_file, load_source


def test_builtin_catalog_is_valid():
    catalog = load_builtin_catalog()
    assert len(catalog["streams"]) >= 10
    assert len(catalog["playlists"]) >= 1
    for ch in catalog["streams"] + catalog["playlists"]:
        assert ch.url.startswith("https://"), ch
        assert ch.name


def test_source_from_dict():
    s = Source.from_dict({"kind": "url", "path": "https://x/y.m3u8", "name": "X"})
    assert s.kind == "url" and s.name == "X"
    with pytest.raises(ValueError):
        Source.from_dict({"kind": "carrier-pigeon", "path": "x"})
    with pytest.raises(ValueError):
        Source.from_dict({"kind": "file", "path": "  "})


def test_load_playlist_file(tmp_path):
    p = tmp_path / "test.m3u8"
    p.write_text(
        '#EXTM3U\n#EXTINF:-1 group-title="G",A\nhttps://a/1.m3u8\n', encoding="utf-8"
    )
    channels = load_playlist_file(p)
    assert len(channels) == 1
    assert channels[0].name == "A" and channels[0].group == "G"


class _FakeResp:
    encoding = "utf-8"
    apparent_encoding = "utf-8"
    text = '#EXTM3U\n#EXTINF:-1,B\nhttps://b/2.m3u8\n'

    def raise_for_status(self):
        return None


def test_load_source_url(monkeypatch):
    monkeypatch.setattr(
        sources_mod.requests, "get", lambda *a, **k: _FakeResp()
    )
    channels = load_source(Source("url", "https://x/y.m3u8"))
    assert [c.name for c in channels] == ["B"]


def test_load_source_file(tmp_path):
    p = tmp_path / "p.m3u"
    p.write_text("#EXTM3U\n#EXTINF:-1,C\nhttp://c/3\n", encoding="utf-8")
    assert [c.name for c in load_source(Source("file", str(p)))] == ["C"]
