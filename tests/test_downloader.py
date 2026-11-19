import os
import time

from tv_live.downloader import (
    CANCELLED,
    DONE,
    PAUSED,
    RUNNING,
    DownloadJob,
    DownloadManager,
    extension_for_url,
    sanitize_filename,
)


def test_sanitize_filename():
    assert sanitize_filename('a/b\\c:d*e?f"g<h>i|j') == "a_b_c_d_e_f_g_h_i_j"
    assert sanitize_filename("   ") == "video"
    assert sanitize_filename("...") == "video"
    assert sanitize_filename("  nom   avec   espaces  ") == "nom avec espaces"
    assert len(sanitize_filename("x" * 500)) <= 180


def test_extension_for_url():
    assert extension_for_url("https://a/b/index.m3u8?token=1") == ".mp4"
    assert extension_for_url("https://a/b/video") == ".mp4"
    assert extension_for_url("https://a/b/movie.mkv?x=1") == ".mkv"
    assert extension_for_url("https://a/b/song.mp3") == ".mp3"
    assert extension_for_url("https://a/b/data.bin") == ".mp4"


def test_target_path():
    job = DownloadJob(url="https://a/b/flux.m3u8?x=1", dest_dir="/tmp/dl", name="Mon replay: 1")
    assert job.target_path() == os.path.join("/tmp/dl", "Mon replay_ 1.mp4")


def _wait_for(cond, timeout=5.0):
    t0 = time.time()
    while time.time() - t0 < timeout:
        if cond():
            return True
        time.sleep(0.02)
    return False


def _manager(runner):
    return DownloadManager(ffmpeg="fake-ffmpeg", runner=runner)


def test_job_completes_and_fifo_order():
    order = []

    def runner(job):
        order.append(job.id)
        job.progress = 1.0
        time.sleep(0.05)

    m = _manager(runner)
    try:
        j1 = m.add("https://a/1.m3u8", "/tmp/dl", "un")
        j2 = m.add("https://a/2.mp4", "/tmp/dl", "deux")
        assert _wait_for(lambda: all(j.status == DONE for j in m.jobs()))
        assert order == [j1.id, j2.id]
        assert j1.progress == 1.0
        events = []
        while not m.events().empty():
            events.append(m.events().get_nowait())
        assert ("finished", j1) in events and ("finished", j2) in events
    finally:
        m.shutdown()


def test_cancel_running_job():
    def runner(job):
        while job.status == RUNNING:
            time.sleep(0.02)

    m = _manager(runner)
    try:
        j = m.add("https://a/live.m3u8", "/tmp/dl", "live")
        assert _wait_for(lambda: j.status == RUNNING)
        m.cancel(j.id)
        assert _wait_for(lambda: j.status == CANCELLED)
    finally:
        m.shutdown()


def test_pause_then_resume():
    def runner(job):
        while job.status == RUNNING:
            time.sleep(0.02)

    m = _manager(runner)
    try:
        j = m.add("https://a/v.m3u8", "/tmp/dl", "v")
        assert _wait_for(lambda: j.status == RUNNING)
        m.pause(j.id)
        assert _wait_for(lambda: j.status == PAUSED)
        m.resume(j.id)
        assert _wait_for(lambda: j.status == RUNNING)
        m.cancel(j.id)
        assert _wait_for(lambda: j.status == CANCELLED)
    finally:
        m.shutdown()


def test_runner_error_marks_job():
    def runner(job):
        raise RuntimeError("flux chiffré non supporté")

    m = _manager(runner)
    try:
        j = m.add("https://a/drm.m3u8", "/tmp/dl", "drm")
        assert _wait_for(lambda: j.status == "error")
        assert "chiffré" in j.error
    finally:
        m.shutdown()
