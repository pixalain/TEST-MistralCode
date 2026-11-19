from tv_live.m3u import Channel, group_channels, parse_m3u, search_channels, to_m3u

SAMPLE = (
    "#EXTM3U\r\n"
    '#EXTINF:-1 tvg-id="Arte.fr" tvg-logo="http://logo/arte.png" group-title="Service public",Arte\r\n'
    "https://example.com/arte.m3u8\r\n"
    '#EXTINF:-1 group-title="Infos" radio="true",Radio Info\n'
    "http://radio.example.com/stream\n"
    "#EXTINF:-1,Chaîne sans groupe\n"
    "https://example.com/chaine.mp4\n"
    "https://example.com/directe.m3u8\n"
)


def test_parse_basic():
    channels = parse_m3u(SAMPLE)
    assert len(channels) == 4
    arte = channels[0]
    assert arte.name == "Arte"
    assert arte.url == "https://example.com/arte.m3u8"
    assert arte.group == "Service public"
    assert arte.tvg_id == "Arte.fr"
    assert arte.logo == "http://logo/arte.png"
    assert arte.radio is False


def test_parse_radio_and_defaults():
    channels = parse_m3u(SAMPLE)
    radio = channels[1]
    assert radio.radio is True
    assert radio.group == "Infos"
    no_group = channels[2]
    assert no_group.group == ""
    plain = channels[3]
    assert plain.name == plain.url == "https://example.com/directe.m3u8"


def test_parse_ignores_other_directives():
    text = "#EXTM3U\n#EXT-X-VERSION:3\n#EXTINF:-1,A\nhttp://a/b.m3u8\n"
    assert [c.name for c in parse_m3u(text)] == ["A"]


def test_group_channels():
    channels = parse_m3u(SAMPLE)
    groups = group_channels(channels)
    assert set(groups) == {"Service public", "Infos", "Divers"}
    assert len(groups["Divers"]) == 2
    assert list(groups) == sorted(groups, key=str.lower)


def test_search_channels():
    channels = parse_m3u(SAMPLE)
    assert len(search_channels(channels, "arte")) == 1
    assert len(search_channels(channels, "RADIO")) == 1
    assert len(search_channels(channels, "example.com")) == 4
    assert len(search_channels(channels, "")) == 4
    assert search_channels(channels, "inexistant") == []


def test_to_m3u_roundtrip():
    channels = [
        Channel("Arte", "https://a/1.m3u8", group="G", logo="l", tvg_id="t", radio=True),
        Channel("B", "https://b/2.mp4"),
    ]
    again = parse_m3u(to_m3u(channels))
    assert [(c.name, c.url, c.group, c.logo, c.tvg_id, c.radio) for c in again] == [
        (c.name, c.url, c.group, c.logo, c.tvg_id, c.radio) for c in channels
    ]
