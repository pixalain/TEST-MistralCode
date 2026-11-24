package com.tvlive.app.player

import android.content.Context
import android.os.Environment
import com.tvlive.app.model.Channel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Téléchargement de fichiers directs (mp4, mp3…) avec progression, vers
 * Android/data/<package>/files/Downloads/TVLive/. Les replays HLS passent
 * par [TvDownloads] (cache Media3, lecture hors ligne dans l'app).
 */
object FileDownloads {

    data class Job(
        val id: String,
        val url: String,
        val name: String,
        val status: String, // running | done | error
        val progress: Float,
        val path: String?,
        val error: String?,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val _jobs = MutableStateFlow<List<Job>>(emptyList())
    val jobs: StateFlow<List<Job>> = _jobs.asStateFlow()

    fun enqueue(context: Context, channel: Channel) {
        if (_jobs.value.any { it.url == channel.url && it.status == "running" }) return
        val id = UUID.randomUUID().toString().take(12)
        val dir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            "TVLive",
        ).apply { mkdirs() }
        val file = File(dir, sanitize(channel.name.ifEmpty { channel.url }) + extensionFor(channel.url))
        update(Job(id, channel.url, channel.name.ifEmpty { channel.url }, "running", 0f, file.absolutePath, null))
        scope.launch {
            try {
                val request = Request.Builder()
                    .url(channel.url)
                    .header("User-Agent", "TVLive/1.0 (Android)")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}")
                    val body = response.body ?: throw IllegalStateException("Réponse vide")
                    val total = body.contentLength()
                    body.byteStream().use { input ->
                        file.outputStream().use { output ->
                            val buffer = ByteArray(64 * 1024)
                            var downloaded = 0L
                            while (true) {
                                val read = input.read(buffer)
                                if (read == -1) break
                                output.write(buffer, 0, read)
                                downloaded += read
                                if (total > 0) setProgress(id, downloaded.toFloat() / total)
                            }
                        }
                    }
                }
                mark(id, "done", 1f, null)
            } catch (e: Exception) {
                file.delete()
                mark(id, "error", 0f, e.message ?: "erreur inconnue")
            }
        }
    }

    fun delete(job: Job) {
        job.path?.let { File(it).delete() }
        _jobs.value = _jobs.value.filterNot { it.id == job.id }
    }

    private fun update(job: Job) {
        _jobs.value = _jobs.value.filterNot { it.id == job.id } + job
    }

    private fun setProgress(id: String, progress: Float) {
        _jobs.value = _jobs.value.map { if (it.id == id) it.copy(progress = progress) else it }
    }

    private fun mark(id: String, status: String, progress: Float, error: String?) {
        _jobs.value = _jobs.value.map {
            if (it.id == id) it.copy(status = status, progress = progress, error = error) else it
        }
    }

    private fun sanitize(name: String): String {
        val cleaned = name
            .replace(Regex("[<>:\"/\\\\|?*\\x00-\\x1f]"), "_")
            .trim().trim('.')
            .replace(Regex("\\s+"), " ")
        return cleaned.take(180).ifEmpty { "video" }
    }

    private fun extensionFor(url: String): String {
        val ext = File(url.substringBefore('?').substringBefore('#')).extension.lowercase()
        return when (ext) {
            "", "m3u", "m3u8" -> ".mp4"
            "mp4", "mkv", "ts", "webm", "mp3", "aac", "m4a", "mov", "avi" -> ".$ext"
            else -> ".mp4"
        }
    }
}
